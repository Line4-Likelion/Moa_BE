package com.moa.moa_be.global.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "공통 에러 응답 형식")
public class BaseResponse {
    private ErrorDetail error;

    @Getter
    @AllArgsConstructor
    public static class ErrorDetail {
        private String code;
        private String message;
        private Object details; // Validation의 경우 Map<String, String> 등 사용
    }

    public static BaseResponse of(String code, String message, Object details) {
        return new BaseResponse(new ErrorDetail(code, message, details));
    }
}