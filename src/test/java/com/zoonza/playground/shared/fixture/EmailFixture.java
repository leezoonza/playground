package com.zoonza.playground.shared.fixture;

import com.zoonza.playground.shared.vo.Email;

public final class EmailFixture {
    public static final String EMAIL_VALUE = "user@example.com";

    private EmailFixture() {
    }

    public static Email email() {
        return new Email(EMAIL_VALUE);
    }
}
