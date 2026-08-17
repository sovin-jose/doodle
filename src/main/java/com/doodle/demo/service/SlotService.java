package com.doodle.demo.service;

import com.doodle.demo.domain.Calendar;
import com.doodle.demo.domain.Slot;
import com.doodle.demo.domain.SlotStatus;
import com.doodle.demo.repository.CalendarRepository;
import com.doodle.demo.repository.SlotRepository;
import com.doodle.demo.web.dto.CreateSlotRequest;
import com.doodle.demo.web.dto.SlotResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SlotService {

    private final SlotRepository slots;
    private final CalendarRepository calendars;

    public SlotService(SlotRepository slots, CalendarRepository calendars) {
        this.slots = slots;
        this.calendars = calendars;
    }

    @Transactional
    public SlotResponse create(UUID userId, CreateSlotRequest request) {
        Calendar calendar = calendars.findByOwnerId(userId)
                .orElseThrow(() -> new NotFoundException("calendar not found for user: " + userId));

        if (!request.endTime().isAfter(request.startTime())) {
            throw new ConflictException("endTime must be after startTime");
        }
        if (slots.existsOverlap(calendar.getId(), request.startTime(), request.endTime())) {
            throw new ConflictException("slot overlaps an existing slot");
        }
        SlotStatus status = request.status() == null ? SlotStatus.FREE : request.status();
        if (status == SlotStatus.BOOKED) {
            throw new ConflictException("cannot create a slot directly as BOOKED; book a meeting instead");
        }
        Slot slot = slots.save(new Slot(calendar, request.startTime(), request.endTime(), status));
        return SlotResponse.of(slot);
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> listForUser(UUID userId, Instant from, Instant to, SlotStatus status) {
        Calendar calendar = calendars.findByOwnerId(userId)
                .orElseThrow(() -> new NotFoundException("calendar not found for user: " + userId));
        List<Slot> found = status == null
                ? slots.findByCalendarIdAndStartTimeGreaterThanEqualAndEndTimeLessThanEqual(
                        calendar.getId(), from, to)
                : slots.findByCalendarIdAndStatusAndStartTimeGreaterThanEqualAndEndTimeLessThanEqual(
                        calendar.getId(), status, from, to);
        return found.stream().map(SlotResponse::of).toList();
    }

    @Transactional
    public void delete(UUID slotId) {
        Slot slot = slots.findById(slotId)
                .orElseThrow(() -> new NotFoundException("slot not found: " + slotId));
        if (slot.getStatus() == SlotStatus.BOOKED) {
            throw new ConflictException("cannot delete a booked slot; cancel the meeting first");
        }
        slots.delete(slot);
    }

    @Transactional
    public SlotResponse updateStatus(UUID slotId, SlotStatus status) {
        Slot slot = slots.findById(slotId)
                .orElseThrow(() -> new NotFoundException("slot not found: " + slotId));
        if (status == SlotStatus.BOOKED || slot.getStatus() == SlotStatus.BOOKED) {
            throw new ConflictException("BOOKED status is managed through meeting booking");
        }
        slot.setStatus(status);
        return SlotResponse.of(slot);
    }
}
