package com.adminvisitor.controller;

import com.adminvisitor.dto.BlacklistRequestDTO;
import com.adminvisitor.dto.BlacklistResponseDTO;
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
    public ResponseEntity<BlacklistResponseDTO> addToBlacklist(
            @Valid @RequestBody BlacklistRequestDTO request) {

        BlacklistResponseDTO response =
                blacklistService.addToBlacklist(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{id}/remove")
    public ResponseEntity<BlacklistResponseDTO> removeFromBlacklist(
            @PathVariable Long id,
            @RequestParam Long removedBy) {

        BlacklistResponseDTO response =
                blacklistService.removeFromBlacklist(
                        id,
                        removedBy
                );

        return ResponseEntity.ok(response);
    }
}