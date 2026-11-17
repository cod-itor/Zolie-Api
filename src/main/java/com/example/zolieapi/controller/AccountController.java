package com.example.zolieapi.controller;

import com.example.zolieapi.dto.request.SyncRequest;
import com.example.zolieapi.dto.request.UpdateProfileRequest;
import com.example.zolieapi.dto.response.MeResponse;
import com.example.zolieapi.security.CurrentUser;
import com.example.zolieapi.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
@Tag(name = "01. Account")
public class AccountController {

    private final AccountService accountService;
    private final CurrentUser currentUser;

    @PostMapping("/sync")
    @Operation(summary = "Create the account after sign-in, or return it if it already exists")
    public MeResponse sync(@Valid @RequestBody SyncRequest request) {
        return accountService.sync(currentUser.principal(), request);
    }

    @GetMapping
    @Operation(summary = "Get my profile, counts, settings, age group and dating status")
    public MeResponse getMe() {
        return accountService.getMe(currentUser.idAllowingSuspended());
    }

    @PatchMapping
    @Operation(summary = "Edit my profile (the date of birth cannot be changed)")
    public MeResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return accountService.updateProfile(currentUser.id(), request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete my account and all of its data")
    public void deleteAccount() {
        accountService.deleteAccount(currentUser.idAllowingSuspended());
    }
}
