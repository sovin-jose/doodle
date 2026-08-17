package com.doodle.demo.service;

import com.doodle.demo.domain.Calendar;
import com.doodle.demo.domain.Meeting;
import com.doodle.demo.domain.Slot;
import com.doodle.demo.domain.SlotStatus;
import com.doodle.demo.domain.User;
import com.doodle.demo.repository.MeetingRepository;
import com.doodle.demo.repository.SlotRepository;
import com.doodle.demo.repository.UserRepository;
import com.doodle.demo.web.dto.BookMeetingRequest;
import com.doodle.demo.web.dto.MeetingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

    @Mock
    MeetingRepository meetings;

    @Mock
    SlotRepository slots;

    @Mock
    UserRepository users;

    @InjectMocks
    MeetingService service;

    User organizer;
    User participant;
    Calendar calendar;
    Slot freeSlot;

    @BeforeEach
    void setUp() {
        organizer = new User("Ada", "ada@example.com");
        participant = new User("Alan", "alan@example.com");
        calendar = new Calendar(organizer, null);
        freeSlot = new Slot(calendar,
                Instant.parse("2026-09-01T09:00:00Z"),
                Instant.parse("2026-09-01T09:30:00Z"),
                SlotStatus.FREE);
    }

    @Test
    void book_transitions_slot_to_BOOKED_and_persists_participants() {
        BookMeetingRequest req = new BookMeetingRequest(
                freeSlot.getId(),
                organizer.getId(),
                "Sync",
                "quick chat",
                List.of(participant.getId()));

        when(slots.findById(freeSlot.getId())).thenReturn(Optional.of(freeSlot));
        when(users.findById(organizer.getId())).thenReturn(Optional.of(organizer));
        when(users.findById(participant.getId())).thenReturn(Optional.of(participant));
        when(meetings.save(any(Meeting.class))).thenAnswer(inv -> inv.getArgument(0));

        MeetingResponse resp = service.book(req);

        assertThat(freeSlot.getStatus()).isEqualTo(SlotStatus.BOOKED);
        assertThat(resp.title()).isEqualTo("Sync");
        assertThat(resp.participants()).hasSize(1);
        assertThat(resp.participants().get(0).userId()).isEqualTo(participant.getId());
    }

    @Test
    void book_rejects_non_FREE_slot() {
        freeSlot.setStatus(SlotStatus.BUSY);
        BookMeetingRequest req = new BookMeetingRequest(
                freeSlot.getId(), organizer.getId(), "Sync", null, List.of());
        when(slots.findById(freeSlot.getId())).thenReturn(Optional.of(freeSlot));

        assertThatThrownBy(() -> service.book(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not FREE");

        verify(meetings, never()).save(any());
    }

    @Test
    void cancel_restores_slot_to_FREE() {
        Meeting meeting = new Meeting(freeSlot, organizer, "Sync", null);
        freeSlot.setStatus(SlotStatus.BOOKED);
        when(meetings.findById(meeting.getId())).thenReturn(Optional.of(meeting));

        service.cancel(meeting.getId());

        assertThat(freeSlot.getStatus()).isEqualTo(SlotStatus.FREE);
        verify(meetings).delete(meeting);
    }

    @Test
    void cancel_throws_when_meeting_missing() {
        UUID unknown = UUID.randomUUID();
        when(meetings.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(unknown))
                .isInstanceOf(NotFoundException.class);
    }
}
