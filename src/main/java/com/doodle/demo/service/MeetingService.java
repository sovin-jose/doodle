package com.doodle.demo.service;

import com.doodle.demo.domain.Meeting;
import com.doodle.demo.domain.Slot;
import com.doodle.demo.domain.SlotStatus;
import com.doodle.demo.domain.User;
import com.doodle.demo.repository.MeetingRepository;
import com.doodle.demo.repository.SlotRepository;
import com.doodle.demo.repository.UserRepository;
import com.doodle.demo.web.dto.BookMeetingRequest;
import com.doodle.demo.web.dto.MeetingResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MeetingService {

    private final MeetingRepository meetings;
    private final SlotRepository slots;
    private final UserRepository users;

    public MeetingService(MeetingRepository meetings, SlotRepository slots, UserRepository users) {
        this.meetings = meetings;
        this.slots = slots;
        this.users = users;
    }

    @Transactional
    public MeetingResponse book(BookMeetingRequest request) {
        Slot slot = slots.findById(request.slotId())
                .orElseThrow(() -> new NotFoundException("slot not found: " + request.slotId()));
        if (slot.getStatus() != SlotStatus.FREE) {
            throw new ConflictException("slot is not FREE, current status: " + slot.getStatus());
        }
        User organizer = users.findById(request.organizerId())
                .orElseThrow(() -> new NotFoundException("organizer not found: " + request.organizerId()));

        Meeting meeting = new Meeting(slot, organizer, request.title(), request.description());
        if (request.participantIds() != null) {
            for (UUID participantId : request.participantIds()) {
                User participant = users.findById(participantId)
                        .orElseThrow(() -> new NotFoundException("participant not found: " + participantId));
                meeting.addParticipant(participant);
            }
        }
        slot.setStatus(SlotStatus.BOOKED);
        Meeting saved = meetings.save(meeting);
        return MeetingResponse.of(saved);
    }

    @Transactional(readOnly = true)
    public MeetingResponse get(UUID meetingId) {
        Meeting meeting = meetings.findById(meetingId)
                .orElseThrow(() -> new NotFoundException("meeting not found: " + meetingId));
        return MeetingResponse.of(meeting);
    }

    @Transactional(readOnly = true)
    public List<MeetingResponse> listOrganizedBy(UUID userId) {
        return meetings.findByOrganizerId(userId).stream()
                .map(MeetingResponse::of)
                .toList();
    }
}
