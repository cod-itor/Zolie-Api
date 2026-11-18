package com.example.zolieapi.repository;

import com.example.zolieapi.model.DatingPromptAnswer;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface DatingPromptAnswerRepository extends JpaRepository<DatingPromptAnswer, UUID> {

    List<DatingPromptAnswer> findByUserIdInOrderByUserIdAscPositionAsc(Collection<UUID> userIds);

    @Modifying(flushAutomatically = true)
    @Query("delete from DatingPromptAnswer a where a.userId = :userId")
    void deleteForUser(UUID userId);
}
