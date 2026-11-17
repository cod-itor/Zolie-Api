package com.example.zolieapi.model;

import com.example.zolieapi.model.enums.FollowStatus;
import com.example.zolieapi.model.id.FollowId;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "follows")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Follow {

    @EmbeddedId
    private FollowId id;

    @Builder.Default
    private FollowStatus status = FollowStatus.ACCEPTED;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    private Instant acceptedAt;
}
