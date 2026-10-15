package com.zoonza.playground.bootstrap.web;

import com.zoonza.playground.shared.error.BusinessException;
import com.zoonza.playground.shared.error.CommonErrorCode;
import com.zoonza.playground.shared.error.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.*;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 공통 예외 처리만 검증하기 위해 테스트 전용 컨트롤러를 사용하고, 보안 필터는 제외한다.
 */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandlerTest.TestController.class)
class GlobalExceptionHandlerTest {
    private static final String INTERNAL_MESSAGE = "외부에 노출되면 안 되는 메시지";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("BusinessException은 ErrorCode의 상태와 코드, 메시지로 응답한다")
    void businessException() throws Exception {
        ResultActions result = mockMvc.perform(get("/test/business"));

        expectError(result, TestErrorCode.CONFLICT)
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/test/illegal-argument", "/test/illegal-state", "/test/runtime"})
    @DisplayName("처리되지 않은 예외는 메시지를 숨기고 500으로 응답한다")
    void unexpectedException(String path) throws Exception {
        ResultActions result = mockMvc.perform(get(path));

        expectError(result, CommonErrorCode.INTERNAL_SERVER_ERROR)
                .andExpect(content().string(not(containsString(INTERNAL_MESSAGE))));
    }

    @Test
    @DisplayName("요청 본문 검증에 실패하면 400과 필드별 오류로 응답한다")
    void invalidRequestBody() throws Exception {
        ResultActions result = mockMvc.perform(post("/test/body")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": " "}
                        """));

        expectError(result, CommonErrorCode.INVALID_INPUT)
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("이름은 필수입니다."));
    }

    @Test
    @DisplayName("메서드 파라미터 검증에 실패하면 400과 파라미터별 오류로 응답한다")
    void invalidMethodParameter() throws Exception {
        ResultActions result = mockMvc.perform(get("/test/path/0"));

        expectError(result, CommonErrorCode.INVALID_INPUT)
                .andExpect(jsonPath("$.errors[0].field").value("value"))
                .andExpect(jsonPath("$.errors[0].message").value("1 이상이어야 합니다."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/test/param", "/test/param?count=abc"})
    @DisplayName("요청 파라미터가 없거나 타입이 맞지 않으면 400으로 응답한다")
    void invalidRequestParameter(String uri) throws Exception {
        ResultActions result = mockMvc.perform(get(uri));

        expectError(result, CommonErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("요청 본문을 읽을 수 없으면 400으로 응답한다")
    void unreadableBody() throws Exception {
        ResultActions result = mockMvc.perform(post("/test/body")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json"));

        expectError(result, CommonErrorCode.INVALID_REQUEST_BODY);
    }

    @Test
    @DisplayName("존재하지 않는 경로는 404로 응답한다")
    void notFound() throws Exception {
        ResultActions result = mockMvc.perform(get("/test/not-exists"));

        expectError(result, CommonErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("지원하지 않는 HTTP 메서드는 405로 응답한다")
    void methodNotAllowed() throws Exception {
        ResultActions result = mockMvc.perform(delete("/test/business"));

        expectError(result, CommonErrorCode.METHOD_NOT_ALLOWED);
    }

    @Test
    @DisplayName("지원하지 않는 콘텐츠 타입은 415로 응답한다")
    void unsupportedMediaType() throws Exception {
        ResultActions result = mockMvc.perform(post("/test/body")
                .contentType(MediaType.TEXT_PLAIN)
                .content("name"));

        expectError(result, CommonErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    private ResultActions expectError(ResultActions result, ErrorCode errorCode) throws Exception {
        return result
                .andExpect(status().is(errorCode.getStatus()))
                .andExpect(jsonPath("$.code").value(errorCode.getCode()))
                .andExpect(jsonPath("$.message").value(errorCode.getMessage()));
    }

    enum TestErrorCode implements ErrorCode {
        CONFLICT;

        @Override
        public String getCode() {
            return "TEST-001";
        }

        @Override
        public String getMessage() {
            return "테스트용 비즈니스 오류입니다.";
        }

        @Override
        public int getStatus() {
            return 409;
        }
    }

    record TestRequest(
            @NotBlank(message = "이름은 필수입니다.")
            String name
    ) {
    }

    @RestController
    static class TestController {

        @GetMapping("/test/business")
        void business() {
            throw new BusinessException(TestErrorCode.CONFLICT);
        }

        @GetMapping("/test/illegal-argument")
        void illegalArgument() {
            throw new IllegalArgumentException(INTERNAL_MESSAGE);
        }

        @GetMapping("/test/illegal-state")
        void illegalState() {
            throw new IllegalStateException(INTERNAL_MESSAGE);
        }

        @GetMapping("/test/runtime")
        void runtime() {
            throw new RuntimeException(INTERNAL_MESSAGE);
        }

        @PostMapping("/test/body")
        void body(@Valid @RequestBody TestRequest request) {
        }

        @GetMapping("/test/path/{value}")
        void path(@PathVariable @Min(value = 1, message = "1 이상이어야 합니다.") int value) {
        }

        @GetMapping("/test/param")
        void param(@RequestParam int count) {
        }
    }
}
