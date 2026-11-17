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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "story_sticker_responses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryStickerResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID stickerId;

    private UUID responderId;

    private String answerText;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer sliderValue;

    private boolean remindMe;

    private UUID messageId;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
