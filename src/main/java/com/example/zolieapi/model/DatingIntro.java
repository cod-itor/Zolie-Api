package com.example.zolieapi.model;

import com.example.zolieapi.model.enums.IntroStatus;
import com.example.zolieapi.model.id.UserPairId;
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
@Table(name = "dating_intros")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatingIntro {

    @EmbeddedId
    private UserPairId id;

    private String messageText;

    @Builder.Default
    private IntroStatus status = IntroStatus.PENDING;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant sentAt;

    private UUID promptAnswerId;
}
