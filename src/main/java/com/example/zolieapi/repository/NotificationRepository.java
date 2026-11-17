package com.example.zolieapi.repository;

import com.example.zolieapi.model.Notification;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query(value = """
            select n.* from notifications n
            where n.recipient_id = :userId and (n.created_at, n.id) < (:time, :id)
              and not exists (select 1 from blocks b
                  where (b.blocker_id = :userId and b.blocked_id = n.actor_id)
                     or (b.blocker_id = n.actor_id and b.blocked_id = :userId))
            order by n.created_at desc, n.id desc limit :limit
            """, nativeQuery = true)
    List<Notification> findPage(UUID userId, Instant time, UUID id, int limit);

    long countByRecipientIdAndReadAtIsNull(UUID recipientId);

    @Modifying
    @Query("update Notification n set n.readAt = current_timestamp "
            + "where n.recipientId = :userId and n.readAt is null and n.createdAt <= :upTo")
    void markReadUpTo(UUID userId, Instant upTo);
}
