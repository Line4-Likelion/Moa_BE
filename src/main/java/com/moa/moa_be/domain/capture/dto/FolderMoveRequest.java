package com.moa.moa_be.domain.capture.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public record FolderMoveRequest(
        @JsonProperty("folder_id") @Schema(description = "null 이면 폴더에서 빼기", example = "1") Long folderId
) {
}
