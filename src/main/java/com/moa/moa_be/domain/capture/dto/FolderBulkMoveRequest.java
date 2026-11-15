package com.moa.moa_be.domain.capture.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record FolderBulkMoveRequest(
        @JsonProperty("capture_ids") @NotEmpty @Size(max = 100) List<@NotNull Long> captureIds,
        @JsonProperty("folder_id") @Schema(description = "null 이면 폴더에서 빼기", example = "1") Long folderId
) {
}
