package com.example.zolieapi.service;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.request.ModerationActionRequest;
import com.example.zolieapi.dto.response.AdminReportResponse;
import com.example.zolieapi.dto.response.AdminUserResponse;
import java.util.UUID;

public interface AdminService {

    CursorPage<AdminReportResponse> listReports(String status, String cursor, Integer limit);

    AdminReportResponse act(UUID adminId, UUID reportId, ModerationActionRequest request);

    AdminUserResponse getUser(UUID userId);
}
