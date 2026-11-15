package com.moa.moa_be.domain.capture.controller;

import com.moa.moa_be.domain.capture.dto.FolderRequest;
import com.moa.moa_be.domain.capture.dto.FolderResponse;
import com.moa.moa_be.domain.capture.service.FolderService;
import com.moa.moa_be.global.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Folder", description = "참고함 폴더 API")
@RestController
@RequestMapping("/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    @Operation(summary = "폴더 목록", description = "각 폴더의 참고 항목 수(count) 포함")
    @GetMapping
    public BaseResponse<List<FolderResponse>> list(@AuthenticationPrincipal Long userId) {
        return BaseResponse.success(folderService.list(userId));
    }

    @Operation(summary = "폴더 만들기", description = "같은 이름이면 409 DUPLICATE_NAME")
    @PostMapping
    public ResponseEntity<BaseResponse<FolderResponse>> create(@AuthenticationPrincipal Long userId,
                                                               @Valid @RequestBody FolderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(201, "폴더를 만들었습니다.", folderService.create(userId, request.name())));
    }

    @Operation(summary = "폴더 이름 바꾸기", description = "같은 이름이면 409 DUPLICATE_NAME")
    @PatchMapping("/{id}")
    public BaseResponse<FolderResponse> rename(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                                               @Valid @RequestBody FolderRequest request) {
        return BaseResponse.success(folderService.rename(userId, id, request.name()));
    }

    @Operation(summary = "폴더 삭제", description = "안의 항목은 지우지 않고 \"폴더 없음\"으로 옮김. 204")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        folderService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
