package com.zoonza.playground.verification.internal.application;

import com.zoonza.playground.shared.vo.Email;
import com.zoonza.playground.verification.internal.application.required.EmailSender;
import com.zoonza.playground.verification.internal.domain.VerificationCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VerificationMailSender {
    private final EmailSender emailSender;

    @Async
    public void sendCode(Email email, VerificationCode code) {
        String subject = "playground 이메일 인증코드입니다.";
        String text = "인증번호 [%s]를 입력해주세요.".formatted(code.value());

        try {
            emailSender.send(email.value(), subject, text);
        } catch (RuntimeException e) {
            log.error("메일 발송에 실패했습니다. to={}", email.value(), e);
        }
    }
}
