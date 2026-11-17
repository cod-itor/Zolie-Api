package com.example.zolieapi.model;

import com.example.zolieapi.model.id.BlockId;
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
@Table(name = "blocks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Block {

    @EmbeddedId
    private BlockId id;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
