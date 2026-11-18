package com.example.zolieapi.controller;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.request.CreateReportRequest;
import com.example.zolieapi.dto.response.ReportResponse;
import com.example.zolieapi.security.CurrentUser;
import com.example.zolieapi.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "18. Reports")
public class ReportController {

    private final ReportService reportService;
    private final CurrentUser currentUser;

    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Report an account, post, Reel, comment, story or message (once per item)")
    public ReportResponse create(@Valid @RequestBody CreateReportRequest request) {
        return reportService.create(currentUser.id(), request);
    }

    @GetMapping("/me/reports")
    @Operation(summary = "List my reports and their status")
    public CursorPage<ReportResponse> listMine(@RequestParam(required = false) String cursor,
                                               @RequestParam(required = false) Integer limit) {
        return reportService.listMine(currentUser.id(), cursor, limit);
    }
}
