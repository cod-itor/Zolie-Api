package com.example.zolieapi.controller;

import com.example.zolieapi.dto.request.UploadUrlRequest;
import com.example.zolieapi.dto.response.MediaResponse;
import com.example.zolieapi.dto.response.UploadUrlResponse;
import com.example.zolieapi.security.CurrentUser;
import com.example.zolieapi.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
@Tag(name = "08. Media")
public class MediaController {

    private final MediaService mediaService;
    private final CurrentUser currentUser;

    @PostMapping("/upload-url")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Step 1: get a presigned upload URL for a photo, video or audio file")
    public UploadUrlResponse createUpload(@Valid @RequestBody UploadUrlRequest request) {
        return mediaService.createUpload(currentUser.id(), request);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Step 2: confirm the upload so the file can be used in a post, Reel, story or chat")
    public MediaResponse complete(@PathVariable UUID id) {
        return mediaService.complete(currentUser.id(), id);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get the status of one of my files")
    public MediaResponse get(@PathVariable UUID id) {
        return mediaService.get(currentUser.id(), id);
    }
}
