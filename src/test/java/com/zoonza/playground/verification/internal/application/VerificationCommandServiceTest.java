package com.zoonza.playground.verification.internal.application;

import com.zoonza.playground.shared.error.BusinessException;
import com.zoonza.playground.shared.fixture.EmailFixture;
import com.zoonza.playground.support.MutableClock;
import com.zoonza.playground.verification.fake.FakeEmailSender;
import com.zoonza.playground.verification.fake.FakeVerificationStore;
import com.zoonza.playground.verification.fixture.VerificationCodeFixture;
import com.zoonza.playground.verification.internal.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;

import static com.zoonza.playground.verification.fixture.VerificationCommandFixture.sendCommand;
import static com.zoonza.playground.verification.fixture.VerificationCommandFixture.verifyCommand;
import static com.zoonza.playground.verification.fixture.VerificationFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class VerificationCommandServiceTest {
    private static final String NEW_CODE_VALUE = "999999";

    private MutableClock clock;
    private FakeVerificationStore verificationStore;
    private FakeEmailSender emailSender;
    private VerificationCommandService verificationCommandService;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(REQUESTED_AT);
        verificationStore = new FakeVerificationStore();
        emailSender = new FakeEmailSender();
        VerificationCodeGenerator codeGenerator = mock(VerificationCodeGenerator.class);
        given(codeGenerator.generate())
                .willReturn(VerificationCodeFixture.code(), new VerificationCode(NEW_CODE_VALUE));

        verificationCommandService = new VerificationCommandService(
                clock,
                new VerificationMailSender(emailSender),
                verificationStore,
                codeGenerator
        );
    }

    private Verification storedVerification() {
        return verificationStore.findBy(EmailFixture.email(), VerificationPurpose.SIGNUP).orElseThrow();
    }

    @Nested
    @DisplayName("인증번호 발송")
    class SendCode {

        @Test
        @DisplayName("인증 정보를 저장하고 인증번호를 메일로 발송한다")
        void sendCode() {
            verificationCommandService.sendCode(sendCommand());

            Verification verification = storedVerification();
            assertThat(verification.getId()).isNotBlank();
            assertThat(verification.getCode()).isEqualTo(VerificationCodeFixture.code());
            assertThat(verification.getStatus()).isEqualTo(VerificationStatus.REQUESTED);
            assertThat(verification.getExpiresAt()).isEqualTo(REQUESTED_AT.plus(CODE_VALIDITY));

            assertThat(emailSender.sentMails())
                    .singleElement()
                    .satisfies(mail -> {
                        assertThat(mail.to()).isEqualTo(EmailFixture.EMAIL_VALUE);
                        assertThat(mail.text()).contains(VerificationCodeFixture.CODE_VALUE);
                    });
        }

        @Test
        @DisplayName("이메일은 정규화되어 저장되고 발송된다")
        void normalizeEmail() {
            verificationCommandService.sendCode(sendCommand("  USER@Example.com "));

            assertThat(storedVerification().getEmail()).isEqualTo(EmailFixture.email());
            assertThat(emailSender.sentMails())
                    .singleElement()
                    .satisfies(mail -> assertThat(mail.to()).isEqualTo(EmailFixture.EMAIL_VALUE));
        }

        @Test
        @DisplayName("다시 발송하면 이전 인증번호는 더 이상 사용할 수 없다")
        void resend() {
            verificationCommandService.sendCode(sendCommand());
            verificationCommandService.sendCode(sendCommand());

            assertThatThrownBy(() -> verificationCommandService.verifyCode(verifyCommand(VerificationCodeFixture.CODE_VALUE)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(VerificationErrorCode.CODE_MISMATCH.getMessage());

            verificationCommandService.verifyCode(verifyCommand(NEW_CODE_VALUE));
            assertThat(storedVerification().getStatus()).isEqualTo(VerificationStatus.VERIFIED);
        }

        @Test
        @DisplayName("메일 발송에 실패해도 예외가 전파되지 않고 인증 정보는 저장된다")
        void mailFailure() {
            emailSender.failWith(new MailSendException("SMTP 연결 실패"));

            verificationCommandService.sendCode(sendCommand());

            assertThat(storedVerification().getStatus()).isEqualTo(VerificationStatus.REQUESTED);
            assertThat(emailSender.sentMails()).isEmpty();
        }

        @Test
        @DisplayName("이메일 형식이 올바르지 않으면 예외가 발생하고 아무것도 저장되지 않는다")
        void invalidEmail() {
            assertThatThrownBy(() -> verificationCommandService.sendCode(sendCommand("invalid-email")))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(verificationStore.saveCount()).isZero();
            assertThat(emailSender.sentMails()).isEmpty();
        }
    }

    @Nested
    @DisplayName("인증번호 확인")
    class VerifyCode {

        @BeforeEach
        void sendCode() {
            verificationCommandService.sendCode(sendCommand());
        }

        @Test
        @DisplayName("인증번호가 일치하면 인증 완료 상태로 저장된다")
        void success() {
            verificationCommandService.verifyCode(verifyCommand());

            Verification verification = storedVerification();
            assertThat(verification.getStatus()).isEqualTo(VerificationStatus.VERIFIED);
            assertThat(verification.getExpiresAt()).isEqualTo(REQUESTED_AT.plus(VERIFIED_VALIDITY));
            assertThat(verificationStore.saveCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("이메일 대소문자가 달라도 같은 인증 정보로 확인한다")
        void normalizeEmail() {
            verificationCommandService.verifyCode(verifyCommand("USER@EXAMPLE.COM", VerificationCodeFixture.CODE_VALUE));

            assertThat(storedVerification().getStatus()).isEqualTo(VerificationStatus.VERIFIED);
        }

        @Test
        @DisplayName("인증번호가 일치하지 않으면 실패 횟수를 저장한 뒤 예외가 발생한다")
        void mismatch() {
            assertThatThrownBy(() -> verificationCommandService.verifyCode(verifyCommand(VerificationCodeFixture.WRONG_CODE_VALUE)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(VerificationErrorCode.CODE_MISMATCH.getMessage());

            assertThat(storedVerification().getFailedAttempts()).isEqualTo(1);
            assertThat(verificationStore.saveCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("최대 시도 횟수만큼 실패하면 일치하는 인증번호여도 인증할 수 없다")
        void tooManyAttempts() {
            for (int i = 0; i < MAX_ATTEMPTS; i++) {
                assertThatThrownBy(() -> verificationCommandService.verifyCode(verifyCommand(VerificationCodeFixture.WRONG_CODE_VALUE)))
                        .hasMessage(VerificationErrorCode.CODE_MISMATCH.getMessage());
            }

            assertThatThrownBy(() -> verificationCommandService.verifyCode(verifyCommand()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(VerificationErrorCode.TOO_MANY_ATTEMPTS.getMessage());
        }

        @Test
        @DisplayName("인증번호 유효시간이 지나면 인증할 수 없다")
        void expired() {
            clock.advance(CODE_VALIDITY);

            assertThatThrownBy(() -> verificationCommandService.verifyCode(verifyCommand()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(VerificationErrorCode.VERIFICATION_EXPIRED.getMessage());
        }

        @Test
        @DisplayName("이미 인증된 경우 다시 인증할 수 없다")
        void alreadyVerified() {
            verificationCommandService.verifyCode(verifyCommand());

            assertThatThrownBy(() -> verificationCommandService.verifyCode(verifyCommand()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(VerificationErrorCode.ALREADY_VERIFIED.getMessage());
        }

        @Test
        @DisplayName("인증 요청 이력이 없으면 예외가 발생한다")
        void notFound() {
            assertThatThrownBy(() -> verificationCommandService.verifyCode(verifyCommand("other@example.com", VerificationCodeFixture.CODE_VALUE)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(VerificationErrorCode.VERIFICATION_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("인증번호 형식이 올바르지 않으면 예외가 발생하고 실패 횟수는 증가하지 않는다")
        void invalidCodeFormat() {
            assertThatThrownBy(() -> verificationCommandService.verifyCode(verifyCommand("abc")))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(storedVerification().getFailedAttempts()).isZero();
        }
    }
}
