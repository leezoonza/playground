package com.zoonza.playground.verification.internal.application;

import com.zoonza.playground.shared.error.BusinessException;
import com.zoonza.playground.shared.vo.Email;
import com.zoonza.playground.verification.internal.application.dto.SendVerificationCodeCommand;
import com.zoonza.playground.verification.internal.application.dto.VerifyCodeCommand;
import com.zoonza.playground.verification.internal.application.provided.VerificationCommandUseCase;
import com.zoonza.playground.verification.internal.application.required.VerificationStore;
import com.zoonza.playground.verification.internal.domain.Verification;
import com.zoonza.playground.verification.internal.domain.VerificationCode;
import com.zoonza.playground.verification.internal.domain.VerificationErrorCode;
import com.zoonza.playground.verification.internal.domain.VerificationPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationCommandService implements VerificationCommandUseCase {
    private final Clock clock;
    private final VerificationMailSender mailSender;
    private final VerificationStore verificationStore;
    private final VerificationCodeGenerator codeGenerator;

    @Override
    public void sendCode(SendVerificationCodeCommand command) {
        Email email = new Email(command.email());

        Verification verification = Verification.create(
                UUID.randomUUID().toString(),
                email,
                command.purpose(),
                codeGenerator.generate(),
                clock.instant()
        );

        verificationStore.save(verification);

        mailSender.sendCode(verification.getEmail(), verification.getCode());
    }

    @Override
    public void verifyCode(VerifyCodeCommand command) {
        Email email = new Email(command.email());
        VerificationCode code = new VerificationCode(command.code());

        Verification verification = findVerification(email, command.purpose());

        boolean verified = verification.tryVerify(code, clock.instant());

        verificationStore.save(verification);

        if (!verified) {
            throw new BusinessException(VerificationErrorCode.CODE_MISMATCH);
        }
    }

    private Verification findVerification(Email email, VerificationPurpose purpose) {
        return verificationStore.findBy(email, purpose)
                .orElseThrow(() -> new BusinessException(VerificationErrorCode.VERIFICATION_NOT_FOUND));
    }
}
