package com.example.zolieapi.service.impl;

import com.example.zolieapi.common.Cursor;
import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.common.Enums;
import com.example.zolieapi.common.Texts;
import com.example.zolieapi.dto.request.CreateReportRequest;
import com.example.zolieapi.dto.response.ReportResponse;
import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.exception.ErrorCode;
import com.example.zolieapi.mapper.ReportMapper;
import com.example.zolieapi.model.Report;
import com.example.zolieapi.model.enums.ReportReason;
import com.example.zolieapi.model.enums.ReportTarget;
import com.example.zolieapi.repository.ReportRepository;
import com.example.zolieapi.security.RateLimiter;
import com.example.zolieapi.service.ReportService;
import com.example.zolieapi.service.ReportTargetService;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final int MAX_DETAILS = 1000;

    private final ReportRepository reportRepository;
    private final ReportTargetService reportTargetService;
    private final RateLimiter rateLimiter;
    private final ReportMapper reportMapper;

    @Override
    @Transactional
    public ReportResponse create(UUID userId, CreateReportRequest request) {
        ReportTarget targetType = Enums.require(ReportTarget.class, request.targetType(), "targetType");
        ReportReason reason = Enums.require(ReportReason.class, request.reason(), "reason");
        if (targetType == ReportTarget.USER && userId.equals(request.targetId())) {
            throw ApiException.rule("You cannot report yourself.");
        }
        if (!reportTargetService.canReport(userId, targetType, request.targetId())) {
            throw ApiException.notFound();
        }
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(userId, targetType, request.targetId())) {
            throw new ApiException(ErrorCode.ALREADY_REPORTED);
        }
        rateLimiter.check("report", userId, 5, Duration.ofHours(1));
        Report report = reportRepository.save(Report.builder()
                .reporterId(userId)
                .targetType(targetType)
                .targetId(request.targetId())
                .reason(reason)
                .details(Texts.clean(request.details(), MAX_DETAILS, "details"))
                .build());
        return reportMapper.toResponse(report);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<ReportResponse> listMine(UUID userId, String cursor, Integer limit) {
        int size = CursorPage.limit(limit);
        Cursor from = Cursor.decode(cursor);
        return CursorPage.of(reportRepository.findByReporter(userId, from.time(), from.id(), size + 1), size,
                report -> new Cursor(report.getCreatedAt(), report.getId()),
                page -> page.stream().map(reportMapper::toResponse).toList());
    }
}
