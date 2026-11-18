package com.example.zolieapi.controller;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.request.CollectionItemRequest;
import com.example.zolieapi.dto.request.CollectionRequest;
import com.example.zolieapi.dto.response.CollectionResponse;
import com.example.zolieapi.dto.response.SavedItemResponse;
import com.example.zolieapi.security.CurrentUser;
import com.example.zolieapi.service.CollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me/collections")
@RequiredArgsConstructor
@Tag(name = "25. Collections")
public class CollectionController {

    private final CollectionService collectionService;
    private final CurrentUser currentUser;

    @GetMapping
    @Operation(summary = "List my saved collections with item counts")
    public List<CollectionResponse> list() {
        return collectionService.list(currentUser.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a collection")
    public CollectionResponse create(@Valid @RequestBody CollectionRequest request) {
        return collectionService.create(currentUser.id(), request.name());
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Rename a collection")
    public CollectionResponse rename(@PathVariable UUID id, @Valid @RequestBody CollectionRequest request) {
        return collectionService.rename(currentUser.id(), id, request.name());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a collection (the items stay saved)")
    public void delete(@PathVariable UUID id) {
        collectionService.delete(currentUser.id(), id);
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "List the posts and Reels in a collection, newest first")
    public CursorPage<SavedItemResponse> listItems(@PathVariable UUID id,
                                                   @RequestParam(required = false) String cursor,
                                                   @RequestParam(required = false) Integer limit) {
        return collectionService.listItems(currentUser.id(), id, cursor, limit);
    }

    @PutMapping("/{id}/items")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Add a post or a Reel to a collection (it is saved too)")
    public void addItem(@PathVariable UUID id, @Valid @RequestBody CollectionItemRequest request) {
        collectionService.addItem(currentUser.id(), id, request);
    }

    @DeleteMapping("/{id}/items")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a post or a Reel from a collection")
    public void removeItem(@PathVariable UUID id,
                           @RequestParam(required = false) UUID postId,
                           @RequestParam(required = false) UUID reelId) {
        collectionService.removeItem(currentUser.id(), id, postId, reelId);
    }
}
