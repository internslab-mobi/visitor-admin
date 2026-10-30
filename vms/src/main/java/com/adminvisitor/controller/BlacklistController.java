package com.adminvisitor.controller;

import com.adminvisitor.dto.requestdto.BlacklistRequest;
import com.adminvisitor.dto.responsedto.BlacklistResponse;
import com.adminvisitor.service.BlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;

import java.util.List;

/**
 * REST controller for managing visitor blacklist records.
 *
 * <p>Provides endpoints to retrieve blacklist records, add existing
 * visitors to the blacklist, remove blacklist entries, and check
 * a visitor's blacklist status.</p>
 *
 * @author VMS Development Team
 */
@Tag(
        name = "Blacklist Management",
        description = "APIs for managing visitor blacklist records"
)
@RestController
@RequestMapping("/api/blacklist")
@RequiredArgsConstructor
@Slf4j
public class BlacklistController {

    private final BlacklistService blacklistService;

    /**
     * Removes an active blacklist record without deleting its audit history.
     *
     * @param id       unique identifier of the blacklist record
     * @param removedBy identifier of the employee removing the record
     * @return the updated blacklist record
     */
    @Operation(
            summary = "Remove a visitor from the blacklist",
            description = "Marks an active blacklist record as removed " +
                    "while retaining the record for audit purposes."
    )
    @PutMapping(value = "/{id}/remove", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BlacklistResponse> removeFromBlacklist(
            @Parameter(
                    description = "Unique identifier of the blacklist record",
                    example = "BL-001",
                    required = true
            )
            @PathVariable String id,

            @Parameter(
                    description = "Identifier of the employee removing the record",
                    example = "EMP-001",
                    required = true
            )
            @RequestParam String removedBy) {

        try {
            return ResponseEntity.ok(
                    blacklistService.removeFromBlacklist(id, removedBy)
            );
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid blacklist removal request. ID: {}, Removed by: {}",
                    id, removedBy, ex);
            throw ex;
        }
    }

    /**
     * Retrieves all blacklist records.
     *
     * @return a list of blacklist records
     */
    @Operation(
            summary = "Get all blacklist records",
            description = "Retrieves the available blacklist records, " +
                    "including their current statuses."
    )
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<BlacklistResponse>> getAllBlacklistRecords() {

        try {
            return ResponseEntity.ok(
                    blacklistService.getAllBlacklistRecords()
            );
        } catch (IllegalStateException ex) {
            log.error("Failed to retrieve blacklist records.", ex);
            throw ex;
        }
    }

    /**
     * Retrieves a specific blacklist record by its identifier.
     *
     * @param id unique identifier of the blacklist record
     * @return the requested blacklist record
     */
    @Operation(
            summary = "Get a blacklist record by ID",
            description = "Retrieves the details of a specific blacklist record."
    )
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BlacklistResponse> getBlacklistById(
            @Parameter(
                    description = "Unique identifier of the blacklist record",
                    example = "BL-001",
                    required = true
            )
            @PathVariable String id) {

        try {
            return ResponseEntity.ok(
                    blacklistService.getBlacklistById(id)
            );
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid blacklist record lookup. ID: {}", id, ex);
            throw ex;
        }
    }

    /**
     * Adds an existing visitor to the blacklist.
     *
     * @param visitorId unique identifier of the existing visitor
     * @param request   request containing the blacklist details
     * @return the newly created blacklist record with HTTP status 201
     */
    @Operation(
            summary = "Add an existing visitor to the blacklist",
            description = "Creates a blacklist record for an existing visitor " +
                    "using the details supplied in the request body."
    )
    @PostMapping(value = "/visitor/{visitorId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BlacklistResponse> addExistingVisitorToBlacklist(
            @Parameter(
                    description = "Unique identifier of the existing visitor",
                    example = "VTR-001",
                    required = true
            )
            @PathVariable String visitorId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Details required to add the visitor to the blacklist",
                    required = true
            )
            @Valid
            @RequestBody
            BlacklistRequest request) {

        try {
            BlacklistResponse response =
                    blacklistService.addExistingVisitorToBlacklist(
                            visitorId, request
                    );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException ex) {
            log.warn("Invalid blacklist creation request for visitor: {}",
                    visitorId, ex);
            throw ex;
        }
    }

    /**
     * Checks whether a visitor is currently blacklisted.
     *
     * @param visitorId unique identifier of the visitor
     * @return true if the visitor is blacklisted; otherwise, false
     */
    @Operation(
            summary = "Check visitor blacklist status",
            description = "Checks whether the specified visitor is currently " +
                    "blacklisted."
    )
    @GetMapping(value = "/visitor/{visitorId}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> isBlacklisted(
            @Parameter(
                    description = "Unique identifier of the visitor to check",
                    example = "VTR-001",
                    required = true
            )
            @PathVariable String visitorId) {

        try {
            return ResponseEntity.ok(
                    blacklistService.isBlacklisted(visitorId)
            );
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid blacklist status lookup for visitor: {}",
                    visitorId, ex);
            throw ex;
        }
    }
}
