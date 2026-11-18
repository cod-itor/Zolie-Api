package com.example.zolieapi.service;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.request.CreateReportRequest;
import com.example.zolieapi.dto.response.ReportResponse;
import java.util.UUID;

public interface ReportService {

    ReportResponse create(UUID userId, CreateReportRequest request);

    CursorPage<ReportResponse> listMine(UUID userId, String cursor, Integer limit);
}
