package com.example.zolieapi.repository;

import com.example.zolieapi.model.Block;
import com.example.zolieapi.model.id.BlockId;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BlockRepository extends JpaRepository<Block, BlockId> {

    @Query(value = """
            select exists (select 1 from blocks
                where (blocker_id = :first and blocked_id = :second)
                   or (blocker_id = :second and blocked_id = :first))
            """, nativeQuery = true)
    boolean existsBetween(UUID first, UUID second);
}
