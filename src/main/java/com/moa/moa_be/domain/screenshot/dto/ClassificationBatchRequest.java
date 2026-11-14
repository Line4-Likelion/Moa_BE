package com.moa.moa_be.domain.screenshot.dto;

import com.moa.moa_be.domain.screenshot.entity.Classification;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ClassificationBatchRequest(@NotEmpty List<@NotNull @Valid Item> items) {
    public record Item(@NotBlank @Size(max = 255) String fileKey, @NotNull Classification classification) {}
}
