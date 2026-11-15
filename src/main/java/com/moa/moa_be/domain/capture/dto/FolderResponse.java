package com.moa.moa_be.domain.capture.dto;

import com.moa.moa_be.domain.capture.entity.Folder;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "참고함 폴더")
public record FolderResponse(@Schema(example = "1") Long id,
                             @Schema(example = "인테리어") String name,
                             @Schema(description = "폴더 안 참고 항목 수", example = "3") long count) {

    public static FolderResponse of(Folder folder, long count) {
        return new FolderResponse(folder.getId(), folder.getName(), count);
    }
}
