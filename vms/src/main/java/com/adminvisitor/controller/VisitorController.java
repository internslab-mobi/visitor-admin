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

@RestController
@RequestMapping("/api/visitors")
@RequiredArgsConstructor

@Tag(
        name = "Visitor Management",
        description = "APIs for retrieving and updating visitor information"
)
public class VisitorController {

    private final VisitorService visitorService;

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