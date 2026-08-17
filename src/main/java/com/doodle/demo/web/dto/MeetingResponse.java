package com.doodle.demo.web.dto;

import com.doodle.demo.domain.Meeting;
import com.doodle.demo.domain.ResponseStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MeetingResponse(
        UUID id,
        UUID slotId,
        UUID organizerId,
        String title,
        String description,
        Instant startTime,
        Instant endTime,
        List<Participant> participants
) {
    public record Participant(UUID userId, ResponseStatus responseStatus) {
    }

    public static MeetingResponse of(Meeting meeting) {
        List<Participant> participants = meeting.getParticipants().stream()
                .map(p -> new Participant(p.getUser().getId(), p.getResponseStatus()))
                .toList();
        return new MeetingResponse(
                meeting.getId(),
                meeting.getSlot().getId(),
                meeting.getOrganizer().getId(),
                meeting.getTitle(),
                meeting.getDescription(),
                meeting.getSlot().getStartTime(),
                meeting.getSlot().getEndTime(),
                participants
        );
    }
}
