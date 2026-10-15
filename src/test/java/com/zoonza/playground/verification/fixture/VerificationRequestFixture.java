package com.zoonza.playground.verification.fixture;

import com.zoonza.playground.shared.fixture.EmailFixture;

public final class VerificationRequestFixture {
    private VerificationRequestFixture() {
    }

    public static String sendRequest() {
        return sendRequest(EmailFixture.EMAIL_VALUE);
    }

    public static String sendRequest(String email) {
        return """
                {"email": %s}
                """.formatted(toJson(email));
    }

    public static String verifyRequest() {
        return verifyRequest(EmailFixture.EMAIL_VALUE, VerificationCodeFixture.CODE_VALUE);
    }

    public static String verifyRequest(String email, String code) {
        return """
                {"email": %s, "code": %s}
                """.formatted(toJson(email), toJson(code));
    }

    private static String toJson(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }
}
