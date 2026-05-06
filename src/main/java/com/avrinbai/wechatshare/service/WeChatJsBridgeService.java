package com.avrinbai.wechatshare.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WeChatJsBridgeService {

    private static final Logger log = LoggerFactory.getLogger(WeChatJsBridgeService.class);

    private static final int TOKEN_FAIL_STREAK_OPEN = 3;
    private static final int CIRCUIT_SECONDS = 45;
    private static final int BODY_SNIP_MAX = 240;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private volatile String cachedAccessToken = "";
    private volatile Instant accessTokenExpiresAt = Instant.EPOCH;

    private volatile String cachedJsapiTicket = "";
    private volatile Instant jsapiTicketExpiresAt = Instant.EPOCH;

    private final AtomicInteger tokenFailStreak = new AtomicInteger(0);
    private final AtomicLong circuitUntilEpochSec = new AtomicLong(0);

    public WeChatJsBridgeService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public Signature sign(String appId, String appSecret, String url) {
        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            return new Signature(appId == null ? "" : appId, "", 0, "", "");
        }
        var ticket = jsapiTicket(appId, appSecret);
        if (ticket.isBlank()) {
            return new Signature(appId, "", 0, "", "");
        }
        var rnd = new SecureRandom();
        var buf = new byte[8];
        rnd.nextBytes(buf);
        var nonce = HexFormat.of().formatHex(buf);
        var ts = Instant.now().getEpochSecond();
        var plain = "jsapi_ticket=" + ticket + "&noncestr=" + nonce + "&timestamp=" + ts + "&url=" + url;
        var sig = sha1Hex(plain);
        return new Signature(appId, nonce, ts, sig, ticket);
    }

    private boolean circuitOpen() {
        var until = circuitUntilEpochSec.get();
        return Instant.now().getEpochSecond() < until;
    }

    private void markTokenSuccess() {
        tokenFailStreak.set(0);
        circuitUntilEpochSec.set(0);
    }

    private void markTokenFailure(String reason, int httpStatus, OptionalInt errcode, String snippet) {
        var streak = tokenFailStreak.incrementAndGet();
        var ec = errcode.isPresent() ? String.valueOf(errcode.getAsInt()) : "-";
        if (streak >= TOKEN_FAIL_STREAK_OPEN) {
            var until = Instant.now().getEpochSecond() + CIRCUIT_SECONDS;
            circuitUntilEpochSec.set(until);
            log.warn(
                "wechat jssdk: token path opening {}s circuit after {} failures reason={} http={} errcode={} snippet={}",
                CIRCUIT_SECONDS,
                streak,
                reason,
                httpStatus,
                ec,
                snippet
            );
        } else {
            log.debug(
                "wechat jssdk: token fetch failed reason={} http={} errcode={} streak={} snippet={}",
                reason,
                httpStatus,
                ec,
                streak,
                snippet
            );
        }
    }

    private void markTicketFailure(String reason, int httpStatus, OptionalInt errcode, String snippet) {
        var ec = errcode.isPresent() ? String.valueOf(errcode.getAsInt()) : "-";
        log.debug("wechat jssdk: jsapi_ticket fetch failed reason={} http={} errcode={} snippet={}", reason, httpStatus, ec, snippet);
    }

    private String jsapiTicket(String appId, String appSecret) {
        if (!cachedJsapiTicket.isBlank() && Instant.now().isBefore(jsapiTicketExpiresAt)) {
            return cachedJsapiTicket;
        }
        synchronized (this) {
            if (!cachedJsapiTicket.isBlank() && Instant.now().isBefore(jsapiTicketExpiresAt)) {
                return cachedJsapiTicket;
            }
            var token = accessToken(appId, appSecret);
            if (token.isBlank()) {
                return "";
            }
            var uri = URI.create(
                "https://api.weixin.qq.com/cgi-bin/ticket/getticket?type=jsapi&access_token=" + token);
            var res = httpGet(uri);
            if (res.status() / 100 != 2) {
                markTicketFailure("http_non_2xx", res.status(), OptionalInt.empty(), snip(res.body()));
                return "";
            }
            var wxErr = parseWxError(res.body());
            if (wxErr.isPresent()) {
                var e = wxErr.get();
                markTicketFailure("wx_api", res.status(), OptionalInt.of(e.errcode()), snip(e.errmsg()));
                return "";
            }
            var ticket = readText(res.body(), "ticket");
            if (ticket.isBlank()) {
                markTicketFailure("missing_ticket", res.status(), OptionalInt.empty(), snip(res.body()));
                return "";
            }
            cachedJsapiTicket = ticket;
            jsapiTicketExpiresAt = Instant.now().plusSeconds(7000);
            return ticket;
        }
    }

    private String accessToken(String appId, String appSecret) {
        if (!cachedAccessToken.isBlank() && Instant.now().isBefore(accessTokenExpiresAt)) {
            return cachedAccessToken;
        }
        synchronized (this) {
            if (!cachedAccessToken.isBlank() && Instant.now().isBefore(accessTokenExpiresAt)) {
                return cachedAccessToken;
            }
            if (circuitOpen()) {
                log.debug(
                    "wechat jssdk: skip access_token fetch (circuit open) appId={} untilEpochSec={}",
                    maskAppId(appId),
                    circuitUntilEpochSec.get()
                );
                return "";
            }
            var uri = URI.create(
                "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
                    + urlEncode(appId) + "&secret=" + urlEncode(appSecret));
            var res = httpGet(uri);
            if (res.status() / 100 != 2) {
                markTokenFailure("http_non_2xx", res.status(), OptionalInt.empty(), snip(res.body()));
                return "";
            }
            var wxErr = parseWxError(res.body());
            if (wxErr.isPresent()) {
                var e = wxErr.get();
                markTokenFailure("wx_api", res.status(), OptionalInt.of(e.errcode()), snip(e.errmsg()));
                return "";
            }
            var token = readText(res.body(), "access_token");
            if (token.isBlank()) {
                markTokenFailure("missing_access_token", res.status(), OptionalInt.empty(), snip(res.body()));
                return "";
            }
            markTokenSuccess();
            cachedAccessToken = token;
            accessTokenExpiresAt = Instant.now().plusSeconds(7000);
            cachedJsapiTicket = "";
            jsapiTicketExpiresAt = Instant.EPOCH;
            return token;
        }
    }

    private record HttpResult(int status, String body) {
    }

    private HttpResult httpGet(URI uri) {
        try {
            var req = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(12)).GET().build();
            var resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            var body = resp.body() == null ? "" : resp.body();
            return new HttpResult(resp.statusCode(), body);
        } catch (Exception e) {
            log.debug("wechat jssdk: http exception host={} msg={}", uri.getHost(), e.toString());
            return new HttpResult(-1, "");
        }
    }

    private record WxApiError(int errcode, String errmsg) {
    }

    private Optional<WxApiError> parseWxError(String json) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            var ec = node.get("errcode");
            if (ec == null || ec.isNull() || !ec.isIntegralNumber()) {
                return Optional.empty();
            }
            var code = ec.asInt();
            if (code == 0) {
                return Optional.empty();
            }
            var msg = node.get("errmsg");
            var msgText = msg == null || msg.isNull() ? "" : msg.asText("");
            return Optional.of(new WxApiError(code, msgText));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static String snip(String s) {
        if (s == null) {
            return "";
        }
        var t = s.replace('\r', ' ').replace('\n', ' ').trim();
        if (t.length() <= BODY_SNIP_MAX) {
            return t;
        }
        return t.substring(0, BODY_SNIP_MAX) + "…";
    }

    private static String maskAppId(String appId) {
        if (appId == null || appId.isBlank()) {
            return "";
        }
        var t = appId.trim();
        if (t.length() <= 6) {
            return t.charAt(0) + "***";
        }
        return t.substring(0, 4) + "…" + t.substring(t.length() - 2);
    }

    private String readText(String json, String field) {
        if (json == null || json.isBlank()) {
            return "";
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            var v = node.get(field);
            return v == null || v.isNull() ? "" : v.asText("");
        } catch (Exception e) {
            return "";
        }
    }

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String sha1Hex(String plain) {
        try {
            var md = MessageDigest.getInstance("SHA-1");
            var bytes = md.digest(plain.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            return "";
        }
    }

    public record Signature(String appId, String nonceStr, long timestamp, String signature, String jsapiTicket) {
        public boolean usable() {
            return appId != null && !appId.isBlank() && signature != null && !signature.isBlank();
        }
    }
}
