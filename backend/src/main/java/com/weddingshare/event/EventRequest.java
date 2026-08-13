package com.weddingshare.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record EventRequest(
        @NotBlank String name,
        @NotNull LocalDate eventDate,
        String coverImageKey,
        Boolean active
) {
}
