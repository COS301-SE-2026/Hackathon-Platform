package com.hackathon.platform.controller;

import com.hackathon.platform.model.Event;
import com.hackathon.platform.service.EventService;
import com.hackathon.platform.dto.EventRequest;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/admin/events")
public class AdminEventController {
    private final EventService eventService;

    public AdminEventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Event> createEvent(@RequestBody EventRequest req) {
        Event newEvent = eventService.createEvent(req);
        return ResponseEntity.ok(newEvent);
    }
}