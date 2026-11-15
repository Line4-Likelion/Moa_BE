package com.moa.moa_be.domain.analysis.controller;

import com.moa.moa_be.domain.analysis.service.AnalysisService;
import com.moa.moa_be.domain.analysis.service.AnalysisService.AnalysisRun;
import com.moa.moa_be.global.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Screenshot 엔티티 없이 분석 파이프라인만 확인하는 개발용 API.
 * 실제 앱은 POST /captures/{id}/analysis (비동기) 를 사용.
 */
@Tag(name = "AI Analysis (Dev)", description = "개발·시연용 AI 분석 테스트 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/dev/analysis")
public class AnalysisDevController {

    private final AnalysisService analysisService;

    @Operation(summary = "이미지 바로 분석 (개발용)",
            description = "스크린샷을 올리면 OCR 블록과 검증된 분석 결과를 동기로 반환합니다. DB에 저장하지 않습니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse<AnalysisRun>> analyze(
            @AuthenticationPrincipal Long userId,
            @RequestPart("image") MultipartFile image) {
        AnalysisRun run = analysisService.analyze(userId, image);
        return ResponseEntity.ok(BaseResponse.success("분석이 완료되었습니다.", run));
    }
}
