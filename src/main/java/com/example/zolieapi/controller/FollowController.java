package com.example.zolieapi.controller;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.response.FollowResponse;
import com.example.zolieapi.dto.response.UserSummaryResponse;
import com.example.zolieapi.security.CurrentUser;
import com.example.zolieapi.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "05. Follows")
public class FollowController {

    private final FollowService followService;
    private final CurrentUser currentUser;

    @PutMapping("/users/{id}/follow")
    @Operation(summary = "Follow an account: returns following (public) or requested (private)")
    public FollowResponse follow(@PathVariable UUID id) {
        return followService.follow(currentUser.id(), id);
    }

    @DeleteMapping("/users/{id}/follow")
    @Operation(summary = "Unfollow an account or cancel a follow request")
    public FollowResponse unfollow(@PathVariable UUID id) {
        return followService.unfollow(currentUser.id(), id);
    }

    @DeleteMapping("/me/followers/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove one of my followers")
    public void removeFollower(@PathVariable UUID userId) {
        followService.removeFollower(currentUser.id(), userId);
    }

    @GetMapping("/me/follow-requests")
    @Operation(summary = "List pending follow requests to my private account")
    public CursorPage<UserSummaryResponse> listRequests(@RequestParam(required = false) String cursor,
                                                        @RequestParam(required = false) Integer limit) {
        return followService.listRequests(currentUser.id(), cursor, limit);
    }

    @PostMapping("/me/follow-requests/{userId}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Approve a follow request")
    public void acceptRequest(@PathVariable UUID userId) {
        followService.acceptRequest(currentUser.id(), userId);
    }

    @DeleteMapping("/me/follow-requests/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Decline a follow request")
    public void declineRequest(@PathVariable UUID userId) {
        followService.declineRequest(currentUser.id(), userId);
    }
}
