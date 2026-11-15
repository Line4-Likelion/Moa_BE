package com.moa.moa_be.domain.capture.controller;

import com.moa.moa_be.domain.capture.dto.CaptureActionRequest;
import com.moa.moa_be.domain.capture.dto.CaptureActionResponse;
import com.moa.moa_be.domain.capture.dto.CaptureDetailResponse;
import com.moa.moa_be.domain.capture.dto.CaptureListResponse;
import com.moa.moa_be.domain.capture.dto.ChecklistCheckRequest;
import com.moa.moa_be.domain.capture.dto.ChecklistCheckResponse;
import com.moa.moa_be.domain.capture.dto.FactsUpdateRequest;
import com.moa.moa_be.domain.capture.service.CaptureActionService;
import com.moa.moa_be.domain.capture.service.CaptureService;
import com.moa.moa_be.domain.capture.service.ChecklistService;
import com.moa.moa_be.domain.capture.service.FactService;
import com.moa.moa_be.global.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Capture", description = "실천함(C-01~C-15)·참고함 캡처 API")
@RestController
@RequestMapping("/captures")
@RequiredArgsConstructor
public class CaptureController {

    private final CaptureService captureService;
    private final CaptureActionService actionService;
    private final ChecklistService checklistService;
    private final FactService factService;

    @Operation(summary = "캡처 목록 (실천 목록·완료함·참고함)",
            description = "status=TODO 실천 목록(기본 정렬 deadline), DONE 완료함·REFERENCE 참고함(기본 정렬 recent). "
                    + "facets 는 실천 목록·완료함에서만 내려갑니다.")
    @GetMapping
    public BaseResponse<CaptureListResponse> list(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "TODO | DONE | REFERENCE", example = "TODO")
            @RequestParam(required = false) String status,
            @Parameter(description = "true 면 보류 중만, false 면 보류 아닌 것만, 없으면 전체")
            @RequestParam(required = false) Boolean snoozed,
            @Parameter(description = "deadline | recent") @RequestParam(required = false) String sort,
            @RequestParam(required = false) String cursor,
            @Parameter(description = "기본 20, 최대 100") @RequestParam(required = false) Integer limit,
            @Parameter(description = "그날 완료한 항목 (YYYY-MM-DD)")
            @RequestParam(name = "completed_on", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate completedOn) {
        return BaseResponse.success(
                captureService.list(userId, status, snoozed, sort, cursor, limit, completedOn));
    }

    @Operation(summary = "캡처 상세 (C-01~C-06)", description = "원본 사진은 서버에 없으므로 file_key 만 내려갑니다.")
    @GetMapping("/{id}")
    public BaseResponse<CaptureDetailResponse> detail(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return BaseResponse.success(captureService.detail(userId, id));
    }

    @Operation(summary = "상태 변경 (C-08~C-11)",
            description = "COMPLETE·SNOOZE·DISMISS 는 TODO 에서, REOPEN 은 DONE 에서 가능(이미 TODO 면 변경 없음). "
                    + "UNDO 는 직전 응답의 undo.token 으로 5초 안에. 허용되지 않으면 409 INVALID_STATE_TRANSITION.")
    @PostMapping("/{id}/actions")
    public BaseResponse<CaptureActionResponse> act(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                                                   @Valid @RequestBody CaptureActionRequest request) {
        return BaseResponse.success(actionService.act(userId, id, request));
    }

    @Operation(summary = "체크리스트 체크 (C-05)", description = "전부 체크해도 자동 완료하지 않고 all_done 만 true")
    @PatchMapping("/{id}/checklist/{itemId}")
    public BaseResponse<ChecklistCheckResponse> check(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                                                      @PathVariable String itemId,
                                                      @Valid @RequestBody ChecklistCheckRequest request) {
        return BaseResponse.success(checklistService.check(userId, id, itemId, request.checked()));
    }

    @Operation(summary = "핵심 정보 수정 (C-07)",
            description = "바꾼 항목만 전송. value 가 null 이면 AI 값으로 되돌림. key 가 deadline 이면 마감기한도 갱신.")
    @PatchMapping("/{id}/facts")
    public BaseResponse<CaptureDetailResponse> updateFacts(@AuthenticationPrincipal Long userId,
                                                           @PathVariable Long id,
                                                           @RequestBody FactsUpdateRequest request) {
        return BaseResponse.success(factService.update(userId, id, request));
    }


    @Operation(summary = "캡처 삭제", description = "앱이 기기에서 이미지 삭제에 성공한 뒤 호출. 서버 기록 삭제, 204")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        captureService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
