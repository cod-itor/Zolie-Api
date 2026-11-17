package com.example.zolieapi.controller;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.response.UserSummaryResponse;
import com.example.zolieapi.security.CurrentUser;
import com.example.zolieapi.service.BlockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "06. Blocks")
public class BlockController {

    private final BlockService blockService;
    private final CurrentUser currentUser;

    @PutMapping("/users/{id}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Block an account: removes follows both ways and closes any match")
    public void block(@PathVariable UUID id) {
        blockService.block(currentUser.id(), id);
    }

    @DeleteMapping("/users/{id}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Unblock an account")
    public void unblock(@PathVariable UUID id) {
        blockService.unblock(currentUser.id(), id);
    }

    @GetMapping("/me/blocks")
    @Operation(summary = "List the accounts I blocked")
    public CursorPage<UserSummaryResponse> listBlocked(@RequestParam(required = false) String cursor,
                                                       @RequestParam(required = false) Integer limit) {
        return blockService.listBlocked(currentUser.id(), cursor, limit);
    }
}
