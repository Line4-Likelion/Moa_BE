package com.moa.moa_be.global.exception;

import com.moa.moa_be.global.common.BaseResponse;
import com.moa.moa_be.global.exception.model.BaseErrorCode;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. 커스텀 예외
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<BaseResponse<Object>> handleCustomException(CustomException ex) {
        BaseErrorCode errorCode = ex.getErrorCode();
        log.warn("CustomException 발생: code={}, message={}", errorCode.getCode(), errorCode.getMessage());
        return ResponseEntity.status(errorCode.getStatus())
                .body(BaseResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }

    // 2. Validation 실패 (@Valid 검증 실패)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<?>> handleValidationException(
            MethodArgumentNotValidException ex) {
        String errorMessages =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(e -> String.format("[%s] %s", e.getField(), e.getDefaultMessage()))
                        .collect(Collectors.joining(" / "));
        log.warn("Validation 오류 발생: {}", errorMessages);
        return ResponseEntity.badRequest().body(
                BaseResponse.error(GlobalErrorCode.VALIDATION_ERROR.getCode(), errorMessages));
    }

    // 3. 요청 본문 JSON 파싱 실패
    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ResponseEntity<BaseResponse<?>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex) {
        log.warn("HttpMessageNotReadableException 발생: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(
                BaseResponse.error(GlobalErrorCode.VALIDATION_ERROR.getCode(),
                        "요청 본문 형식이 올바르지 않습니다."));
    }

    // 4. 필수 multipart 요청 파트 누락
    @ExceptionHandler(MissingServletRequestPartException.class)
    protected ResponseEntity<BaseResponse<?>> handleMissingServletRequestPartException(
            MissingServletRequestPartException ex) {
        log.warn("MissingServletRequestPartException 발생: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(
                BaseResponse.error(GlobalErrorCode.VALIDATION_ERROR.getCode(),
                        "필수 파일이 누락되었습니다: " + ex.getRequestPartName()));
    }

    // 5. 지원하지 않는 HTTP Method 호출
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<BaseResponse<?>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex) {
        log.warn("HttpRequestMethodNotSupportedException 발생: {}", ex.getMessage());
        return ResponseEntity.status(405)
                .body(BaseResponse.error(GlobalErrorCode.VALIDATION_ERROR.getCode(),
                        "지원하지 않는 HTTP 메서드입니다."));
    }

    // 6. 메서드 인자 타입 불일치
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<BaseResponse<?>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex) {
        log.warn("MethodArgumentTypeMismatchException 발생: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(
                BaseResponse.error(GlobalErrorCode.VALIDATION_ERROR.getCode(),
                        "요청 인자의 데이터 타입이 올바르지 않습니다."));
    }

    // 7. 정적 파일/경로 누락
    @ExceptionHandler(NoResourceFoundException.class)
    protected ResponseEntity<BaseResponse<?>> handleNoResourceFoundException(
            NoResourceFoundException ex) {
        log.warn("NoResourceFoundException 발생: {}", ex.getMessage());
        return ResponseEntity.status(404)
                .body(BaseResponse.error("RESOURCE_NOT_FOUND", "요청하신 리소스를 찾을 수 없습니다."));
    }

    // 8. 예상치 못한 서버 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<?>> handleException(Exception ex) {
        log.error("Server 오류 발생: ", ex);
        return ResponseEntity.status(500)
                .body(BaseResponse.error("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."));
    }
}