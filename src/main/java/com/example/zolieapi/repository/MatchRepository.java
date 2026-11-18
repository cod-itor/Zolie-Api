package com.example.zolieapi.repository;

import com.example.zolieapi.model.Match;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MatchRepository extends JpaRepository<Match, UUID> {

    @Modifying
    @Query(value = """
            update matches set status = 'blocked', ended_at = now(), ended_by = :endedBy
            where status = 'active'
              and user_a_id = least(:first, :second) and user_b_id = greatest(:first, :second)
            """, nativeQuery = true)
    void blockBetween(UUID first, UUID second, UUID endedBy);

    @Modifying
    @Query(value = """
            insert into matches (user_a_id, user_b_id) values (least(:first, :second), greatest(:first, :second))
            on conflict (user_a_id, user_b_id) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(UUID first, UUID second);

    @Query(value = """
            select m.* from matches m
            where m.user_a_id = least(:first, :second) and m.user_b_id = greatest(:first, :second)
            """, nativeQuery = true)
    Optional<Match> findPair(UUID first, UUID second);

    @Query(value = """
            select m.* from matches m
            where (m.user_a_id = :userId or m.user_b_id = :userId) and m.status = 'active'
              and (m.created_at, m.id) < (:time, :id)
            order by m.created_at desc, m.id desc limit :limit
            """, nativeQuery = true)
    List<Match> findActiveForUser(UUID userId, Instant time, UUID id, int limit);
}
