package com.example.zolieapi.controller;

import com.example.zolieapi.dto.response.HelpArticleResponse;
import com.example.zolieapi.service.HelpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "21. Help")
public class HelpController {

    private final HelpService helpService;

    @GetMapping("/help/articles")
    @Operation(summary = "Help and safety articles in English and Khmer (no token needed)")
    public List<HelpArticleResponse> list(
            @Parameter(schema = @Schema(allowableValues = {"account", "privacy", "dating_safety", "reporting"}))
            @RequestParam(required = false) String topic) {
        return helpService.list(topic);
    }
}
