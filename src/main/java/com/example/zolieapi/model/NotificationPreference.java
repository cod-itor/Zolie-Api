package com.example.zolieapi.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreference {

    @Id
    private UUID userId;

    @Builder.Default
    private boolean follows = true;

    @Builder.Default
    private boolean likes = true;

    @Builder.Default
    private boolean comments = true;

    @Builder.Default
    private boolean mentions = true;

    @Builder.Default
    private boolean matches = true;

    @Builder.Default
    private boolean messages = true;

    @Builder.Default
    private boolean storyInteractions = true;

    @Builder.Default
    private boolean events = true;

    private Instant pausedUntil;

    @UpdateTimestamp
    private Instant updatedAt;
}
