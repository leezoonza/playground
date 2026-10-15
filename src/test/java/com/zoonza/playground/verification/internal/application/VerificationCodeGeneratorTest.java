package com.zoonza.playground.verification.internal.application;

import com.zoonza.playground.verification.internal.domain.VerificationCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

import static org.assertj.core.api.Assertions.assertThat;

class VerificationCodeGeneratorTest {
    private final VerificationCodeGenerator verificationCodeGenerator = new VerificationCodeGenerator();

    @RepeatedTest(5)
    @DisplayName("6자리 숫자 인증번호를 생성한다")
    void generate() {
        VerificationCode code = verificationCodeGenerator.generate();

        assertThat(code.value()).matches("\\d{6}");
    }
}
