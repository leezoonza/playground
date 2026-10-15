package com.zoonza.playground.verification.internal.presentation;

import com.zoonza.playground.verification.internal.application.provided.VerificationCommandUseCase;
import com.zoonza.playground.verification.internal.domain.VerificationPurpose;
import com.zoonza.playground.verification.internal.presentation.dto.SendVerificationCodeRequest;
import com.zoonza.playground.verification.internal.presentation.dto.VerifyCodeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/verifications")
public class VerificationController {
    private final VerificationCommandUseCase verificationCommandUseCase;

    @PostMapping("/signup")
    public ResponseEntity<Void> sendCodeForSignup(
            @Valid @RequestBody SendVerificationCodeRequest request
    ) {
        verificationCommandUseCase.sendCode(request.toCommand(VerificationPurpose.SIGNUP));

        return ResponseEntity.ok().build();
    }

    @PostMapping("/signup/verify")
    public ResponseEntity<Void> verifyCodeForSignup(
            @Valid @RequestBody VerifyCodeRequest request
    ) {
        verificationCommandUseCase.verifyCode(request.toCommand(VerificationPurpose.SIGNUP));

        return ResponseEntity.ok().build();
    }
}
