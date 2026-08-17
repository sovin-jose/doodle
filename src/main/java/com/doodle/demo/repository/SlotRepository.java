package com.doodle.demo.repository;

import com.doodle.demo.domain.Slot;
import com.doodle.demo.domain.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SlotRepository extends JpaRepository<Slot, UUID> {

    List<Slot> findByCalendarIdAndStartTimeGreaterThanEqualAndEndTimeLessThanEqual(
            UUID calendarId, Instant from, Instant to);

    List<Slot> findByCalendarIdAndStatusAndStartTimeGreaterThanEqualAndEndTimeLessThanEqual(
            UUID calendarId, SlotStatus status, Instant from, Instant to);

    @Query("""
            select count(s) > 0 from Slot s
            where s.calendar.id = :calendarId
              and s.startTime < :end
              and s.endTime > :start
            """)
    boolean existsOverlap(@Param("calendarId") UUID calendarId,
                          @Param("start") Instant start,
                          @Param("end") Instant end);

    @Query("""
            select s from Slot s
            where s.calendar.owner.id in :userIds
              and s.startTime < :to
              and s.endTime > :from
            order by s.calendar.owner.id, s.startTime
            """)
    List<Slot> findOverlappingForUsers(@Param("userIds") Collection<UUID> userIds,
                                       @Param("from") Instant from,
                                       @Param("to") Instant to);
}
