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
import org.springframework.stereotype.Service;

@Service
public class WeChatJsBridgeService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private volatile String cachedAccessToken = "";
    private volatile Instant accessTokenExpiresAt = Instant.EPOCH;

    private volatile String cachedJsapiTicket = "";
    private volatile Instant jsapiTicketExpiresAt = Instant.EPOCH;

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
            var body = httpGet(uri);
            var ticket = readText(body, "ticket");
            if (ticket.isBlank()) {
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
            var uri = URI.create(
                "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
                    + urlEncode(appId) + "&secret=" + urlEncode(appSecret));
            var body = httpGet(uri);
            var token = readText(body, "access_token");
            if (token.isBlank()) {
                return "";
            }
            cachedAccessToken = token;
            accessTokenExpiresAt = Instant.now().plusSeconds(7000);
            cachedJsapiTicket = "";
            jsapiTicketExpiresAt = Instant.EPOCH;
            return token;
        }
    }

    private String httpGet(URI uri) {
        try {
            var req = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(12)).GET().build();
            var resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() / 100 != 2) {
                return "";
            }
            return resp.body() == null ? "" : resp.body();
        } catch (Exception e) {
            return "";
        }
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
            var sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
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
