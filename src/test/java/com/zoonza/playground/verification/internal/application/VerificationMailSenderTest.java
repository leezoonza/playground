package com.zoonza.playground.verification.internal.application;

import com.zoonza.playground.shared.fixture.EmailFixture;
import com.zoonza.playground.verification.fake.FakeEmailSender;
import com.zoonza.playground.verification.fixture.VerificationCodeFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class VerificationMailSenderTest {
    private FakeEmailSender emailSender;
    private VerificationMailSender verificationMailSender;

    @BeforeEach
    void setUp() {
        emailSender = new FakeEmailSender();
        verificationMailSender = new VerificationMailSender(emailSender);
    }

    @Test
    @DisplayName("인증번호가 담긴 메일을 수신자에게 발송한다")
    void sendCode() {
        verificationMailSender.sendCode(EmailFixture.email(), VerificationCodeFixture.code());

        assertThat(emailSender.sentMails())
                .singleElement()
                .satisfies(mail -> {
                    assertThat(mail.to()).isEqualTo(EmailFixture.EMAIL_VALUE);
                    assertThat(mail.subject()).isEqualTo("playground 이메일 인증코드입니다.");
                    assertThat(mail.text()).isEqualTo("인증번호 [%s]를 입력해주세요.".formatted(VerificationCodeFixture.CODE_VALUE));
                });
    }

    @Test
    @DisplayName("메일 발송에 실패해도 예외를 전파하지 않는다")
    void swallowFailure() {
        emailSender.failWith(new MailSendException("SMTP 연결 실패"));

        assertThatCode(() -> verificationMailSender.sendCode(EmailFixture.email(), VerificationCodeFixture.code()))
                .doesNotThrowAnyException();
    }
}
