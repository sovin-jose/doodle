package com.doodle.demo.web.dto;

import com.doodle.demo.domain.Calendar;
import com.doodle.demo.domain.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        UUID calendarId,
        String timezone,
        Instant createdAt
) {
    public static UserResponse of(User user, Calendar calendar) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                calendar.getId(),
                calendar.getTimezone().getId(),
                user.getCreatedAt()
        );
    }
}
