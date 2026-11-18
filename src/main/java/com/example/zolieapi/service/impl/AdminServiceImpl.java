package com.example.zolieapi.service.impl;

import com.example.zolieapi.common.Ages;
import com.example.zolieapi.common.Cursor;
import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.common.Enums;
import com.example.zolieapi.common.Texts;
import com.example.zolieapi.dto.request.ModerationActionRequest;
import com.example.zolieapi.dto.response.AdminReportResponse;
import com.example.zolieapi.dto.response.AdminUserResponse;
import com.example.zolieapi.dto.response.UserSummaryResponse;
import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.mapper.ReportMapper;
import com.example.zolieapi.mapper.UserMapper;
import com.example.zolieapi.model.ModerationAction;
import com.example.zolieapi.model.Report;
import com.example.zolieapi.model.User;
import com.example.zolieapi.model.enums.AccountStatus;
import com.example.zolieapi.model.enums.ModerationActionType;
import com.example.zolieapi.model.enums.NotificationTarget;
import com.example.zolieapi.model.enums.NotificationType;
import com.example.zolieapi.model.enums.ReportStatus;
import com.example.zolieapi.model.enums.ReportTarget;
import com.example.zolieapi.model.enums.UserRole;
import com.example.zolieapi.repository.ModerationActionRepository;
import com.example.zolieapi.repository.ReportRepository;
import com.example.zolieapi.repository.UserRepository;
import com.example.zolieapi.service.AdminService;
import com.example.zolieapi.service.NotificationService;
import com.example.zolieapi.service.ReportTargetService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final int MAX_NOTE = 500;

    private final ReportRepository reportRepository;
    private final ModerationActionRepository actionRepository;
    private final UserRepository userRepository;
    private final ReportTargetService reportTargetService;
    private final NotificationService notificationService;
    private final ReportMapper reportMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public CursorPage<AdminReportResponse> listReports(String status, String cursor, Integer limit) {
        ReportStatus parsed = status == null ? ReportStatus.OPEN : Enums.parse(ReportStatus.class, status, "status");
        int size = CursorPage.limit(limit);
        Cursor from = Cursor.decodeAscending(cursor);
        return CursorPage.of(reportRepository.findQueue(Enums.value(parsed), from.time(), from.id(), size + 1), size,
                report -> new Cursor(report.getCreatedAt(), report.getId()), this::toResponses);
    }

    @Override
    @Transactional
    public AdminReportResponse act(UUID adminId, UUID reportId, ModerationActionRequest request) {
        Report report = reportRepository.findById(reportId).orElseThrow(ApiException::notFound);
        if (report.getStatus() == ReportStatus.RESOLVED || report.getStatus() == ReportStatus.DISMISSED) {
            throw ApiException.rule("This report was already reviewed.");
        }
        String note = Texts.clean(request.note(), MAX_NOTE, "note");
        Optional<UUID> ownerId = reportTargetService.findOwner(report.getTargetType(), report.getTargetId());

        switch (request.action().trim().toLowerCase()) {
            case "remove" -> {
                reportTargetService.remove(report.getTargetType(), report.getTargetId());
                record(adminId, report, ModerationActionType.REMOVE_CONTENT, report.getTargetType(), report.getTargetId(), note);
                ownerId.ifPresent(this::notifyOwner);
            }
            case "warn" -> {
                UUID owner = ownerId.orElseThrow(() -> ApiException.rule("The reported account no longer exists."));
                record(adminId, report, ModerationActionType.WARN_USER, ReportTarget.USER, owner, note);
                notifyOwner(owner);
            }
            case "suspend" -> {
                UUID owner = ownerId.orElseThrow(() -> ApiException.rule("The reported account no longer exists."));
                suspend(owner, request.suspendedUntil());
                record(adminId, report, ModerationActionType.SUSPEND_USER, ReportTarget.USER, owner, note);
                notifyOwner(owner);
            }
            case "dismiss" -> record(adminId, report, ModerationActionType.DISMISS_REPORT, report.getTargetType(),
                    report.getTargetId(), note);
            default -> throw ApiException.invalid("action must be one of: remove, warn, suspend, dismiss");
        }

        boolean dismissed = "dismiss".equalsIgnoreCase(request.action().trim());
        report.setStatus(dismissed ? ReportStatus.DISMISSED : ReportStatus.RESOLVED);
        report.setReviewedBy(adminId);
        report.setReviewedAt(Instant.now());
        report.setResolutionNote(note);
        reportRepository.saveAndFlush(report);
        notificationService.send(report.getReporterId(), null, NotificationType.REPORT_RESOLVED,
                NotificationTarget.REPORT, report.getId());
        return toResponses(List.of(report)).get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse getUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(ApiException::notFound);
        return new AdminUserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getEmail(),
                Enums.value(user.getRole()), Enums.value(user.getStatus()), Ages.group(user.getDateOfBirth()),
                user.getSuspendedUntil(), user.getCreatedAt(),
                reportRepository.findTop50ByTargetTypeAndTargetIdOrderByCreatedAtDesc(ReportTarget.USER, userId).stream()
                        .map(reportMapper::toResponse).toList(),
                actionRepository.findTop50ByTargetTypeAndTargetIdOrderByCreatedAtDesc(ReportTarget.USER, userId).stream()
                        .map(reportMapper::toResponse).toList());
    }

    private void suspend(UUID userId, Instant until) {
        if (until != null && !until.isAfter(Instant.now())) {
            throw ApiException.rule("suspendedUntil must be in the future.");
        }
        User user = userRepository.findById(userId).orElseThrow(ApiException::notFound);
        if (user.getRole() == UserRole.ADMIN) {
            throw ApiException.rule("An admin account cannot be suspended.");
        }
        user.setStatus(AccountStatus.SUSPENDED);
        user.setSuspendedUntil(until);
        userRepository.saveAndFlush(user);
    }

    private void record(UUID adminId, Report report, ModerationActionType type, ReportTarget targetType, UUID targetId,
                        String note) {
        actionRepository.save(ModerationAction.builder()
                .adminId(adminId)
                .reportId(report.getId())
                .action(type)
                .targetType(targetType)
                .targetId(targetId)
                .note(note)
                .build());
        log.info("Admin action: admin={} action={} targetType={} targetId={} report={}",
                adminId, type, targetType, targetId, report.getId());
    }

    private void notifyOwner(UUID ownerId) {
        notificationService.send(ownerId, null, NotificationType.MODERATION_NOTICE, null, null);
    }

    private List<AdminReportResponse> toResponses(List<Report> reports) {
        List<UUID> userIds = new ArrayList<>();
        List<Optional<UUID>> owners = new ArrayList<>();
        for (Report report : reports) {
            Optional<UUID> owner = reportTargetService.findOwner(report.getTargetType(), report.getTargetId());
            owners.add(owner);
            userIds.add(report.getReporterId());
            owner.ifPresent(userIds::add);
        }
        Map<UUID, UserSummaryResponse> users = userMapper.summariesById(userIds.stream().filter(Objects::nonNull).toList());
        List<AdminReportResponse> responses = new ArrayList<>();
        for (int index = 0; index < reports.size(); index++) {
            Report report = reports.get(index);
            responses.add(new AdminReportResponse(
                    reportMapper.toResponse(report),
                    users.get(report.getReporterId()),
                    owners.get(index).map(users::get).orElse(null),
                    reportTargetService.preview(report.getTargetType(), report.getTargetId())));
        }
        return responses;
    }
}
