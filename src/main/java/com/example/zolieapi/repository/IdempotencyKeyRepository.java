package com.example.zolieapi.repository;

import com.example.zolieapi.model.IdempotencyKey;
import com.example.zolieapi.model.id.IdempotencyKeyId;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, IdempotencyKeyId> {

    @Modifying
    @Query("delete from IdempotencyKey k where k.createdAt < :before")
    int deleteOlderThan(Instant before);
}
