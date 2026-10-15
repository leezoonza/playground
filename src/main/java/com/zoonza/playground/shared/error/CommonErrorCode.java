package com.zoonza.playground.shared.error;

public enum CommonErrorCode implements ErrorCode {
    INVALID_INPUT("COMMON-001", "입력값이 올바르지 않습니다.", 400),
    INVALID_REQUEST_BODY("COMMON-002", "요청 본문을 읽을 수 없습니다.", 400),
    RESOURCE_NOT_FOUND("COMMON-003", "요청한 리소스를 찾을 수 없습니다.", 404),
    METHOD_NOT_ALLOWED("COMMON-004", "지원하지 않는 HTTP 메서드입니다.", 405),
    UNSUPPORTED_MEDIA_TYPE("COMMON-005", "지원하지 않는 콘텐츠 타입입니다.", 415),
    INTERNAL_SERVER_ERROR("COMMON-999", "서버 내부 오류가 발생했습니다.", 500);

    private final String code;
    private final String message;
    private final int status;

    CommonErrorCode(String code, String message, int status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public int getStatus() {
        return this.status;
    }
}
