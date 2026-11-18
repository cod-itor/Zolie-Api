package com.example.zolieapi.mapper;

import com.example.zolieapi.common.Enums;
import com.example.zolieapi.dto.response.ModerationActionResponse;
import com.example.zolieapi.dto.response.ReportResponse;
import com.example.zolieapi.model.ModerationAction;
import com.example.zolieapi.model.Report;
import org.springframework.stereotype.Component;

@Component
public class ReportMapper {

    public ReportResponse toResponse(Report report) {
        return new ReportResponse(report.getId(), Enums.value(report.getTargetType()), report.getTargetId(),
                Enums.value(report.getReason()), report.getDetails(), Enums.value(report.getStatus()),
                report.getResolutionNote(), report.getCreatedAt(), report.getReviewedAt());
    }

    public ModerationActionResponse toResponse(ModerationAction action) {
        return new ModerationActionResponse(action.getId(), Enums.value(action.getAction()),
                Enums.value(action.getTargetType()), action.getTargetId(), action.getNote(), action.getAdminId(),
                action.getCreatedAt());
    }
}
