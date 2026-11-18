package com.example.zolieapi.repository;

import com.example.zolieapi.model.PollOption;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PollOptionRepository extends JpaRepository<PollOption, UUID> {

    List<PollOption> findByPostIdInOrderByPostIdAscPositionAsc(Collection<UUID> postIds);

    @Modifying
    @Query(value = """
            insert into poll_votes (post_id, user_id, option_id) values (:postId, :userId, :optionId)
            on conflict do nothing
            """, nativeQuery = true)
    int insertVote(UUID postId, UUID userId, UUID optionId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update PollOption o set o.voteCount = o.voteCount + 1 where o.id = :optionId")
    void addVote(UUID optionId);

    @Query(value = """
            select post_id as postId, option_id as optionId from poll_votes
            where user_id = :userId and post_id in (:postIds)
            """, nativeQuery = true)
    List<VoteRow> findVotes(UUID userId, Collection<UUID> postIds);

    @Query(value = "select exists (select 1 from poll_votes where post_id = :postId and user_id = :userId)",
            nativeQuery = true)
    boolean hasVoted(UUID postId, UUID userId);

    @Query(value = "select 1 from (select pg_advisory_xact_lock(hashtextextended(:key, 0))) locked", nativeQuery = true)
    Integer lock(String key);

    interface VoteRow {

        UUID getPostId();

        UUID getOptionId();
    }
}
