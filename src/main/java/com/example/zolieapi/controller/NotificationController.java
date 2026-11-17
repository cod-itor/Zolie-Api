package com.example.zolieapi.controller;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.request.MarkNotificationsReadRequest;
import com.example.zolieapi.dto.response.NotificationResponse;
import com.example.zolieapi.dto.response.UnreadCountResponse;
import com.example.zolieapi.security.CurrentUser;
import com.example.zolieapi.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "16. Notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUser currentUser;

    @GetMapping
    @Operation(summary = "List my activity, newest first")
    public CursorPage<NotificationResponse> list(@RequestParam(required = false) String cursor,
                                                 @RequestParam(required = false) Integer limit) {
        return notificationService.list(currentUser.id(), cursor, limit);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Count my unread notifications")
    public UnreadCountResponse unreadCount() {
        return notificationService.unreadCount(currentUser.id());
    }

    @PostMapping("/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Mark notifications as read up to one item, or all of them")
    public void markRead(@RequestBody(required = false) MarkNotificationsReadRequest request) {
        UUID upToId = request == null ? null : request.upToId();
        notificationService.markRead(currentUser.id(), upToId);
    }
}
