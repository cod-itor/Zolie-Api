package com.example.zolieapi.repository;

import com.example.zolieapi.model.Message;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    Optional<Message> findBySenderIdAndClientMessageId(UUID senderId, UUID clientMessageId);

    @Query(value = """
            select m.* from messages m
            where m.conversation_id = :conversationId and m.deleted_at is null and m.created_at > :visibleAfter
              and (m.created_at, m.id) < (:time, :id)
            order by m.created_at desc, m.id desc limit :limit
            """, nativeQuery = true)
    List<Message> findPage(UUID conversationId, Instant visibleAfter, Instant time, UUID id, int limit);

    @Query(value = """
            select m.* from messages m
            where m.conversation_id = :conversationId and m.deleted_at is null and m.created_at > :visibleAfter
              and m.media_id is not null and (m.created_at, m.id) < (:time, :id)
            order by m.created_at desc, m.id desc limit :limit
            """, nativeQuery = true)
    List<Message> findMediaPage(UUID conversationId, Instant visibleAfter, Instant time, UUID id, int limit);

    @Query(value = """
            select distinct on (m.conversation_id) m.* from messages m
            where m.conversation_id in (:conversationIds) and m.deleted_at is null
            order by m.conversation_id, m.created_at desc, m.id desc
            """, nativeQuery = true)
    List<Message> findLatest(Collection<UUID> conversationIds);

    @Query(value = """
            select m.conversation_id as conversationId, count(*) as total
            from messages m
            join conversation_participants cp on cp.conversation_id = m.conversation_id and cp.user_id = :userId
            where m.conversation_id in (:conversationIds) and m.deleted_at is null and m.sender_id <> :userId
              and m.created_at > coalesce(cp.last_read_at, 'epoch'::timestamptz)
              and m.created_at > coalesce(cp.cleared_at, 'epoch'::timestamptz)
            group by m.conversation_id
            """, nativeQuery = true)
    List<UnreadRow> countUnread(UUID userId, Collection<UUID> conversationIds);

    interface UnreadRow {

        UUID getConversationId();

        long getTotal();
    }
}
