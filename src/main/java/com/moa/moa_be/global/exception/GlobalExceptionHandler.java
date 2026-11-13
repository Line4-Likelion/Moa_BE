package com.moa.moa_be.global.exception;

import com.moa.moa_be.global.common.BaseResponse;
import com.moa.moa_be.global.exception.model.BaseErrorCode;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. 커스텀 예외 처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<BaseResponse> handleCustomException(CustomException ex) {
        BaseErrorCode errorCode = ex.getErrorCode();
        log.warn("CustomException 발생: code={}, message={}", errorCode.getCode(), errorCode.getMessage());
        return ResponseEntity.status(errorCode.getStatus())
                .body(BaseResponse.of(errorCode.getCode(), errorCode.getMessage(), null));
    }

    // 2. Validation 실패 (@Valid 검증 실패) -> details.fields에 필드별 사유 매핑
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> details = new HashMap<>();
        details.put("fields", fieldErrors);

        log.warn("Validation 오류 발생: {}", fieldErrors);
        return ResponseEntity.badRequest()
                .body(BaseResponse.of(
                        GlobalErrorCode.VALIDATION_ERROR.getCode(),
                        GlobalErrorCode.VALIDATION_ERROR.getMessage(),
                        details
                ));
    }

    // 3. 요청 본문 JSON 파싱 실패
    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ResponseEntity<BaseResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("HttpMessageNotReadableException 발생: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(BaseResponse.of(
                        GlobalErrorCode.VALIDATION_ERROR.getCode(),
                        "요청 본문 형식이 올바르지 않습니다.",
                        null
                ));
    }

    // 4. 지원하지 않는 HTTP Method 호출
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<BaseResponse> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        log.warn("HttpRequestMethodNotSupportedException 발생: {}", ex.getMessage());
        return ResponseEntity.status(405)
                .body(BaseResponse.of(
                        GlobalErrorCode.VALIDATION_ERROR.getCode(),
                        "지원하지 않는 HTTP 메서드입니다.",
                        null
                ));
    }

    // 5. 메서드 인자 타입 불일치
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<BaseResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.warn("MethodArgumentTypeMismatchException 발생: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(BaseResponse.of(
                        GlobalErrorCode.VALIDATION_ERROR.getCode(),
                        "요청 인자의 데이터 타입이 올바르지 않습니다.",
                        null
                ));
    }

    // 6. 예상치 못한 서버 예외 (Internal Error)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse> handleException(Exception ex) {
        log.error("Server 오류 발생: ", ex);
        return ResponseEntity.status(GlobalErrorCode.INTERNAL_ERROR.getStatus())
                .body(BaseResponse.of(
                        GlobalErrorCode.INTERNAL_ERROR.getCode(),
                        GlobalErrorCode.INTERNAL_ERROR.getMessage(),
                        null
                ));
    }
}