package com.doodle.demo.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AvailabilityResponse(
        Instant from,
        Instant to,
        List<UserAvailability> users,
        List<Interval> commonFree
) {
    public record Interval(Instant start, Instant end) {
    }

    public record UserAvailability(
            UUID userId,
            List<Interval> free,
            List<Interval> busy
    ) {
    }
}
