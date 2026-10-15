package com.zoonza.playground.verification.internal.domain;

import com.zoonza.playground.shared.error.BusinessException;
import com.zoonza.playground.shared.vo.Email;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;

import static java.util.Objects.requireNonNull;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Verification {
    private static final Duration CODE_VALIDITY = Duration.ofMinutes(5);
    private static final Duration VERIFIED_VALIDITY = Duration.ofMinutes(10);
    private static final int MAX_ATTEMPTS = 5;

    private String id;
    private Email email;
    private VerificationPurpose purpose;
    private VerificationCode code;
    private VerificationStatus status;
    private Instant expiresAt;
    private int failedAttempts;

    private Verification(
            String id,
            Email email,
            VerificationPurpose purpose,
            VerificationCode code,
            Instant expiresAt
    ) {
        this.id = requireNonNull(id, "인증 ID는 필수입니다.");
        this.email = requireNonNull(email, "이메일은 필수입니다.");
        this.purpose = requireNonNull(purpose, "인증 유형은 필수입니다.");
        this.code = requireNonNull(code, "인증번호는 필수입니다.");
        this.status = VerificationStatus.REQUESTED;
        this.expiresAt = requireNonNull(expiresAt, "만료 시각은 필수입니다.");
        this.failedAttempts = 0;
    }

    public static Verification create(
            String id,
            Email email,
            VerificationPurpose purpose,
            VerificationCode code,
            Instant requestedAt
    ) {
        return new Verification(
                id,
                email,
                purpose,
                code,
                requestedAt.plus(CODE_VALIDITY)
        );
    }

    public boolean tryVerify(VerificationCode input, Instant now) {
        if (status == VerificationStatus.VERIFIED) {
            throw new BusinessException(VerificationErrorCode.ALREADY_VERIFIED);
        }

        if (status == VerificationStatus.CONSUMED) {
            throw new BusinessException(VerificationErrorCode.ALREADY_CONSUMED);
        }

        if (!now.isBefore(expiresAt)) {
            throw new BusinessException(VerificationErrorCode.VERIFICATION_EXPIRED);
        }

        if (this.failedAttempts >= MAX_ATTEMPTS) {
            throw new BusinessException(VerificationErrorCode.TOO_MANY_ATTEMPTS);
        }

        if (!code.equals(input)) {
            failedAttempts++;

            return false;
        }

        this.status =  VerificationStatus.VERIFIED;
        this.expiresAt = now.plus(VERIFIED_VALIDITY);

        return true;
    }
}
