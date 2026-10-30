package com.adminvisitor.controller;

import com.adminvisitor.dto.requestdto.UpdateVisitorRequest;
import com.adminvisitor.dto.responsedto.VisitorEditResponse;
import com.adminvisitor.dto.responsedto.VisitorResponse;
import com.adminvisitor.service.VisitorService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * REST controller for managing visitor information.
 *
 * <p>Provides APIs for retrieving individual visitor details,
 * retrieving all visitors, updating visitor information, and
 * retrieving the details required to populate the visitor edit page.</p>
 *
 * @author Anisha Z
 */
@RestController
@RequestMapping("/api/visitors")
@RequiredArgsConstructor

@Tag(
        name = "Visitor Management",
        description = "APIs for retrieving and updating visitor information"
)
public class VisitorController {

    private final VisitorService visitorService;


    /**
     * Retrieves the details of a specific visitor using the visitor ID.
     *
     * @param visitorId the unique identifier of the visitor
     * @return the visitor details with HTTP status 200
     */
    @GetMapping(
            value = "/{visitorId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get visitor by ID",
            description = "Retrieves the details of a specific visitor using the visitor ID."
    )

    public ResponseEntity<VisitorResponse> getVisitor(
            @Parameter(
                    name = "visitorId",
                    description = "Unique identifier of the visitor",
                    required = true,
                    example = "VIS001"
            )@PathVariable String visitorId) {

        VisitorResponse response =
                visitorService.getVisitor(visitorId);

        return ResponseEntity.ok(response);
    }

    /**
     * Updates the information of an existing visitor.
     *
     * <p>Validates the incoming request body and delegates the update
     * operation to the visitor service using the specified visitor ID.</p>
     *
     * @param visitorId the unique identifier of the visitor to update
     * @param request the request containing the updated visitor information
     * @return the updated visitor details with HTTP status 200
     */
    @PutMapping(
            value = "/{visitorId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Update visitor details",
            description = "Updates an existing visitor's information using the visitor ID and the supplied request data."
    )

    public ResponseEntity<VisitorResponse> updateVisitor(
            @Parameter(
                    name = "visitorId",
                    description = "Unique identifier of the visitor to update",
                    required = true,
                    example = "VIS001"
            )
            @PathVariable("visitorId") String visitorId,
            @Valid @RequestBody UpdateVisitorRequest request) {

        VisitorResponse response =
                visitorService.updateVisitor(visitorId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the list of all visitors available in the VMS application.
     *
     * @return the list of visitor details with HTTP status 200
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get all visitors",
            description = "Retrieves the list of visitors available in the VMS application."
    )

    public ResponseEntity<List<VisitorResponse>> getAllVisitors() {

        List<VisitorResponse> response =
                visitorService.getAllVisitors();

        return ResponseEntity.ok(response);
    }


    /**
     * Retrieves the visitor information required to populate the edit page.
     *
     * <p>Returns the edit-specific response for the visitor identified
     * by the supplied visitor ID.</p>
     *
     * @param visitorId the unique identifier of the visitor
     * @return the visitor edit details with HTTP status 200
     */
    @GetMapping(
            value = "/{visitorId}/edit",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get visitor edit details",
            description = "Retrieves the visitor information required to populate the visitor edit page."
    )

    public ResponseEntity<VisitorEditResponse> getVisitorEditDetails(
            @Parameter(
                    name = "visitorId",
                    description = "Unique identifier of the visitor",
                    required = true,
                    example = "VIS001"
            )
            @PathVariable("visitorId") String visitorId
    ) {

        return ResponseEntity.ok(
                visitorService.getVisitorEditDetails(visitorId)
        );
    }



}