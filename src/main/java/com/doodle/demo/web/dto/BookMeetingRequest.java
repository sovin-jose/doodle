package com.doodle.demo.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record BookMeetingRequest(
        @NotNull UUID slotId,
        @NotNull UUID organizerId,
        @NotBlank String title,
        String description,
        List<UUID> participantIds
) {
}
