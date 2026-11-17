package com.example.zolieapi.model;

import com.example.zolieapi.model.enums.MediaKind;
import com.example.zolieapi.model.enums.MediaPurpose;
import com.example.zolieapi.model.enums.MediaStatus;
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
@Table(name = "media_assets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID ownerId;

    private String objectKey;

    private MediaKind kind;

    private MediaPurpose purpose;

    private String mimeType;

    private Long sizeBytes;

    private Integer width;

    private Integer height;

    private Integer durationMs;

    @Builder.Default
    private MediaStatus status = MediaStatus.UPLOADING;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
