package com.zoonza.playground.verification.internal.domain;

import com.zoonza.playground.shared.error.ErrorCode;

public enum VerificationErrorCode implements ErrorCode {
    VERIFICATION_NOT_FOUND("VERIFICATION-001", "인증 정보가 없거나 만료되었습니다. 다시 인증해 주세요.", 404),
    VERIFICATION_EXPIRED("VERIFICATION-002", "인증이 만료되었습니다. 다시 인증해 주세요.", 400),
    CODE_MISMATCH("VERIFICATION-003", "인증번호가 일치하지 않습니다.", 400),
    TOO_MANY_ATTEMPTS("VERIFICATION-004", "인증 시도 횟수를 초과했습니다. 다시 인증해 주세요.", 429),
    ALREADY_VERIFIED("VERIFICATION-005", "이미 완료된 인증입니다.", 409),
    NOT_VERIFIED("VERIFICATION-006", "인증번호 확인이 완료되지 않았습니다.", 400),
    ALREADY_CONSUMED("VERIFICATION-007", "이미 사용된 인증입니다. 다시 인증해 주세요.", 409),
    VERIFICATION_MISMATCH("VERIFICATION-008", "인증 정보가 요청과 일치하지 않습니다.", 400);

    private final String code;
    private final String message;
    private final int status;

    VerificationErrorCode(String code, String message, int status) {
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
