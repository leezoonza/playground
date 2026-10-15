package com.zoonza.playground.verification.internal.presentation.dto;

import com.zoonza.playground.verification.internal.application.dto.SendVerificationCodeCommand;
import com.zoonza.playground.verification.internal.domain.VerificationPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SendVerificationCodeRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Pattern(
                regexp = "^[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+$",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "이메일 형식이 올바르지 않습니다."
        )
        String email
) {
        public SendVerificationCodeCommand toCommand(VerificationPurpose purpose) {
                return new SendVerificationCodeCommand(email, purpose);
        }
}
