package com.zoonza.playground.verification.internal.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerificationCodeTest {

    @Test
    @DisplayName("6자리 숫자로 인증번호를 생성한다")
    void create() {
        VerificationCode code = new VerificationCode("012345");

        assertThat(code.value()).isEqualTo("012345");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"12345", "1234567", "12a456", " 123456", "123456 ", "12 456", "١٢٣٤٥٦"})
    @DisplayName("6자리 숫자가 아니면 예외가 발생한다")
    void invalidFormat(String value) {
        assertThatThrownBy(() -> new VerificationCode(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("인증번호 형식이 올바르지 않습니다.");
    }

    @Test
    @DisplayName("값이 같으면 같은 인증번호다")
    void equality() {
        assertThat(new VerificationCode("123456")).isEqualTo(new VerificationCode("123456"));
    }
}
