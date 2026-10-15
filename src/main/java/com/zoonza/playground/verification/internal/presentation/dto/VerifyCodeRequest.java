package com.zoonza.playground.verification.internal.presentation.dto;

import com.zoonza.playground.verification.internal.application.dto.VerifyCodeCommand;
import com.zoonza.playground.verification.internal.domain.VerificationPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyCodeRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Pattern(
                regexp = "^[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+$",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "이메일 형식이 올바르지 않습니다."
        )
        String email,

        @NotBlank(message = "인증코드는 필수입니다.")
        @Pattern(regexp = "^\\d{6}$", message = "인증코드는 6자리 숫자여야 합니다.")
        String code
) {
    public VerifyCodeCommand toCommand(VerificationPurpose purpose) {
        return new VerifyCodeCommand(email, code, purpose);
    }
}
