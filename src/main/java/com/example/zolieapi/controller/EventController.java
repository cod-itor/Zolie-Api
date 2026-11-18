package com.example.zolieapi.controller;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.request.EventRequest;
import com.example.zolieapi.dto.request.RsvpRequest;
import com.example.zolieapi.dto.request.UpdateEventRequest;
import com.example.zolieapi.dto.response.EventResponse;
import com.example.zolieapi.dto.response.UserSummaryResponse;
import com.example.zolieapi.security.CurrentUser;
import com.example.zolieapi.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Tag(name = "26. Events")
public class EventController {

    private final EventService eventService;
    private final CurrentUser currentUser;

    @GetMapping
    @Operation(summary = "What's on: upcoming events, soonest first (guests see public events)")
    public CursorPage<EventResponse> listUpcoming(
            @Parameter(description = "Only events still running at or after this time. Defaults to now.")
            @RequestParam(required = false) Instant from,
            @Parameter(description = "Only events starting before this time.")
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) UUID placeId,
            @Parameter(schema = @Schema(allowableValues = {"festival", "music", "food", "sports", "market", "community", "other"}))
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        return eventService.listUpcoming(currentUser.idOrGuest(), from, to, placeId, category, cursor, limit);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Host an event")
    public EventResponse create(@Valid @RequestBody EventRequest request) {
        return eventService.create(currentUser.id(), request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one event with my RSVP and the friends who are going")
    public EventResponse get(@PathVariable UUID id) {
        return eventService.get(currentUser.idOrGuest(), id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Edit my event")
    public EventResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateEventRequest request) {
        return eventService.update(currentUser.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancel my event")
    public void cancel(@PathVariable UUID id) {
        eventService.cancel(currentUser.id(), id);
    }

    @PutMapping("/{id}/rsvp")
    @Operation(summary = "Mark myself as interested or going")
    public EventResponse rsvp(@PathVariable UUID id, @Valid @RequestBody RsvpRequest request) {
        return eventService.rsvp(currentUser.id(), id, request.status());
    }

    @DeleteMapping("/{id}/rsvp")
    @Operation(summary = "Remove my RSVP")
    public EventResponse removeRsvp(@PathVariable UUID id) {
        return eventService.removeRsvp(currentUser.id(), id);
    }

    @GetMapping("/{id}/attendees")
    @Operation(summary = "List who is going or interested (when the host shows the guest list)")
    public CursorPage<UserSummaryResponse> listAttendees(
            @PathVariable UUID id,
            @Parameter(schema = @Schema(allowableValues = {"going", "interested"}))
            @RequestParam(defaultValue = "going") String status,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        return eventService.listAttendees(currentUser.id(), id, status, cursor, limit);
    }
}
