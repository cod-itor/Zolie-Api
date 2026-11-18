package com.example.zolieapi.controller;

import com.example.zolieapi.dto.request.PostViewsRequest;
import com.example.zolieapi.dto.response.InsightsResponse;
import com.example.zolieapi.security.CurrentUser;
import com.example.zolieapi.service.InsightService;
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
@Tag(name = "22. Insights")
public class InsightController {

    private final InsightService insightService;
    private final CurrentUser currentUser;

    @GetMapping("/me/insights")
    @Operation(summary = "My reach, profile visits, new followers, top posts and audience for the last days")
    public InsightsResponse getMine(@RequestParam(required = false) Integer days) {
        return insightService.getMine(currentUser.id(), days);
    }

    @PostMapping("/posts/views")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Count a batch of post views (used for reach)")
    public void recordPostViews(@Valid @RequestBody PostViewsRequest request) {
        insightService.recordPostViews(currentUser.id(), request.postIds());
    }
}
