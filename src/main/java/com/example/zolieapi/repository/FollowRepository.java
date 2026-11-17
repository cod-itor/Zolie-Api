package com.example.zolieapi.repository;

import com.example.zolieapi.model.Follow;
import com.example.zolieapi.model.id.FollowId;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    @Modifying
    @Query(value = """
            insert into follows (follower_id, followed_id, status, accepted_at)
            values (:followerId, :followedId, :status, case when :status = 'accepted' then now() end)
            on conflict do nothing
            """, nativeQuery = true)
    int insert(UUID followerId, UUID followedId, String status);

    @Modifying
    @Query(value = """
            update users set following_count = following_count + 1
            where id in (select follower_id from follows where followed_id = :userId and status = 'pending')
            """, nativeQuery = true)
    void countPendingAsFollowing(UUID userId);

    @Modifying
    @Query(value = """
            update follows set status = 'accepted', accepted_at = now()
            where followed_id = :userId and status = 'pending'
            """, nativeQuery = true)
    int acceptAllPending(UUID userId);
}
