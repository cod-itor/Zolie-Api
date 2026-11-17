package com.example.zolieapi.model;

import com.example.zolieapi.model.enums.StickerType;
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
@Table(name = "story_stickers")
@DynamicUpdate
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorySticker {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID storyId;

    private StickerType type;

    private String prompt;

    private String emoji;

    private Instant countdownEndsAt;

    @Column(name = "pos_x", columnDefinition = "real")
    @Builder.Default
    private float posX = 0.5f;

    @Column(name = "pos_y", columnDefinition = "real")
    @Builder.Default
    private float posY = 0.5f;

    @Column(columnDefinition = "real")
    @Builder.Default
    private float scale = 1.0f;

    @Column(columnDefinition = "real")
    private float rotationDeg;

    private int responseCount;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
