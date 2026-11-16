package com.moa.moa_be.domain.capture.controller;

import com.moa.moa_be.domain.capture.dto.CompletionStatsResponse;
import com.moa.moa_be.domain.capture.service.CompletionStatsService;
import com.moa.moa_be.global.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Stats", description = "완료율 (C-15)")
@RestController
@RequiredArgsConstructor
public class StatsController {

    private final CompletionStatsService completionStatsService;

    @Operation(summary = "완료율", description = "완료 ÷ (완료 + 실천 대기). 대상이 없으면 rate 는 null. weekly 는 최근 6주")
    @GetMapping("/stats/completion")
    public BaseResponse<CompletionStatsResponse> completion(@AuthenticationPrincipal Long userId) {
        return BaseResponse.success(completionStatsService.get(userId));
    }
}
