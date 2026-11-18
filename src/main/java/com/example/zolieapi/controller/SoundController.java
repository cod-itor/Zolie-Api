package com.example.zolieapi.controller;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.request.CreateSoundRequest;
import com.example.zolieapi.dto.response.ReelResponse;
import com.example.zolieapi.dto.response.SoundResponse;
import com.example.zolieapi.security.CurrentUser;
import com.example.zolieapi.service.ReelService;
import com.example.zolieapi.service.SoundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sounds")
@RequiredArgsConstructor
@Tag(name = "23. Sounds")
public class SoundController {

    private final SoundService soundService;
    private final ReelService reelService;
    private final CurrentUser currentUser;

    @GetMapping
    @Operation(summary = "Search songs and sounds for Add music; without q it lists the most used")
    public List<SoundResponse> search(@RequestParam(required = false) String q) {
        return soundService.search(q);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a sound from an uploaded audio file")
    public SoundResponse create(@Valid @RequestBody CreateSoundRequest request) {
        return soundService.create(currentUser.id(), request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one sound with its use count")
    public SoundResponse get(@PathVariable UUID id) {
        return soundService.get(id);
    }

    @GetMapping("/{id}/reels")
    @Operation(summary = "List the Reels that use a sound")
    public CursorPage<ReelResponse> listReels(@PathVariable UUID id,
                                              @RequestParam(required = false) String cursor,
                                              @RequestParam(required = false) Integer limit) {
        soundService.get(id);
        return reelService.listBySound(currentUser.id(), id, cursor, limit);
    }
}
