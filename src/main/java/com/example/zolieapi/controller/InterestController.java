package com.example.zolieapi.controller;

import com.example.zolieapi.dto.request.UpdateInterestsRequest;
import com.example.zolieapi.dto.response.InterestResponse;
import com.example.zolieapi.security.CurrentUser;
import com.example.zolieapi.service.InterestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "03. Interests")
public class InterestController {

    private final InterestService interestService;
    private final CurrentUser currentUser;

    @GetMapping("/interests")
    @SecurityRequirements
    @Operation(summary = "List all interests in English and Khmer (no token needed)")
    public List<InterestResponse> listAll() {
        return interestService.listAll();
    }

    @GetMapping("/me/interests")
    @Operation(summary = "List the interests I picked")
    public List<InterestResponse> listMine() {
        return interestService.listMine(currentUser.id());
    }

    @PutMapping("/me/interests")
    @Operation(summary = "Replace my interests (at least 3)")
    public List<InterestResponse> replaceMine(@Valid @RequestBody UpdateInterestsRequest request) {
        return interestService.replaceMine(currentUser.id(), request.interestIds());
    }
}
