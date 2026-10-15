package com.zoonza.playground.verification.fixture;

import com.zoonza.playground.shared.fixture.EmailFixture;
import com.zoonza.playground.verification.internal.domain.Verification;
import com.zoonza.playground.verification.internal.domain.VerificationPurpose;

import java.time.Duration;
import java.time.Instant;

public final class VerificationFixture {
    public static final String ID = "verification-id";
    public static final Instant REQUESTED_AT = Instant.parse("2026-01-01T00:00:00Z");
    public static final Duration CODE_VALIDITY = Duration.ofMinutes(5);
    public static final Duration VERIFIED_VALIDITY = Duration.ofMinutes(10);
    public static final int MAX_ATTEMPTS = 5;

    private VerificationFixture() {
    }

    public static Verification requested() {
        return Verification.create(
                ID,
                EmailFixture.email(),
                VerificationPurpose.SIGNUP,
                VerificationCodeFixture.code(),
                REQUESTED_AT
        );
    }

    public static Verification verified() {
        Verification verification = requested();
        verification.tryVerify(VerificationCodeFixture.code(), REQUESTED_AT);

        return verification;
    }

    public static Verification failed(int times) {
        Verification verification = requested();

        for (int i = 0; i < times; i++) {
            verification.tryVerify(VerificationCodeFixture.wrongCode(), REQUESTED_AT);
        }

        return verification;
    }

    public static Instant expiresAt() {
        return REQUESTED_AT.plus(CODE_VALIDITY);
    }
}
