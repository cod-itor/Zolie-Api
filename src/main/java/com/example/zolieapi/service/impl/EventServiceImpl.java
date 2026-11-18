package com.example.zolieapi.service.impl;

import com.example.zolieapi.common.Cursor;
import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.common.Enums;
import com.example.zolieapi.common.Texts;
import com.example.zolieapi.dto.request.EventRequest;
import com.example.zolieapi.dto.request.UpdateEventRequest;
import com.example.zolieapi.dto.response.EventResponse;
import com.example.zolieapi.dto.response.PlaceResponse;
import com.example.zolieapi.dto.response.UserSummaryResponse;
import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.mapper.UserMapper;
import com.example.zolieapi.model.Event;
import com.example.zolieapi.model.enums.EventCategory;
import com.example.zolieapi.model.enums.EventVisibility;
import com.example.zolieapi.model.enums.MediaPurpose;
import com.example.zolieapi.model.enums.NotificationTarget;
import com.example.zolieapi.model.enums.NotificationType;
import com.example.zolieapi.model.enums.RsvpStatus;
import com.example.zolieapi.repository.EventRepository;
import com.example.zolieapi.repository.EventRepository.AttendeeRow;
import com.example.zolieapi.repository.EventRepository.RsvpRow;
import com.example.zolieapi.repository.UserRepository;
import com.example.zolieapi.security.RateLimiter;
import com.example.zolieapi.security.Viewer;
import com.example.zolieapi.service.EventService;
import com.example.zolieapi.service.MediaService;
import com.example.zolieapi.service.NotificationService;
import com.example.zolieapi.service.PlaceService;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private static final int MAX_TITLE = 120;
    private static final int MAX_DESCRIPTION = 2000;
    private static final int MAX_VENUE = 120;
    private static final int MAX_PRICE_NOTE = 80;
    private static final int FRIENDS_PREVIEW = 3;
    private static final int REMINDER_BATCH = 500;
    private static final Duration MAX_AHEAD = Duration.ofDays(730);
    private static final Duration MAX_LENGTH = Duration.ofDays(30);
    private static final Duration DEFAULT_LENGTH = Duration.ofHours(3);
    private static final Instant FAR_FUTURE = Instant.parse("9999-01-01T00:00:00Z");

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final MediaService mediaService;
    private final PlaceService placeService;
    private final NotificationService notificationService;
    private final RateLimiter rateLimiter;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public EventResponse create(UUID userId, EventRequest request) {
        rateLimiter.check("event", userId, 10, Duration.ofDays(1));
        requireUpcoming(request.startsAt());
        requireValidTimes(request.startsAt(), request.endsAt());
        placeService.requireExists(request.placeId());
        if (request.coverMediaId() != null) {
            mediaService.requireUsable(userId, request.coverMediaId(), MediaPurpose.EVENT);
        }
        Event event = eventRepository.saveAndFlush(Event.builder()
                .hostId(userId)
                .title(Texts.required(request.title(), MAX_TITLE, "title"))
                .description(Texts.clean(request.description(), MAX_DESCRIPTION, "description"))
                .category(request.category() == null
                        ? EventCategory.OTHER
                        : Enums.parse(EventCategory.class, request.category(), "category"))
                .coverMediaId(request.coverMediaId())
                .startsAt(request.startsAt())
                .endsAt(request.endsAt())
                .placeId(request.placeId())
                .venueName(Texts.clean(request.venueName(), MAX_VENUE, "venueName"))
                .free(!Boolean.FALSE.equals(request.isFree()))
                .priceNote(Texts.clean(request.priceNote(), MAX_PRICE_NOTE, "priceNote"))
                .visibility(request.visibility() == null
                        ? EventVisibility.PUBLIC
                        : Enums.parse(EventVisibility.class, request.visibility(), "visibility"))
                .showGuestList(!Boolean.FALSE.equals(request.showGuestList()))
                .build());
        return toResponses(userId, List.of(event)).get(0);
    }

    @Override
    @Transactional
    public EventResponse update(UUID userId, UUID eventId, UpdateEventRequest request) {
        Event event = requireOwn(userId, eventId);
        if (request.title() != null) {
            event.setTitle(Texts.required(request.title(), MAX_TITLE, "title"));
        }
        if (request.description() != null) {
            event.setDescription(Texts.clean(request.description(), MAX_DESCRIPTION, "description"));
        }
        if (request.category() != null) {
            event.setCategory(Enums.parse(EventCategory.class, request.category(), "category"));
        }
        if (request.coverMediaId() != null) {
            mediaService.requireUsable(userId, request.coverMediaId(), MediaPurpose.EVENT);
            event.setCoverMediaId(request.coverMediaId());
        }
        if (request.startsAt() != null && !request.startsAt().equals(event.getStartsAt())) {
            requireUpcoming(request.startsAt());
            event.setStartsAt(request.startsAt());
            eventRepository.resetReminders(eventId);
        }
        if (request.endsAt() != null) {
            event.setEndsAt(request.endsAt());
        }
        requireValidTimes(event.getStartsAt(), event.getEndsAt());
        if (request.placeId() != null) {
            placeService.requireExists(request.placeId());
            event.setPlaceId(request.placeId());
        }
        if (request.venueName() != null) {
            event.setVenueName(Texts.clean(request.venueName(), MAX_VENUE, "venueName"));
        }
        if (request.isFree() != null) {
            event.setFree(request.isFree());
        }
        if (request.priceNote() != null) {
            event.setPriceNote(Texts.clean(request.priceNote(), MAX_PRICE_NOTE, "priceNote"));
        }
        if (request.visibility() != null) {
            event.setVisibility(Enums.parse(EventVisibility.class, request.visibility(), "visibility"));
        }
        if (request.showGuestList() != null) {
            event.setShowGuestList(request.showGuestList());
        }
        return toResponses(userId, List.of(eventRepository.saveAndFlush(event))).get(0);
    }

    @Override
    @Transactional
    public void cancel(UUID userId, UUID eventId) {
        requireOwn(userId, eventId).setCancelledAt(Instant.now());
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse get(UUID viewerId, UUID eventId) {
        return toResponses(viewerId, List.of(requireVisible(viewerId, eventId))).get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<EventResponse> listUpcoming(UUID viewerId, Instant from, Instant to, UUID placeId,
                                                  String category, String cursor, Integer limit) {
        int size = CursorPage.limit(limit);
        Cursor after = Cursor.decodeAscending(cursor);
        String categoryValue = category == null || category.isBlank()
                ? ""
                : Enums.value(Enums.parse(EventCategory.class, category, "category"));
        List<Event> events = eventRepository.findUpcoming(viewerId,
                from == null ? Instant.now() : from,
                to == null ? FAR_FUTURE : to,
                placeId == null,
                placeId == null ? Viewer.GUEST : placeId,
                categoryValue, after.time(), after.id(), size + 1);
        return CursorPage.of(events, size, event -> new Cursor(event.getStartsAt(), event.getId()),
                page -> toResponses(viewerId, page));
    }

    @Override
    @Transactional
    public EventResponse rsvp(UUID userId, UUID eventId, String status) {
        if (hasEnded(requireVisible(userId, eventId))) {
            throw ApiException.rule("This event has ended.");
        }
        RsvpStatus parsed = Enums.require(RsvpStatus.class, status, "status");
        eventRepository.upsertRsvp(eventId, userId, Enums.value(parsed));
        eventRepository.refreshCounts(eventId);
        return get(userId, eventId);
    }

    @Override
    @Transactional
    public EventResponse removeRsvp(UUID userId, UUID eventId) {
        requireVisible(userId, eventId);
        eventRepository.deleteRsvp(eventId, userId);
        eventRepository.refreshCounts(eventId);
        return get(userId, eventId);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<UserSummaryResponse> listAttendees(UUID viewerId, UUID eventId, String status, String cursor,
                                                         Integer limit) {
        Event event = requireVisible(viewerId, eventId);
        if (!event.isShowGuestList() && !event.getHostId().equals(viewerId)) {
            throw ApiException.forbidden();
        }
        RsvpStatus parsed = status == null ? RsvpStatus.GOING : Enums.parse(RsvpStatus.class, status, "status");
        int size = CursorPage.limit(limit);
        return CursorPage.of(userRepository.findEventAttendees(eventId, Enums.value(parsed), viewerId,
                        Cursor.decode(cursor).id(), size + 1), size,
                user -> Cursor.ofId(user.getId()), userMapper::toSummaries);
    }

    @Override
    @Transactional
    public void sendReminders() {
        for (AttendeeRow row : eventRepository.findHourRemindersDue(REMINDER_BATCH)) {
            eventRepository.markHourReminded(row.getEventId(), row.getUserId());
            remind(row);
        }
        for (AttendeeRow row : eventRepository.findDayRemindersDue(REMINDER_BATCH)) {
            eventRepository.markDayReminded(row.getEventId(), row.getUserId());
            remind(row);
        }
    }

    private void remind(AttendeeRow row) {
        notificationService.send(row.getUserId(), null, NotificationType.EVENT_REMINDER, NotificationTarget.EVENT,
                row.getEventId());
    }

    private void requireUpcoming(Instant startsAt) {
        Instant now = Instant.now();
        if (startsAt.isBefore(now) || startsAt.isAfter(now.plus(MAX_AHEAD))) {
            throw ApiException.rule("The event must start in the future, within the next two years.");
        }
    }

    private boolean hasEnded(Event event) {
        Instant end = event.getEndsAt() != null ? event.getEndsAt() : event.getStartsAt().plus(DEFAULT_LENGTH);
        return end.isBefore(Instant.now());
    }

    private void requireValidTimes(Instant startsAt, Instant endsAt) {
        if (endsAt != null && (!endsAt.isAfter(startsAt) || endsAt.isAfter(startsAt.plus(MAX_LENGTH)))) {
            throw ApiException.rule("The event must end after it starts, within 30 days.");
        }
    }

    private Event requireVisible(UUID viewerId, UUID eventId) {
        return eventRepository.findVisible(eventId, viewerId).orElseThrow(ApiException::notFound);
    }

    private Event requireOwn(UUID userId, UUID eventId) {
        Event event = requireVisible(userId, eventId);
        if (!event.getHostId().equals(userId)) {
            throw ApiException.forbidden();
        }
        return event;
    }

    private List<EventResponse> toResponses(UUID viewerId, List<Event> events) {
        if (events.isEmpty()) {
            return List.of();
        }
        List<UUID> eventIds = events.stream().map(Event::getId).toList();
        Map<UUID, String> covers = mediaService.urls(events.stream().map(Event::getCoverMediaId).toList());
        Map<UUID, PlaceResponse> places = placeService.byIds(events.stream().map(Event::getPlaceId).toList());
        Map<UUID, String> myRsvps = eventRepository.findMyRsvps(viewerId, eventIds).stream()
                .collect(Collectors.toMap(RsvpRow::getEventId, RsvpRow::getStatus));
        Map<UUID, List<UUID>> friendsByEvent = eventRepository.findFriendsGoing(viewerId, eventIds).stream()
                .collect(Collectors.groupingBy(AttendeeRow::getEventId,
                        Collectors.mapping(AttendeeRow::getUserId, Collectors.toList())));
        List<UUID> userIds = new ArrayList<>(events.stream().map(Event::getHostId).toList());
        friendsByEvent.values().forEach(friends -> userIds.addAll(friends.stream().limit(FRIENDS_PREVIEW).toList()));
        Map<UUID, UserSummaryResponse> users = userMapper.summariesById(userIds);

        return events.stream()
                .map(event -> {
                    List<UUID> friends = friendsByEvent.getOrDefault(event.getId(), List.of());
                    return new EventResponse(
                            event.getId(),
                            event.getTitle(),
                            event.getDescription(),
                            Enums.value(event.getCategory()),
                            covers.get(event.getCoverMediaId()),
                            event.getStartsAt(),
                            event.getEndsAt(),
                            places.get(event.getPlaceId()),
                            event.getVenueName(),
                            event.isFree(),
                            event.getPriceNote(),
                            Enums.value(event.getVisibility()),
                            event.isShowGuestList(),
                            users.get(event.getHostId()),
                            event.getInterestedCount(),
                            event.getGoingCount(),
                            myRsvps.get(event.getId()),
                            friends.stream().limit(FRIENDS_PREVIEW).map(users::get).toList(),
                            friends.size(),
                            event.getHostId().equals(viewerId),
                            event.getCreatedAt());
                })
                .toList();
    }
}
