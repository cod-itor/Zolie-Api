package com.example.zolieapi.model;

import com.example.zolieapi.model.enums.MessageType;
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
@Table(name = "messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID conversationId;

    private UUID senderId;

    @Builder.Default
    private MessageType type = MessageType.TEXT;

    private String text;

    private UUID mediaId;

    private UUID replyToStoryId;

    private UUID sharedPostId;

    private UUID sharedReelId;

    private UUID clientMessageId;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    private Instant deletedAt;
}
