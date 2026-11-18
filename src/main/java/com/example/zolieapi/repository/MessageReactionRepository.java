package com.example.zolieapi.repository;

import com.example.zolieapi.model.Message;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface MessageReactionRepository extends Repository<Message, UUID> {

    @Modifying
    @Query(value = """
            insert into message_reactions (message_id, user_id, emoji) values (:messageId, :userId, :emoji)
            on conflict (message_id, user_id) do update set emoji = excluded.emoji, created_at = now()
            """, nativeQuery = true)
    void upsert(UUID messageId, UUID userId, String emoji);

    @Modifying
    @Query(value = "delete from message_reactions where message_id = :messageId and user_id = :userId", nativeQuery = true)
    void delete(UUID messageId, UUID userId);

    @Query(value = """
            select message_id as messageId, user_id as userId, emoji as emoji from message_reactions
            where message_id in (:messageIds) order by created_at
            """, nativeQuery = true)
    List<ReactionRow> findForMessages(Collection<UUID> messageIds);

    interface ReactionRow {

        UUID getMessageId();

        UUID getUserId();

        String getEmoji();
    }
}
