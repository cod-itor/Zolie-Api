package com.example.zolieapi.model;

import com.example.zolieapi.model.id.CloseFriendId;
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
@Table(name = "close_friends")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloseFriend {

    @EmbeddedId
    private CloseFriendId id;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
