package com.example.zolieapi.controller;

import com.example.zolieapi.dto.request.ResolveLoginRequest;
import com.example.zolieapi.dto.response.AvailabilityResponse;
import com.example.zolieapi.dto.response.ResolveLoginResponse;
import com.example.zolieapi.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "01. Account")
public class AuthController {

    private final AccountService accountService;

    @GetMapping("/usernames/{username}/availability")
    @Operation(summary = "Check whether a username is free (no token needed)")
    public AvailabilityResponse checkUsername(@PathVariable String username, HttpServletRequest request) {
        return accountService.checkUsername(username, request.getRemoteAddr());
    }

    @PostMapping("/auth/resolve-login")
    @Operation(summary = "Turn a username into the email used for sign-in (no token needed)")
    public ResolveLoginResponse resolveLogin(@Valid @RequestBody ResolveLoginRequest body, HttpServletRequest request) {
        return accountService.resolveLogin(body.username(), request.getRemoteAddr());
    }
}
