package com.zoonza.playground.verification.fixture;

import com.zoonza.playground.verification.internal.domain.VerificationCode;

public final class VerificationCodeFixture {
    public static final String CODE_VALUE = "123456";
    public static final String WRONG_CODE_VALUE = "654321";

    private VerificationCodeFixture() {
    }

    public static VerificationCode code() {
        return new VerificationCode(CODE_VALUE);
    }

    public static VerificationCode wrongCode() {
        return new VerificationCode(WRONG_CODE_VALUE);
    }
}
