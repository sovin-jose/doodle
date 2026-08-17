package com.doodle.demo.service;

import com.doodle.demo.domain.Slot;
import com.doodle.demo.domain.SlotStatus;
import com.doodle.demo.repository.SlotRepository;
import com.doodle.demo.web.dto.AvailabilityResponse;
import com.doodle.demo.web.dto.AvailabilityResponse.Interval;
import com.doodle.demo.web.dto.AvailabilityResponse.UserAvailability;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AvailabilityService {

    private final SlotRepository slots;

    public AvailabilityService(SlotRepository slots) {
        this.slots = slots;
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse aggregate(Collection<UUID> userIds, Instant from, Instant to) {
        if (userIds == null || userIds.isEmpty()) {
            throw new IllegalArgumentException("userIds must not be empty");
        }
        if (!to.isAfter(from)) {
            throw new IllegalArgumentException("`to` must be after `from`");
        }

        List<Slot> found = slots.findOverlappingForUsers(userIds, from, to);

        // Preserve request order in the response.
        Map<UUID, List<Interval>> freeByUser = new LinkedHashMap<>();
        Map<UUID, List<Interval>> busyByUser = new LinkedHashMap<>();
        for (UUID userId : userIds) {
            freeByUser.put(userId, new ArrayList<>());
            busyByUser.put(userId, new ArrayList<>());
        }

        for (Slot slot : found) {
            UUID ownerId = slot.getCalendar().getOwner().getId();
            Interval clipped = new Interval(
                    max(slot.getStartTime(), from),
                    min(slot.getEndTime(), to));
            if (slot.getStatus() == SlotStatus.FREE) {
                freeByUser.get(ownerId).add(clipped);
            } else {
                busyByUser.get(ownerId).add(clipped);
            }
        }

        List<UserAvailability> perUser = new ArrayList<>(userIds.size());
        List<List<Interval>> allFree = new ArrayList<>(userIds.size());
        for (UUID userId : userIds) {
            List<Interval> free = mergeAdjacent(freeByUser.get(userId));
            List<Interval> busy = mergeAdjacent(busyByUser.get(userId));
            perUser.add(new UserAvailability(userId, free, busy));
            allFree.add(free);
        }

        List<Interval> common = intersectAll(allFree);
        return new AvailabilityResponse(from, to, perUser, common);
    }

    /**
     * Sort by start, then merge intervals that touch or overlap.
     */
    private static List<Interval> mergeAdjacent(List<Interval> intervals) {
        if (intervals.isEmpty()) {
            return List.of();
        }
        intervals.sort(Comparator.comparing(Interval::start));
        List<Interval> merged = new ArrayList<>();
        Instant curStart = intervals.get(0).start();
        Instant curEnd = intervals.get(0).end();
        for (int i = 1; i < intervals.size(); i++) {
            Interval next = intervals.get(i);
            if (!next.start().isAfter(curEnd)) {
                curEnd = max(curEnd, next.end());
            } else {
                merged.add(new Interval(curStart, curEnd));
                curStart = next.start();
                curEnd = next.end();
            }
        }
        merged.add(new Interval(curStart, curEnd));
        return merged;
    }

    /**
     * Intersect a list of per-user free interval lists to compute the common free windows.
     * Assumes each list is already sorted and merged.
     */
    private static List<Interval> intersectAll(List<List<Interval>> lists) {
        if (lists.isEmpty()) {
            return List.of();
        }
        List<Interval> acc = new ArrayList<>(lists.get(0));
        for (int i = 1; i < lists.size(); i++) {
            acc = intersect(acc, lists.get(i));
            if (acc.isEmpty()) {
                return List.of();
            }
        }
        return acc;
    }

    /**
     * Two-pointer intersection of two sorted, non-overlapping interval lists.
     */
    private static List<Interval> intersect(List<Interval> a, List<Interval> b) {
        List<Interval> out = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < a.size() && j < b.size()) {
            Interval x = a.get(i);
            Interval y = b.get(j);
            Instant start = max(x.start(), y.start());
            Instant end = min(x.end(), y.end());
            if (start.isBefore(end)) {
                out.add(new Interval(start, end));
            }
            if (x.end().isBefore(y.end())) {
                i++;
            } else {
                j++;
            }
        }
        return out;
    }

    private static Instant max(Instant a, Instant b) {
        return a.isAfter(b) ? a : b;
    }

    private static Instant min(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }
}
