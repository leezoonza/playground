package com.zoonza.playground.verification.internal.domain;

import java.util.regex.Pattern;

public record VerificationCode(String value) {
    private static final Pattern PATTERN = Pattern.compile("\\d{6}");

    public VerificationCode {
        if (value == null || !PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("인증번호 형식이 올바르지 않습니다.");
        }
    }
}
