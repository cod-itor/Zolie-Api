package com.example.zolieapi.controller;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.response.ProfileResponse;
import com.example.zolieapi.dto.response.UserSummaryResponse;
import com.example.zolieapi.security.CurrentUser;
import com.example.zolieapi.service.FollowService;
import com.example.zolieapi.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "04. Users")
public class UserController {

    private final ProfileService profileService;
    private final FollowService followService;
    private final CurrentUser currentUser;

    @GetMapping("/{username}")
    @Operation(summary = "Get a profile by username (guests can see public accounts)")
    public ProfileResponse getProfile(@PathVariable String username) {
        return profileService.getByUsername(currentUser.idOrGuest(), username);
    }

    @GetMapping("/{id}/followers")
    @Operation(summary = "List an account's followers")
    public CursorPage<UserSummaryResponse> listFollowers(@PathVariable UUID id,
                                                         @RequestParam(required = false) String cursor,
                                                         @RequestParam(required = false) Integer limit) {
        return followService.listFollowers(currentUser.id(), id, cursor, limit);
    }

    @GetMapping("/{id}/following")
    @Operation(summary = "List the accounts someone follows")
    public CursorPage<UserSummaryResponse> listFollowing(@PathVariable UUID id,
                                                         @RequestParam(required = false) String cursor,
                                                         @RequestParam(required = false) Integer limit) {
        return followService.listFollowing(currentUser.id(), id, cursor, limit);
    }
}
