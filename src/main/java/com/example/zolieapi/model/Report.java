package com.example.zolieapi.model;

import com.example.zolieapi.model.enums.ReportReason;
import com.example.zolieapi.model.enums.ReportStatus;
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
@Table(name = "reports")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID reporterId;

    private ReportTarget targetType;

    private UUID targetId;

    private ReportReason reason;

    private String details;

    @Builder.Default
    private ReportStatus status = ReportStatus.OPEN;

    private UUID reviewedBy;

    private Instant reviewedAt;

    private String resolutionNote;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
