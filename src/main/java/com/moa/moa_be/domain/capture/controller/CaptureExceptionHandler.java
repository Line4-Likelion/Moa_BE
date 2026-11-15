package com.moa.moa_be.domain.capture.controller;

import com.moa.moa_be.domain.capture.exception.FieldValidationException;
import com.moa.moa_be.global.common.BaseResponse;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * C 파트 컨트롤러 전용: 400 VALIDATION_ERROR 에 필드별 사유(data.fields)를 담는다.
 * 그 외 예외는 전역 GlobalExceptionHandler 가 그대로 처리한다.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {CaptureController.class, FolderController.class, StatsController.class})
public class CaptureExceptionHandler {

    static final String MESSAGE = "입력값을 확인해주세요";

    @ExceptionHandler(FieldValidationException.class)
    public ResponseEntity<BaseResponse<Map<String, Object>>> handle(FieldValidationException ex) {
        log.warn("입력값 오류: {}", ex.getFields());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(BaseResponse.error(ex.getErrorCode().getCode(), MESSAGE, Map.of("fields", ex.getFields())));
    }
}
