package com.adminvisitor.controller;

import com.adminvisitor.dto.requestdto.BlacklistRequest;
import com.adminvisitor.dto.responsedto.BlacklistResponse;
import com.adminvisitor.service.BlacklistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/blacklist")
@RequiredArgsConstructor
public class BlacklistController {

    private final BlacklistService blacklistService;

    @PostMapping
    public ResponseEntity<BlacklistResponse> addToBlacklist(
            @Valid @RequestBody BlacklistRequest request) {

        BlacklistResponse response =
                blacklistService.addToBlacklist(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{id}/remove")
    public ResponseEntity<BlacklistResponse> removeFromBlacklist(
            @PathVariable Long id,
            @RequestParam Long removedBy) {

        BlacklistResponse response =
                blacklistService.removeFromBlacklist(
                        id,
                        removedBy
                );

        return ResponseEntity.ok(response);
    }
}