package com.doodle.demo.web;

import com.doodle.demo.service.MeetingService;
import com.doodle.demo.web.dto.BookMeetingRequest;
import com.doodle.demo.web.dto.MeetingResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @PostMapping
    public ResponseEntity<MeetingResponse> book(@Valid @RequestBody BookMeetingRequest request) {
        MeetingResponse booked = meetingService.book(request);
        return ResponseEntity.created(URI.create("/api/meetings/" + booked.id())).body(booked);
    }

    @GetMapping("/{id}")
    public MeetingResponse get(@PathVariable UUID id) {
        return meetingService.get(id);
    }

    @GetMapping
    public List<MeetingResponse> listOrganizedBy(@RequestParam UUID organizerId) {
        return meetingService.listOrganizedBy(organizerId);
    }
}
