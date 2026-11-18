package com.example.zolieapi.model;

import com.example.zolieapi.model.enums.StoryAudience;
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
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "stories")
@DynamicUpdate
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Story {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID authorId;

    private UUID mediaId;

    @Builder.Default
    private StoryAudience audience = StoryAudience.EVERYONE;

    private int viewCount;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    private Instant expiresAt;

    private String textContent;

    private String textBackground;

    private UUID sharedPostId;
}
