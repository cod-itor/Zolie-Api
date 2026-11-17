package com.example.zolieapi.model;

import com.example.zolieapi.model.enums.ConversationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "conversations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private ConversationType type;

    @Column(name = "user_a_id")
    private UUID userAId;

    @Column(name = "user_b_id")
    private UUID userBId;

    private UUID matchId;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    private Instant lastMessageAt;
}
