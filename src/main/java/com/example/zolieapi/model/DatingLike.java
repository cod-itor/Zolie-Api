package com.example.zolieapi.model;

import com.example.zolieapi.model.enums.DatingLikeType;
import com.example.zolieapi.model.id.UserPairId;
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
@Table(name = "dating_likes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatingLike {

    @EmbeddedId
    private UserPairId id;

    @Builder.Default
    private DatingLikeType type = DatingLikeType.SWIPE;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
