package com.zoonza.playground.shared.vo;

import jakarta.persistence.Embeddable;

import java.util.Locale;
import java.util.regex.Pattern;

@Embeddable
public record Email(String value) {
    private static final Pattern PATTERN = Pattern.compile(
            "^[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+$"
    );

    public Email {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }

        value = value.strip().toLowerCase(Locale.ROOT);

        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("이메일 형식이 올바르지 않습니다.");
        }

        String localPart = value.substring(0, value.indexOf('@'));

        if (localPart.startsWith(".") || localPart.endsWith(".") || localPart.contains("..")) {
            throw new IllegalArgumentException("이메일 형식이 올바르지 않습니다.");
        }
    }
}
