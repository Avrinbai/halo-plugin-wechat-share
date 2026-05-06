package com.avrinbai.wechatshare.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** 创建/更新卡片请求体。未知字段忽略，便于前后端独立升级。 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CardWriteRequest(
    String cardKind,
    String title,
    String description,
    String img,
    String redirectUrl,
    String mediaUrl,
    String displayName,
    String optionalLinkLabel,
    String optionalLinkUrl,
    String contactInfo,
    String videoTitle,
    String videoGuideText,
    String videoExtraLink,
    String videoExtraLinkLabel,
    List<FileNoteWrite> fileNotes
) {
    public static CardWriteRequest legacyLink(String title, String description, String img, String redirectUrl) {
        return new CardWriteRequest(
            null,
            title,
            description,
            img,
            redirectUrl,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of()
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FileNoteWrite(String title, String detail, Boolean jumpLink, String url) {
    }
}
