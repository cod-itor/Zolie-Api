package com.example.zolieapi.repository;

import com.example.zolieapi.model.Event;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface EventRepository extends JpaRepository<Event, UUID> {

    String VISIBLE = " from events e join users a on a.id = e.host_id where e.cancelled_at is null and "
            + VisibilitySql.AUTHOR_ACTIVE + " and " + VisibilitySql.NOT_BLOCKED
            + " and (e.visibility = 'public' or a.id = :viewer or " + VisibilitySql.FOLLOWS_AUTHOR + ") ";

    @Query(value = "select e.*" + VISIBLE + " and e.id = :eventId", nativeQuery = true)
    Optional<Event> findVisible(UUID eventId, UUID viewer);

    @Query(value = "select e.*" + VISIBLE + """
             and coalesce(e.ends_at, e.starts_at + interval '3 hours') >= :fromTime
             and e.starts_at < :toTime
             and (:anyPlace or e.place_id = :placeId)
             and (:category = '' or e.category = :category)
             and (e.starts_at, e.id) > (:time, :id)
            order by e.starts_at, e.id limit :limit
            """, nativeQuery = true)
    List<Event> findUpcoming(UUID viewer, Instant fromTime, Instant toTime, boolean anyPlace, UUID placeId,
                             String category, Instant time, UUID id, int limit);

    @Modifying
    @Query(value = """
            insert into event_rsvps (event_id, user_id, status) values (:eventId, :userId, :status)
            on conflict (event_id, user_id) do update set status = excluded.status
            """, nativeQuery = true)
    void upsertRsvp(UUID eventId, UUID userId, String status);

    @Modifying
    @Query(value = "delete from event_rsvps where event_id = :eventId and user_id = :userId", nativeQuery = true)
    void deleteRsvp(UUID eventId, UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            update events set
                going_count = (select count(*) from event_rsvps where event_id = :eventId and status = 'going'),
                interested_count = (select count(*) from event_rsvps where event_id = :eventId and status = 'interested')
            where id = :eventId
            """, nativeQuery = true)
    void refreshCounts(UUID eventId);

    @Query(value = """
            select event_id as eventId, status as status from event_rsvps
            where user_id = :viewer and event_id in (:eventIds)
            """, nativeQuery = true)
    List<RsvpRow> findMyRsvps(UUID viewer, Collection<UUID> eventIds);

    @Query(value = """
            select r.event_id as eventId, r.user_id as userId
            from event_rsvps r join events e on e.id = r.event_id join users a on a.id = r.user_id
            where r.event_id in (:eventIds) and r.status = 'going'
              and (e.show_guest_list or e.host_id = :viewer) and
            """ + VisibilitySql.AUTHOR_ACTIVE + " and " + VisibilitySql.NOT_BLOCKED + " and " + VisibilitySql.FOLLOWS_AUTHOR
            + " order by r.event_id, r.created_at", nativeQuery = true)
    List<AttendeeRow> findFriendsGoing(UUID viewer, Collection<UUID> eventIds);

    @Query(value = """
            select r.event_id as eventId, r.user_id as userId from event_rsvps r join events e on e.id = r.event_id
            where e.cancelled_at is null and not r.day_reminded
              and e.starts_at > now() + interval '1 hour' and e.starts_at <= now() + interval '24 hours'
            limit :limit
            """, nativeQuery = true)
    List<AttendeeRow> findDayRemindersDue(int limit);

    @Query(value = """
            select r.event_id as eventId, r.user_id as userId from event_rsvps r join events e on e.id = r.event_id
            where e.cancelled_at is null and not r.hour_reminded
              and e.starts_at > now() and e.starts_at <= now() + interval '1 hour'
            limit :limit
            """, nativeQuery = true)
    List<AttendeeRow> findHourRemindersDue(int limit);

    @Modifying
    @Query(value = "update event_rsvps set day_reminded = false, hour_reminded = false where event_id = :eventId",
            nativeQuery = true)
    void resetReminders(UUID eventId);

    @Modifying
    @Query(value = "update event_rsvps set day_reminded = true where event_id = :eventId and user_id = :userId",
            nativeQuery = true)
    void markDayReminded(UUID eventId, UUID userId);

    @Modifying
    @Query(value = """
            update event_rsvps set hour_reminded = true, day_reminded = true
            where event_id = :eventId and user_id = :userId
            """, nativeQuery = true)
    void markHourReminded(UUID eventId, UUID userId);

    interface RsvpRow {

        UUID getEventId();

        String getStatus();
    }

    interface AttendeeRow {

        UUID getEventId();

        UUID getUserId();
    }
}
