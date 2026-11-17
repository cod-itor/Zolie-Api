package com.example.zolieapi.repository;

import com.example.zolieapi.model.Conversation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @Query(value = """
            select c.* from conversations c
            where c.type = 'regular' and c.user_a_id = :userA and c.user_b_id = :userB
            """, nativeQuery = true)
    Optional<Conversation> findRegular(UUID userA, UUID userB);

    Optional<Conversation> findByMatchId(UUID matchId);

    @Query(value = """
            select c.* from conversations c
            join conversation_participants cp on cp.conversation_id = c.id and cp.user_id = :userId
            left join matches m on m.id = c.match_id
            where (c.match_id is null or m.status = 'active')
              and (c.type = 'dating' or c.last_message_at is not null)
              and (cp.cleared_at is null or c.last_message_at > cp.cleared_at)
              and not exists (select 1 from blocks b
                  where (b.blocker_id = c.user_a_id and b.blocked_id = c.user_b_id)
                     or (b.blocker_id = c.user_b_id and b.blocked_id = c.user_a_id))
              and ((:folder = 'inbox' and c.type = 'regular' and cp.state = 'inbox')
                or (:folder = 'requests' and cp.state = 'request')
                or (:folder = 'dating' and c.type = 'dating'))
              and (coalesce(c.last_message_at, c.created_at), c.id) < (:time, :id)
            order by coalesce(c.last_message_at, c.created_at) desc, c.id desc
            limit :limit
            """, nativeQuery = true)
    List<Conversation> findForUser(UUID userId, String folder, Instant time, UUID id, int limit);

    @Modifying
    @Query("update Conversation c set c.lastMessageAt = :time where c.id = :id")
    void touch(UUID id, Instant time);
}
