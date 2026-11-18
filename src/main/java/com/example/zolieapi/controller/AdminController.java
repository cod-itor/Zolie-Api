package com.example.zolieapi.controller;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.request.ModerationActionRequest;
import com.example.zolieapi.dto.response.AdminReportResponse;
import com.example.zolieapi.dto.response.AdminUserResponse;
import com.example.zolieapi.security.CurrentUser;
import com.example.zolieapi.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "19. Admin")
public class AdminController {

    private final AdminService adminService;
    private final CurrentUser currentUser;

    @GetMapping("/reports")
    @Operation(summary = "Report queue, oldest first, with the reported item (admin only)")
    public CursorPage<AdminReportResponse> listReports(
            @Parameter(schema = @Schema(allowableValues = {"open", "reviewing", "resolved", "dismissed"}))
            @RequestParam(defaultValue = "open") String status,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        return adminService.listReports(status, cursor, limit);
    }

    @PostMapping("/reports/{id}/actions")
    @Operation(summary = "Remove content, warn, suspend or dismiss; the reporter is notified (admin only)")
    public AdminReportResponse act(@PathVariable UUID id, @Valid @RequestBody ModerationActionRequest request) {
        return adminService.act(currentUser.id(), id, request);
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Account details with reports against it and past actions (admin only)")
    public AdminUserResponse getUser(@PathVariable UUID id) {
        return adminService.getUser(id);
    }
}
