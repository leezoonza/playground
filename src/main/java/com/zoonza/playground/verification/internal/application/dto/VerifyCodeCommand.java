package com.zoonza.playground.verification.internal.application.dto;

import com.zoonza.playground.verification.internal.domain.VerificationPurpose;

public record VerifyCodeCommand(
        String email,
        String code,
        VerificationPurpose purpose
) {
}
