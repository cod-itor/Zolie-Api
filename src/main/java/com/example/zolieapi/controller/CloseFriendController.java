package com.example.zolieapi.controller;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.response.UserSummaryResponse;
import com.example.zolieapi.security.CurrentUser;
import com.example.zolieapi.service.CloseFriendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me/close-friends")
@RequiredArgsConstructor
@Tag(name = "07. Close Friends")
public class CloseFriendController {

    private final CloseFriendService closeFriendService;
    private final CurrentUser currentUser;

    @GetMapping
    @Operation(summary = "List my Close Friends (private to me)")
    public CursorPage<UserSummaryResponse> list(@RequestParam(required = false) String cursor,
                                                @RequestParam(required = false) Integer limit) {
        return closeFriendService.list(currentUser.id(), cursor, limit);
    }

    @PutMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Add someone to Close Friends (no notification is sent)")
    public void add(@PathVariable UUID userId) {
        closeFriendService.add(currentUser.id(), userId);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove someone from Close Friends")
    public void remove(@PathVariable UUID userId) {
        closeFriendService.remove(currentUser.id(), userId);
    }
}
