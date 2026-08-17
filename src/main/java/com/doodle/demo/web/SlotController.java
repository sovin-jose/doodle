package com.doodle.demo.web;

import com.doodle.demo.domain.SlotStatus;
import com.doodle.demo.service.SlotService;
import com.doodle.demo.web.dto.CreateSlotRequest;
import com.doodle.demo.web.dto.SlotResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class SlotController {

    private final SlotService slotService;

    public SlotController(SlotService slotService) {
        this.slotService = slotService;
    }

    @PostMapping("/users/{userId}/slots")
    public ResponseEntity<SlotResponse> create(
            @PathVariable UUID userId,
            @Valid @RequestBody CreateSlotRequest request) {
        SlotResponse created = slotService.create(userId, request);
        return ResponseEntity.created(URI.create("/api/slots/" + created.id())).body(created);
    }

    @GetMapping("/users/{userId}/slots")
    public List<SlotResponse> list(
            @PathVariable UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) SlotStatus status) {
        return slotService.listForUser(userId, from, to, status);
    }

    @DeleteMapping("/slots/{slotId}")
    public ResponseEntity<Void> delete(@PathVariable UUID slotId) {
        slotService.delete(slotId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/slots/{slotId}/status")
    public SlotResponse updateStatus(@PathVariable UUID slotId, @RequestParam SlotStatus status) {
        return slotService.updateStatus(slotId, status);
    }
}
