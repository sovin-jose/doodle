package com.doodle.demo.service;

import com.doodle.demo.domain.Calendar;
import com.doodle.demo.domain.Slot;
import com.doodle.demo.domain.SlotStatus;
import com.doodle.demo.domain.User;
import com.doodle.demo.repository.SlotRepository;
import com.doodle.demo.web.dto.AvailabilityResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    SlotRepository slots;

    @InjectMocks
    AvailabilityService service;

    User ada;
    User alan;
    Calendar adaCal;
    Calendar alanCal;

    Instant from = Instant.parse("2026-09-01T09:00:00Z");
    Instant to = Instant.parse("2026-09-01T12:00:00Z");

    @BeforeEach
    void setUp() {
        ada = new User("Ada", "ada@example.com");
        alan = new User("Alan", "alan@example.com");
        adaCal = new Calendar(ada, null);
        alanCal = new Calendar(alan, null);
    }

    @Test
    void aggregate_computes_common_free_windows() {
        // Ada is free 09:00–10:30, busy 10:30–11:00, free 11:00–12:00
        Slot adaFree1 = new Slot(adaCal, ts("09:00"), ts("10:30"), SlotStatus.FREE);
        Slot adaBusy = new Slot(adaCal, ts("10:30"), ts("11:00"), SlotStatus.BUSY);
        Slot adaFree2 = new Slot(adaCal, ts("11:00"), ts("12:00"), SlotStatus.FREE);
        // Alan is free 09:30–11:30
        Slot alanFree = new Slot(alanCal, ts("09:30"), ts("11:30"), SlotStatus.FREE);

        when(slots.findOverlappingForUsers(anyCollection(), any(), any()))
                .thenReturn(List.of(adaFree1, adaBusy, adaFree2, alanFree));

        AvailabilityResponse resp = service.aggregate(
                List.of(ada.getId(), alan.getId()), from, to);

        // Common free should be intersection of Ada's free (09:00-10:30, 11:00-12:00)
        // with Alan's free (09:30-11:30) = 09:30-10:30 and 11:00-11:30.
        assertThat(resp.commonFree()).hasSize(2);
        assertThat(resp.commonFree().get(0).start()).isEqualTo(ts("09:30"));
        assertThat(resp.commonFree().get(0).end()).isEqualTo(ts("10:30"));
        assertThat(resp.commonFree().get(1).start()).isEqualTo(ts("11:00"));
        assertThat(resp.commonFree().get(1).end()).isEqualTo(ts("11:30"));
    }

    @Test
    void aggregate_clips_slots_to_window() {
        // Ada's slot extends beyond the window; should be clipped.
        Slot bigSlot = new Slot(adaCal, ts("08:00"), ts("13:00"), SlotStatus.FREE);
        when(slots.findOverlappingForUsers(anyCollection(), any(), any()))
                .thenReturn(List.of(bigSlot));

        AvailabilityResponse resp = service.aggregate(List.of(ada.getId()), from, to);

        assertThat(resp.users().get(0).free()).hasSize(1);
        assertThat(resp.users().get(0).free().get(0).start()).isEqualTo(from);
        assertThat(resp.users().get(0).free().get(0).end()).isEqualTo(to);
    }

    @Test
    void aggregate_returns_empty_common_when_no_overlap() {
        // Two disjoint free intervals.
        Slot adaFree = new Slot(adaCal, ts("09:00"), ts("10:00"), SlotStatus.FREE);
        Slot alanFree = new Slot(alanCal, ts("11:00"), ts("12:00"), SlotStatus.FREE);
        when(slots.findOverlappingForUsers(anyCollection(), any(), any()))
                .thenReturn(List.of(adaFree, alanFree));

        AvailabilityResponse resp = service.aggregate(
                List.of(ada.getId(), alan.getId()), from, to);

        assertThat(resp.commonFree()).isEmpty();
    }

    @Test
    void aggregate_rejects_empty_userIds() {
        assertThatThrownBy(() -> service.aggregate(List.of(), from, to))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aggregate_rejects_inverted_window() {
        assertThatThrownBy(() -> service.aggregate(List.of(UUID.randomUUID()), to, from))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Instant ts(String hhmm) {
        return Instant.parse("2026-09-01T" + hhmm + ":00Z");
    }
}
