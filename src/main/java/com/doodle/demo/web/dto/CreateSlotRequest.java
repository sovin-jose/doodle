package com.doodle.demo.web.dto;

import com.doodle.demo.domain.SlotStatus;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateSlotRequest(
        @NotNull Instant startTime,
        @NotNull Instant endTime,
        SlotStatus status
) {
}
