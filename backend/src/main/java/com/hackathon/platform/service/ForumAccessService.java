package com.hackathon.platform.service;

import com.hackathon.platform.dto.ForumPermissionResponse;
import com.hackathon.platform.model.Event;
import com.hackathon.platform.model.User;
import com.hackathon.platform.repository.EventRegistrationRepository;
import com.hackathon.platform.repository.EventRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ForumAccessService {
    private final EventRepository eventRepo;
    private final EventRegistrationRepository eventRegRepo;

    public Event requireForumAccess(UUID eventId, User user) {
        requireAuthenticatedUser(user);
        Event event = requireEvent(eventId);

        if (isSuperAdmin(user)) {
            return event;
        }

        if (isEventOwnerAdmin(event, user)) {
            return event;
        }

        boolean reg = eventRegRepo.existsByEventIdAndUserId(eventId, user.getUserId());

        if (!reg) {
            throw new AccessDeniedException("You cannot access this event as you are not a participant");
        }

        return event;
    }

    public Event requireModeratorAccess(UUID eventId, User user) {
        requireAuthenticatedUser(user);
        Event event = requireEvent(eventId);

        if (isSuperAdmin(user)) {
            return event;
        }

        if(!isEventOwnerAdmin(event, user)) {
            throw new AccessDeniedException("This is not your event, you cannot moderate it");
        }

        return event;
    }

    public ForumPermissionResponse getPermissions(UUID eventId, User user) {
        requireAuthenticatedUser(user);
        Event event = requireEvent(eventId);
        boolean canMod = isEventOwnerAdmin(event, user) || isSuperAdmin(user);
        boolean canReg = eventRegRepo.existsByEventIdAndUserId(eventId, user.getUserId());
        boolean canPart = canMod || canReg;

        return new ForumPermissionResponse(canPart, canPart, canMod);
    }

    private Event requireEvent(UUID eventId) {
        return eventRepo.findById(eventId).orElseThrow(() -> new IllegalArgumentException("The event: " + eventId + " could not be found"));
    }

    private void requireAuthenticatedUser(User user) {
        if (user == null) {
            throw new AccessDeniedException("You are not Authenticated");
        }
    }

    private boolean isEventOwnerAdmin(Event event, User user) {
        return user.getRole() != null && "ADMIN".equals(user.getRole().getName()) && user.getUserId().equals(event.getCreatedByUserId());
    }

    private boolean isSuperAdmin(User user) {
        return user.getRole() != null && "SUPERADMIN".equals(user.getRole().getName());
    }
}