package com.doodle.demo.repository;

import com.doodle.demo.domain.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MeetingRepository extends JpaRepository<Meeting, UUID> {
    Optional<Meeting> findBySlotId(UUID slotId);

    List<Meeting> findByOrganizerId(UUID organizerId);
}
