package com.example.zolieapi.dto.response;

public record AdminReportResponse(
        ReportResponse report,
        UserSummaryResponse reporter,
        UserSummaryResponse targetOwner,
        String targetPreview
) {
}
