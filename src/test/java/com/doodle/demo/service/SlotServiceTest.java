package com.doodle.demo.service;

import com.doodle.demo.domain.Calendar;
import com.doodle.demo.domain.Slot;
import com.doodle.demo.domain.SlotStatus;
import com.doodle.demo.domain.User;
import com.doodle.demo.repository.CalendarRepository;
import com.doodle.demo.repository.SlotRepository;
import com.doodle.demo.web.dto.CreateSlotRequest;
import com.doodle.demo.web.dto.SlotResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlotServiceTest {

    @Mock
    SlotRepository slots;

    @Mock
    CalendarRepository calendars;

    @InjectMocks
    SlotService service;

    User owner;
    Calendar calendar;

    @BeforeEach
    void setUp() {
        owner = new User("Ada", "ada@example.com");
        calendar = new Calendar(owner, null);
    }

    @Test
    void create_rejects_when_end_not_after_start() {
        Instant now = Instant.parse("2026-09-01T09:00:00Z");
        CreateSlotRequest req = new CreateSlotRequest(now, now, SlotStatus.FREE);
        when(calendars.findByOwnerId(owner.getId())).thenReturn(Optional.of(calendar));

        assertThatThrownBy(() -> service.create(owner.getId(), req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("endTime must be after startTime");

        verify(slots, never()).save(any());
    }

    @Test
    void create_rejects_overlap() {
        Instant start = Instant.parse("2026-09-01T09:00:00Z");
        Instant end = start.plus(30, ChronoUnit.MINUTES);
        CreateSlotRequest req = new CreateSlotRequest(start, end, SlotStatus.FREE);
        when(calendars.findByOwnerId(owner.getId())).thenReturn(Optional.of(calendar));
        when(slots.existsOverlap(calendar.getId(), start, end)).thenReturn(true);

        assertThatThrownBy(() -> service.create(owner.getId(), req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("overlap");

        verify(slots, never()).save(any());
    }

    @Test
    void create_rejects_direct_BOOKED_status() {
        Instant start = Instant.parse("2026-09-01T09:00:00Z");
        Instant end = start.plus(30, ChronoUnit.MINUTES);
        CreateSlotRequest req = new CreateSlotRequest(start, end, SlotStatus.BOOKED);
        when(calendars.findByOwnerId(owner.getId())).thenReturn(Optional.of(calendar));
        when(slots.existsOverlap(any(), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.create(owner.getId(), req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("BOOKED");

        verify(slots, never()).save(any());
    }

    @Test
    void create_persists_when_valid() {
        Instant start = Instant.parse("2026-09-01T09:00:00Z");
        Instant end = start.plus(30, ChronoUnit.MINUTES);
        CreateSlotRequest req = new CreateSlotRequest(start, end, SlotStatus.FREE);
        when(calendars.findByOwnerId(owner.getId())).thenReturn(Optional.of(calendar));
        when(slots.existsOverlap(any(), any(), any())).thenReturn(false);
        when(slots.save(any(Slot.class))).thenAnswer(inv -> inv.getArgument(0));

        SlotResponse resp = service.create(owner.getId(), req);

        assertThat(resp.status()).isEqualTo(SlotStatus.FREE);
        assertThat(resp.startTime()).isEqualTo(start);
        assertThat(resp.endTime()).isEqualTo(end);
        verify(slots).save(any(Slot.class));
    }

    @Test
    void delete_blocks_booked_slot() {
        Slot slot = new Slot(calendar,
                Instant.parse("2026-09-01T09:00:00Z"),
                Instant.parse("2026-09-01T09:30:00Z"),
                SlotStatus.BOOKED);
        when(slots.findById(slot.getId())).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> service.delete(slot.getId()))
                .isInstanceOf(ConflictException.class);

        verify(slots, never()).delete(any());
    }

    @Test
    void updateStatus_rejects_transition_involving_BOOKED() {
        Slot slot = new Slot(calendar,
                Instant.parse("2026-09-01T09:00:00Z"),
                Instant.parse("2026-09-01T09:30:00Z"),
                SlotStatus.FREE);
        when(slots.findById(slot.getId())).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> service.updateStatus(slot.getId(), SlotStatus.BOOKED))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateStatus_allows_free_to_busy() {
        Slot slot = new Slot(calendar,
                Instant.parse("2026-09-01T09:00:00Z"),
                Instant.parse("2026-09-01T09:30:00Z"),
                SlotStatus.FREE);
        when(slots.findById(slot.getId())).thenReturn(Optional.of(slot));

        SlotResponse resp = service.updateStatus(slot.getId(), SlotStatus.BUSY);

        assertThat(resp.status()).isEqualTo(SlotStatus.BUSY);
        assertThat(slot.getStatus()).isEqualTo(SlotStatus.BUSY);
    }

    @Test
    void create_throws_notFound_when_calendar_missing() {
        UUID unknownUser = UUID.randomUUID();
        when(calendars.findByOwnerId(eq(unknownUser))).thenReturn(Optional.empty());
        CreateSlotRequest req = new CreateSlotRequest(
                Instant.parse("2026-09-01T09:00:00Z"),
                Instant.parse("2026-09-01T09:30:00Z"),
                SlotStatus.FREE);

        assertThatThrownBy(() -> service.create(unknownUser, req))
                .isInstanceOf(NotFoundException.class);
    }
}
