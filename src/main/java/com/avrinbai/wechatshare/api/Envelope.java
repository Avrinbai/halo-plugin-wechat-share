package com.avrinbai.wechatshare.api;

public record Envelope<T>(boolean ok, T data, String message, String code) {
    public static <T> Envelope<T> ok(T data) {
        return new Envelope<>(true, data, null, null);
    }

    public static <T> Envelope<T> error(String message) {
        return new Envelope<>(false, null, message, null);
    }

    public static <T> Envelope<T> error(String message, String code) {
        return new Envelope<>(false, null, message, code);
    }
}
