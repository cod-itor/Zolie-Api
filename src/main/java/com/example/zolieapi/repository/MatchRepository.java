package com.example.zolieapi.repository;

import com.example.zolieapi.model.Match;
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
}
