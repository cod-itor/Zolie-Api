package com.example.zolieapi.repository;

import com.example.zolieapi.model.ConversationParticipant;
import com.example.zolieapi.model.id.ParticipantId;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, ParticipantId> {

    @Query("select p from ConversationParticipant p where p.id.userId = :userId and p.id.conversationId in :conversationIds")
    List<ConversationParticipant> findForUser(UUID userId, Collection<UUID> conversationIds);
}
