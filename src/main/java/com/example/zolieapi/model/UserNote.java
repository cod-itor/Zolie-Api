package com.example.zolieapi.model;

import jakarta.persistence.Column;
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
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "user_notes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNote {

    @Id
    private UUID userId;

    private String text;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    private Instant expiresAt;
}
