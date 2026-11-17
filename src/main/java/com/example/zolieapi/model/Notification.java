package com.example.zolieapi.model;

import com.example.zolieapi.model.enums.NotificationTarget;
import com.example.zolieapi.model.enums.NotificationType;
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
@Table(name = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID recipientId;

    private UUID actorId;

    private NotificationType type;

    private NotificationTarget targetType;

    private UUID targetId;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    private Instant readAt;
}
