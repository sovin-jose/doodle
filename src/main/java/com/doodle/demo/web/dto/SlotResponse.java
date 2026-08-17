package com.doodle.demo.web.dto;

import com.doodle.demo.domain.Slot;
import com.doodle.demo.domain.SlotStatus;

import java.time.Instant;
import java.util.UUID;

public record SlotResponse(
        UUID id,
        UUID calendarId,
        Instant startTime,
        Instant endTime,
        SlotStatus status
) {
    public static SlotResponse of(Slot slot) {
        return new SlotResponse(
                slot.getId(),
                slot.getCalendar().getId(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getStatus()
        );
    }
}
