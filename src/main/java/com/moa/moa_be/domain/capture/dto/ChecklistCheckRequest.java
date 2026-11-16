package com.moa.moa_be.domain.capture.dto;

import jakarta.validation.constraints.NotNull;

public record ChecklistCheckRequest(@NotNull Boolean checked) {
}
