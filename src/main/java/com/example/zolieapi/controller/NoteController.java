package com.example.zolieapi.controller;

import com.example.zolieapi.dto.request.NoteRequest;
import com.example.zolieapi.dto.response.NoteResponse;
import com.example.zolieapi.security.CurrentUser;
import com.example.zolieapi.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "15. Messaging")
public class NoteController {

    private final NoteService noteService;
    private final CurrentUser currentUser;

    @GetMapping("/notes")
    @Operation(summary = "Notes above the inbox: mine first, then people I follow (gone after 24 hours)")
    public List<NoteResponse> feed() {
        return noteService.feed(currentUser.id());
    }

    @PutMapping("/me/note")
    @Operation(summary = "Set my note (up to 60 characters, shown for 24 hours)")
    public NoteResponse setMine(@Valid @RequestBody NoteRequest request) {
        return noteService.setMine(currentUser.id(), request.text());
    }

    @DeleteMapping("/me/note")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove my note")
    public void deleteMine() {
        noteService.deleteMine(currentUser.id());
    }
}
