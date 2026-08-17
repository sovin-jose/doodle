package com.doodle.demo.repository;

import com.doodle.demo.domain.MeetingParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, UUID> {
    List<MeetingParticipant> findByMeetingId(UUID meetingId);

    List<MeetingParticipant> findByUserId(UUID userId);
}
