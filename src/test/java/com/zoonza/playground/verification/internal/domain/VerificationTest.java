package com.zoonza.playground.verification.internal.domain;

import com.zoonza.playground.shared.error.BusinessException;
import com.zoonza.playground.shared.fixture.EmailFixture;
import com.zoonza.playground.verification.fixture.VerificationCodeFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static com.zoonza.playground.verification.fixture.VerificationFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerificationTest {

    @Nested
    @DisplayName("인증 생성")
    class Create {

        @Test
        @DisplayName("요청 상태로 생성되고 요청 시각으로부터 5분 뒤 만료된다")
        void create() {
            Verification verification = Verification.create(
                    ID,
                    EmailFixture.email(),
                    VerificationPurpose.SIGNUP,
                    VerificationCodeFixture.code(),
                    REQUESTED_AT
            );

            assertThat(verification.getId()).isEqualTo(ID);
            assertThat(verification.getEmail()).isEqualTo(EmailFixture.email());
            assertThat(verification.getPurpose()).isEqualTo(VerificationPurpose.SIGNUP);
            assertThat(verification.getCode()).isEqualTo(VerificationCodeFixture.code());
            assertThat(verification.getStatus()).isEqualTo(VerificationStatus.REQUESTED);
            assertThat(verification.getExpiresAt()).isEqualTo(REQUESTED_AT.plus(Duration.ofMinutes(5)));
            assertThat(verification.getFailedAttempts()).isZero();
        }

        @Test
        @DisplayName("필수 값이 없으면 예외가 발생한다")
        void requiredFields() {
            assertThatThrownBy(() -> Verification.create(null, EmailFixture.email(), VerificationPurpose.SIGNUP, VerificationCodeFixture.code(), REQUESTED_AT))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("인증 ID는 필수입니다.");
            assertThatThrownBy(() -> Verification.create(ID, null, VerificationPurpose.SIGNUP, VerificationCodeFixture.code(), REQUESTED_AT))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("이메일은 필수입니다.");
            assertThatThrownBy(() -> Verification.create(ID, EmailFixture.email(), null, VerificationCodeFixture.code(), REQUESTED_AT))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("인증 유형은 필수입니다.");
            assertThatThrownBy(() -> Verification.create(ID, EmailFixture.email(), VerificationPurpose.SIGNUP, null, REQUESTED_AT))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("인증번호는 필수입니다.");
            assertThatThrownBy(() -> Verification.create(ID, EmailFixture.email(), VerificationPurpose.SIGNUP, VerificationCodeFixture.code(), null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("인증번호 확인")
    class TryVerify {

        @Test
        @DisplayName("인증번호가 일치하면 인증 완료 상태가 되고 확인 시각으로부터 10분 뒤 만료된다")
        void success() {
            Verification verification = requested();
            Instant now = REQUESTED_AT.plus(Duration.ofMinutes(1));

            boolean result = verification.tryVerify(VerificationCodeFixture.code(), now);

            assertThat(result).isTrue();
            assertThat(verification.getStatus()).isEqualTo(VerificationStatus.VERIFIED);
            assertThat(verification.getExpiresAt()).isEqualTo(now.plus(VERIFIED_VALIDITY));
            assertThat(verification.getFailedAttempts()).isZero();
        }

        @Test
        @DisplayName("인증번호가 일치하지 않으면 false를 반환하고 실패 횟수가 증가한다")
        void mismatch() {
            Verification verification = requested();

            boolean result = verification.tryVerify(VerificationCodeFixture.wrongCode(), REQUESTED_AT);

            assertThat(result).isFalse();
            assertThat(verification.getStatus()).isEqualTo(VerificationStatus.REQUESTED);
            assertThat(verification.getExpiresAt()).isEqualTo(expiresAt());
            assertThat(verification.getFailedAttempts()).isEqualTo(1);
        }

        @Test
        @DisplayName("만료 직전에는 인증할 수 있다")
        void justBeforeExpiry() {
            Verification verification = requested();

            boolean result = verification.tryVerify(VerificationCodeFixture.code(), expiresAt().minusNanos(1));

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("만료 시각부터는 인증할 수 없고 실패 횟수도 증가하지 않는다")
        void expired() {
            Verification verification = requested();

            assertThatThrownBy(() -> verification.tryVerify(VerificationCodeFixture.wrongCode(), expiresAt()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(VerificationErrorCode.VERIFICATION_EXPIRED.getMessage());
            assertThat(verification.getFailedAttempts()).isZero();
        }

        @Test
        @DisplayName("최대 시도 횟수 직전까지 실패했더라도 일치하면 인증된다")
        void lastAttempt() {
            Verification verification = failed(MAX_ATTEMPTS - 1);

            boolean result = verification.tryVerify(VerificationCodeFixture.code(), REQUESTED_AT);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("최대 시도 횟수만큼 실패하면 일치하는 인증번호여도 인증할 수 없다")
        void tooManyAttempts() {
            Verification verification = failed(MAX_ATTEMPTS);

            assertThatThrownBy(() -> verification.tryVerify(VerificationCodeFixture.code(), REQUESTED_AT))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(VerificationErrorCode.TOO_MANY_ATTEMPTS.getMessage());
            assertThat(verification.getFailedAttempts()).isEqualTo(MAX_ATTEMPTS);
        }

        @Test
        @DisplayName("만료와 시도 횟수 초과가 겹치면 만료 예외가 먼저 발생한다")
        void expiredTakesPrecedence() {
            Verification verification = failed(MAX_ATTEMPTS);

            assertThatThrownBy(() -> verification.tryVerify(VerificationCodeFixture.code(), expiresAt()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(VerificationErrorCode.VERIFICATION_EXPIRED.getMessage());
        }

        @Test
        @DisplayName("이미 인증된 경우 다시 인증할 수 없다")
        void alreadyVerified() {
            Verification verification = verified();

            assertThatThrownBy(() -> verification.tryVerify(VerificationCodeFixture.code(), REQUESTED_AT))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(VerificationErrorCode.ALREADY_VERIFIED.getMessage());
        }
    }
}
