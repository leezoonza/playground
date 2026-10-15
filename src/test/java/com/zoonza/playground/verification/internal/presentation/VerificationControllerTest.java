package com.zoonza.playground.verification.internal.presentation;

import com.zoonza.playground.bootstrap.security.SecurityConfig;
import com.zoonza.playground.shared.error.BusinessException;
import com.zoonza.playground.shared.error.CommonErrorCode;
import com.zoonza.playground.shared.fixture.EmailFixture;
import com.zoonza.playground.verification.fixture.VerificationCodeFixture;
import com.zoonza.playground.verification.internal.application.dto.SendVerificationCodeCommand;
import com.zoonza.playground.verification.internal.application.dto.VerifyCodeCommand;
import com.zoonza.playground.verification.internal.application.provided.VerificationCommandUseCase;
import com.zoonza.playground.verification.internal.domain.VerificationErrorCode;
import com.zoonza.playground.verification.internal.domain.VerificationPurpose;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static com.zoonza.playground.verification.fixture.VerificationRequestFixture.sendRequest;
import static com.zoonza.playground.verification.fixture.VerificationRequestFixture.verifyRequest;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VerificationController.class)
@Import(SecurityConfig.class)
class VerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VerificationCommandUseCase verificationCommandUseCase;

    private ResultActions postJson(String uri, String body) throws Exception {
        return mockMvc.perform(post(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Nested
    @DisplayName("회원가입 인증번호 발송 API")
    class SendCodeForSignup {
        private static final String URI = "/api/verifications/signup";

        @Test
        @DisplayName("인증 없이 호출할 수 있고, 회원가입 용도로 발송을 요청한다")
        void success() throws Exception {
            postJson(URI, sendRequest())
                    .andExpect(status().isOk());

            then(verificationCommandUseCase).should()
                    .sendCode(new SendVerificationCodeCommand(EmailFixture.EMAIL_VALUE, VerificationPurpose.SIGNUP));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "invalid", "user@", "@example.com"})
        @DisplayName("이메일이 없거나 형식이 올바르지 않으면 400으로 응답한다")
        void invalidEmail(String email) throws Exception {
            postJson(URI, sendRequest(email))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(CommonErrorCode.INVALID_INPUT.getCode()))
                    .andExpect(jsonPath("$.errors[*].field", everyItem(is("email"))));

            then(verificationCommandUseCase).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("회원가입 인증번호 확인 API")
    class VerifyCodeForSignup {
        private static final String URI = "/api/verifications/signup/verify";

        @Test
        @DisplayName("인증 없이 호출할 수 있고, 회원가입 용도로 확인을 요청한다")
        void success() throws Exception {
            postJson(URI, verifyRequest())
                    .andExpect(status().isOk());

            then(verificationCommandUseCase).should()
                    .verifyCode(new VerifyCodeCommand(
                            EmailFixture.EMAIL_VALUE,
                            VerificationCodeFixture.CODE_VALUE,
                            VerificationPurpose.SIGNUP
                    ));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "abc", "12345", "1234567", "12a456"})
        @DisplayName("인증번호가 없거나 6자리 숫자가 아니면 400으로 응답한다")
        void invalidCode(String code) throws Exception {
            postJson(URI, verifyRequest(EmailFixture.EMAIL_VALUE, code))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(CommonErrorCode.INVALID_INPUT.getCode()))
                    .andExpect(jsonPath("$.errors[*].field", everyItem(is("code"))));

            then(verificationCommandUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("이메일 형식이 올바르지 않으면 400으로 응답한다")
        void invalidEmail() throws Exception {
            postJson(URI, verifyRequest("invalid", VerificationCodeFixture.CODE_VALUE))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[*].field", everyItem(is("email"))));

            then(verificationCommandUseCase).shouldHaveNoInteractions();
        }

        @ParameterizedTest
        @EnumSource(
                value = VerificationErrorCode.class,
                names = {"VERIFICATION_NOT_FOUND", "VERIFICATION_EXPIRED", "CODE_MISMATCH", "TOO_MANY_ATTEMPTS", "ALREADY_VERIFIED"}
        )
        @DisplayName("인증 실패 시 에러 코드에 맞는 상태와 코드로 응답한다")
        void verificationFailure(VerificationErrorCode errorCode) throws Exception {
            willThrow(new BusinessException(errorCode))
                    .given(verificationCommandUseCase).verifyCode(any());

            postJson(URI, verifyRequest())
                    .andExpect(status().is(errorCode.getStatus()))
                    .andExpect(jsonPath("$.code").value(errorCode.getCode()))
                    .andExpect(jsonPath("$.message").value(errorCode.getMessage()));
        }
    }
}
