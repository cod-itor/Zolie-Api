package com.example.zolieapi.model;

import com.example.zolieapi.model.id.IdempotencyKeyId;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
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
@Table(name = "idempotency_keys")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyKey {

    @EmbeddedId
    private IdempotencyKeyId id;

    private String resourceType;

    private UUID resourceId;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
