package com.example.zolieapi.model;

import com.example.zolieapi.model.enums.ParticipantState;
import com.example.zolieapi.model.id.ParticipantId;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "conversation_participants")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationParticipant {

    @EmbeddedId
    private ParticipantId id;

    @Builder.Default
    private ParticipantState state = ParticipantState.INBOX;

    private Instant lastReadAt;

    private Instant clearedAt;

    private boolean muted;
}
