package com.hackathon.platform.controller;

import com.hackathon.platform.dto.EventRequest;
import com.hackathon.platform.dto.HackathonRequest;
import com.hackathon.platform.model.Event;
import com.hackathon.platform.model.Hackathon;
import com.hackathon.platform.service.HackathonService;
import com.hackathon.platform.service.EventService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hackahon")
public class HackathonController {

    private final HackathonService hackathonService;
    private final EventService eventService;

    public HackathonController(HackathonService hackathonService, EventService eventService) {
        this.hackathonService = hackathonService;
        this.eventService = eventService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Hackathon> createHackathon(@RequestBody HackathonRequest req){
        return ResponseEntity.ok(hackathonService.createHackathon(req));
    }

    @GetMapping
    public ResponseEntity<List<Hackathon>> getAllHackathons() {
        return ResponseEntity.ok(hackathonService.getAllHackathons());
    }

    @GetMapping("/{hackathonId")
    public ResponseEntity<Hackathon> getHackathon(@PathVariable("hackathonId") UUID hackathonId) {
        return ResponseEntity.ok(hackathonService.getHackathonById(hackathonId));
    }

    @PutMapping("/{hackathonId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Hackathon> updateHackathon(@PathVariable("hackathonId") UUID hackathonId, @RequestBody HackathonRequest req) {
        return ResponseEntity.ok(hackathonService.updateHackathonById(hackathonId, req));
    }

    @DeleteMapping("/{hackathonId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletHackathon(@PathVariable("hackathonId") UUID hackathonId) {
        hackathonService.deleteHackathonById(hackathonId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{hackathonId}/events")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Event> createEvent(@PathVariable("hackathonId") UUID hackathonId, @RequestBody EventRequest req) {
        req.setHackathonId(hackathonId);
        return ResponseEntity.ok(eventService.createEvent(req));
    }

    @GetMapping("/{hackathonId}/events")
    public ResponseEntity<List<Event>> getEventsForHackathon(@PathVariable("hackathonId") UUID hackathonId) {
        return ResponseEntity.ok(eventService.getEventsbyHackathonId(hackathonId));
    }
}