package com.moa.moa_be.domain.capture.dto;

import com.moa.moa_be.domain.capture.entity.Folder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FolderRequest(
        @NotBlank @Size(max = Folder.MAX_NAME_LENGTH) @Schema(example = "인테리어") String name
) {
}
