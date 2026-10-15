package com.zoonza.playground.verification.internal.application.provided;

import com.zoonza.playground.verification.internal.application.dto.SendVerificationCodeCommand;
import com.zoonza.playground.verification.internal.application.dto.VerifyCodeCommand;

public interface VerificationCommandUseCase {
    void sendCode(SendVerificationCodeCommand command);

    void verifyCode(VerifyCodeCommand command);
}
