package com.moa.moa_be.domain.screenshot.controller;

import com.moa.moa_be.domain.screenshot.dto.ClassificationBatchRequest;
import com.moa.moa_be.domain.screenshot.dto.ClassificationResponse;
import com.moa.moa_be.domain.screenshot.dto.ClassificationChangeRequest;
import com.moa.moa_be.domain.screenshot.service.ScreenshotClassificationService;
import com.moa.moa_be.global.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Screenshot classification", description = "스크린샷 분류 결과 API")
@RestController
@RequestMapping("/screenshot-classifications")
@RequiredArgsConstructor
public class ScreenshotClassificationController {
    private final ScreenshotClassificationService service;

    @Operation(summary = "분류 결과 저장", description = "한 번에 최대 20개의 분류 결과를 저장합니다.")
    @PostMapping
    public ResponseEntity<BaseResponse<List<ClassificationResponse>>> save(
            @AuthenticationPrincipal Long userId, @Valid @RequestBody ClassificationBatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(201, "분류 결과를 저장했습니다.", service.save(userId, request)));
    }

    @Operation(summary = "내 분류 결과 조회")
    @GetMapping
    public BaseResponse<List<ClassificationResponse>> list(@AuthenticationPrincipal Long userId) {
        return BaseResponse.success(service.list(userId));
    }

    @Operation(summary = "실천·참고 분류 변경")
    @PatchMapping("/{id}")
    public BaseResponse<ClassificationResponse> change(@AuthenticationPrincipal Long userId,
            @PathVariable Long id, @Valid @RequestBody ClassificationChangeRequest request) {
        return BaseResponse.success("분류를 변경했습니다.", service.change(userId, id, request.classification()));
    }

    @Operation(summary = "분류 취소", description = "분류 기록을 삭제합니다. 기기에서 삭제된 사진은 복구하지 않습니다.")
    @DeleteMapping("/{id}")
    public BaseResponse<Void> cancel(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        service.cancel(userId, id);
        return BaseResponse.success("분류를 취소했습니다.", null);
    }
}
