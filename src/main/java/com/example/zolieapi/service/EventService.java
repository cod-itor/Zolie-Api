package com.example.zolieapi.service;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.request.EventRequest;
import com.example.zolieapi.dto.request.UpdateEventRequest;
import com.example.zolieapi.dto.response.EventResponse;
import com.example.zolieapi.dto.response.UserSummaryResponse;
import java.time.Instant;
import java.util.UUID;

public interface EventService {

    EventResponse create(UUID userId, EventRequest request);

    EventResponse update(UUID userId, UUID eventId, UpdateEventRequest request);

    void cancel(UUID userId, UUID eventId);

    EventResponse get(UUID viewerId, UUID eventId);

    CursorPage<EventResponse> listUpcoming(UUID viewerId, Instant from, Instant to, UUID placeId, String category,
                                           String cursor, Integer limit);

    EventResponse rsvp(UUID userId, UUID eventId, String status);

    EventResponse removeRsvp(UUID userId, UUID eventId);

    CursorPage<UserSummaryResponse> listAttendees(UUID viewerId, UUID eventId, String status, String cursor,
                                                  Integer limit);

    void sendReminders();
}
