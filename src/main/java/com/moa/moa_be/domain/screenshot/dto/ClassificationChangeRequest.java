package com.moa.moa_be.domain.screenshot.dto;

import com.moa.moa_be.domain.screenshot.entity.Classification;
import jakarta.validation.constraints.NotNull;

public record ClassificationChangeRequest(@NotNull Classification classification) {}
