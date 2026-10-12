package com.echanneling.service;
public final class PasswordPolicy {
    private PasswordPolicy() {}
    public static boolean valid(String raw) {
        return raw != null && !raw.isBlank() && raw.length() >= 8 && raw.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72;
    }
    public static void requireValid(String raw) {
        if (!valid(raw)) throw new IllegalArgumentException("Use at least 8 characters and at most 72 UTF-8 bytes.");
    }
}
