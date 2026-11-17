package com.example.zolieapi.model;

import com.example.zolieapi.model.enums.ModerationActionType;
import com.example.zolieapi.model.enums.ReportTarget;
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
@Table(name = "moderation_actions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationAction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID adminId;

    private UUID reportId;

    private ModerationActionType action;

    private ReportTarget targetType;

    private UUID targetId;

    private String note;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
