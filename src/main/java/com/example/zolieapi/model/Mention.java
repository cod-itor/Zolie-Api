package com.example.zolieapi.model;

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
@Table(name = "mentions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mention {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID mentionedUserId;

    private UUID authorId;

    private UUID postId;

    private UUID reelId;

    private UUID commentId;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
