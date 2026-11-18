package com.example.zolieapi.repository;

import com.example.zolieapi.model.ModerationAction;
import com.example.zolieapi.model.enums.ReportTarget;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModerationActionRepository extends JpaRepository<ModerationAction, UUID> {

    List<ModerationAction> findTop50ByTargetTypeAndTargetIdOrderByCreatedAtDesc(ReportTarget targetType, UUID targetId);
}
