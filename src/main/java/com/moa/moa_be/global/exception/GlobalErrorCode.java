package com.moa.moa_be.global.exception;

import com.moa.moa_be.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GlobalErrorCode implements BaseErrorCode {

    // 400 Bad Request
    VALIDATION_ERROR("VALIDATION_ERROR", "필수 값 누락 또는 형식 오류입니다.", HttpStatus.BAD_REQUEST),
    BATCH_LIMIT_EXCEEDED("BATCH_LIMIT_EXCEEDED", "한 번에 20장 넘게 분류 요청할 수 없습니다.", HttpStatus.BAD_REQUEST),

    // 401 Unauthorized
    UNAUTHORIZED("UNAUTHORIZED", "토큰이 없거나 만료되었습니다.", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "이메일 또는 비밀번호가 틀렸습니다.", HttpStatus.UNAUTHORIZED),

    // 403 Forbidden
    FORBIDDEN("FORBIDDEN", "다른 사용자의 데이터에 접근할 수 없습니다.", HttpStatus.FORBIDDEN),

    // 404 Not Found
    NOT_FOUND("NOT_FOUND", "요청한 리소스(캡처, 카테고리, 폴더, 작업)를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // 409 Conflict
    EMAIL_ALREADY_EXISTS("EMAIL_ALREADY_EXISTS", "이미 가입된 이메일입니다.", HttpStatus.CONFLICT),
    DUPLICATE_FILE_KEY("DUPLICATE_FILE_KEY", "이미 분류한 사진을 다시 분류할 수 없습니다.", HttpStatus.CONFLICT),
    DUPLICATE_NAME("DUPLICATE_NAME", "같은 이름의 카테고리·폴더가 이미 있습니다.", HttpStatus.CONFLICT),
    INVALID_STATE_TRANSITION("INVALID_STATE_TRANSITION", "지금 상태에서 할 수 없는 동작입니다.", HttpStatus.CONFLICT),

    // 413 / 415
    IMAGE_TOO_LARGE("IMAGE_TOO_LARGE", "이미지는 10MB를 초과할 수 없습니다.", HttpStatus.PAYLOAD_TOO_LARGE),
    UNSUPPORTED_IMAGE("UNSUPPORTED_IMAGE", "jpg, png, webp 형식만 지원합니다.", HttpStatus.UNSUPPORTED_MEDIA_TYPE),

    // 429
    RATE_LIMITED("RATE_LIMITED", "분석 요청이 너무 잦거나 하루 분석 상한을 초과했습니다.", HttpStatus.TOO_MANY_REQUESTS),

    // 500 / 502 / 504
    INTERNAL_ERROR("INTERNAL_ERROR", "서버 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    ANALYSIS_FAILED("ANALYSIS_FAILED", "AI 분석 작업에 실패했습니다.", HttpStatus.BAD_GATEWAY),
    ANALYSIS_TIMEOUT("ANALYSIS_TIMEOUT", "AI 분석 작업 시간이 초과되었습니다.", HttpStatus.GATEWAY_TIMEOUT);

    private final String code;
    private final String message;
    private final HttpStatus status;
}