package com.zoonza.playground.verification.fixture;

import com.zoonza.playground.shared.fixture.EmailFixture;
import com.zoonza.playground.verification.internal.application.dto.SendVerificationCodeCommand;
import com.zoonza.playground.verification.internal.application.dto.VerifyCodeCommand;
import com.zoonza.playground.verification.internal.domain.VerificationPurpose;

public final class VerificationCommandFixture {
    private VerificationCommandFixture() {
    }

    public static SendVerificationCodeCommand sendCommand() {
        return sendCommand(EmailFixture.EMAIL_VALUE);
    }

    public static SendVerificationCodeCommand sendCommand(String email) {
        return new SendVerificationCodeCommand(email, VerificationPurpose.SIGNUP);
    }

    public static VerifyCodeCommand verifyCommand() {
        return verifyCommand(VerificationCodeFixture.CODE_VALUE);
    }

    public static VerifyCodeCommand verifyCommand(String code) {
        return verifyCommand(EmailFixture.EMAIL_VALUE, code);
    }

    public static VerifyCodeCommand verifyCommand(String email, String code) {
        return new VerifyCodeCommand(email, code, VerificationPurpose.SIGNUP);
    }
}
