package com.moa.moa_be.domain.capture.exception;

import com.moa.moa_be.global.exception.CustomException;
import com.moa.moa_be.global.exception.GlobalErrorCode;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;

/** 400 VALIDATION_ERROR + 필드별 사유 (응답 data.fields) */
@Getter
public class FieldValidationException extends CustomException {

    private final Map<String, String> fields;

    public FieldValidationException(Map<String, String> fields) {
        super(GlobalErrorCode.VALIDATION_ERROR);
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields)); // 보낸 순서 유지
    }

    public static FieldValidationException of(String field, String reason) {
        return new FieldValidationException(Map.of(field, reason));
    }
}
