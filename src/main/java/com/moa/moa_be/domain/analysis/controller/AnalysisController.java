package com.moa.moa_be.domain.analysis.controller;

import com.moa.moa_be.domain.analysis.dto.AnalysisJobResponse;
import com.moa.moa_be.domain.analysis.dto.AnalysisJobStatusResponse;
import com.moa.moa_be.domain.analysis.dto.CaptureAnalysisResponse;
import com.moa.moa_be.domain.analysis.dto.CategoryChangeRequest;
import com.moa.moa_be.domain.analysis.service.AnalysisJobService;
import com.moa.moa_be.domain.capture.dto.CaptureSummaryResponse;
import com.moa.moa_be.global.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "AI Analysis", description = "AI 분석 요청·상태 조회·카테고리 변경 (B-1, B-2, B-4)")
@RestController
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisJobService analysisJobService;

    @Operation(summary = "분석 요청",
            description = "실천으로 분류한 사진을 올리면 바로 202와 jobId를 돌려주고 백그라운드에서 분석합니다. "
                    + "jpg·png·webp, 10MB 이하. 분석이 끝나면 이미지는 버립니다.")
    @PostMapping(value = "/captures/{id}/analysis", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse<AnalysisJobResponse>> requestAnalysis(
            @AuthenticationPrincipal Long userId,
            @PathVariable("id") Long captureId,
            @RequestPart("image") MultipartFile image) {
        AnalysisJobResponse response = analysisJobService.request(userId, captureId, image);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(BaseResponse.success(202, "분석 요청이 접수되었습니다.", response));
    }

    @Operation(summary = "분석 상태 조회", description = "2초 간격으로 조회. 예: /analysis-jobs?ids=1,2")
    @GetMapping("/analysis-jobs")
    public ResponseEntity<BaseResponse<AnalysisJobStatusResponse>> getJobs(
            @AuthenticationPrincipal Long userId,
            @RequestParam("ids") List<Long> jobIds) {
        return ResponseEntity.ok(BaseResponse.success(analysisJobService.getJobs(userId, jobIds)));
    }

    @Operation(summary = "캡처 분석 결과 조회", description = "마감기한·카테고리·추출 정보·가이드·체크리스트")
    @GetMapping("/captures/{id}/analysis")
    public ResponseEntity<BaseResponse<CaptureAnalysisResponse>> getResult(
            @AuthenticationPrincipal Long userId,
            @PathVariable("id") Long captureId) {
        return ResponseEntity.ok(BaseResponse.success(analysisJobService.getResult(userId, captureId)));
    }

    @Operation(summary = "카테고리 수동 변경 (B-4)",
            description = "실천 사진은 분석 결과의 카테고리를 바꾸고(category_source=USER), 참고 사진은 참고함 카테고리를 바꿉니다. 응답은 캡처 요약형.")
    @PatchMapping("/captures/{id}/category")
    public ResponseEntity<BaseResponse<CaptureSummaryResponse>> changeCategory(
            @AuthenticationPrincipal Long userId,
            @PathVariable("id") Long captureId,
            @Valid @RequestBody CategoryChangeRequest request) {
        return ResponseEntity.ok(BaseResponse.success("카테고리가 변경되었습니다.",
                analysisJobService.changeCategory(userId, captureId, request.categoryId())));
    }
}
