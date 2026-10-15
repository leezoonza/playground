package com.zoonza.playground.verification.internal.application;

import com.zoonza.playground.verification.internal.domain.VerificationCode;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class VerificationCodeGenerator {
    private static final int CODE_BOUND = 1_000_000;

    private final SecureRandom secureRandom = new SecureRandom();

    public VerificationCode generate() {
        String codeValue = String.format("%06d", secureRandom.nextInt(CODE_BOUND));

        return new VerificationCode(codeValue);
    }
}
