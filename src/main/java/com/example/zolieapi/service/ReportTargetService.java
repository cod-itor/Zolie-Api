package com.example.zolieapi.service;

import com.example.zolieapi.model.enums.ReportTarget;
import java.util.Optional;
import java.util.UUID;

public interface ReportTargetService {

    Optional<UUID> findOwner(ReportTarget targetType, UUID targetId);

    boolean canReport(UUID reporterId, ReportTarget targetType, UUID targetId);

    String preview(ReportTarget targetType, UUID targetId);

    void remove(ReportTarget targetType, UUID targetId);
}
