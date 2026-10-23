package com.adminvisitor.controller;

import com.adminvisitor.dto.responsedto.VisitorLogResponse;
import com.adminvisitor.enums.VisitStatus;
import com.adminvisitor.enums.VisitorType;
import com.adminvisitor.service.VisitManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST controller for retrieving visitor log records.
 *
 * <p>Provides a paginated visitor log with optional search criteria,
 * date range, visit status, visitor type, host, and sorting filters.</p>
 *
 * @author Nethra
 */
@Tag(
        name = "Visitor Log Management",
        description = "APIs for searching, filtering, and retrieving visitor log records"
)
@RestController
@RequestMapping("/api/visitor-log")
@RequiredArgsConstructor
public class VisitorLogController {

    private final VisitManagementService visitManagementService;

    /**
     * Retrieves visitor log records using optional filters and pagination.
     *
     * @param search        optional search term for matching visitor log records
     * @param fromDate      optional start date for filtering visit records
     * @param toDate        optional end date for filtering visit records
     * @param status        optional visit status filter
     * @param visitorType   optional visitor type filter
     * @param hostId        optional employee identifier of the visit host
     * @param page          zero-based page number, defaults to 0
     * @param size          number of records per page, defaults to 10
     * @param sortDirection sort direction, defaults to DESC
     * @return a page of visitor log records with HTTP status 200
     */
    @Operation(
            summary = "Get visitor log",
            description = "Retrieves paginated visitor log records with optional " +
                    "search, date range, status, visitor type, host, and sorting filters."
    )
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<VisitorLogResponse>> getVisitorLog(

            @Parameter(
                    description = "Search by supported visitor details, email, mobile, or visit reference",
                    example = "VTR-001"
            )
            @RequestParam(required = false)
            String search,

            @Parameter(
                    description = "Start date for filtering visit records, inclusive",
                    example = "2026-10-01"
            )
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @Parameter(
                    description = "End date for filtering visit records, inclusive",
                    example = "2026-10-22"
            )
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @Parameter(
                    description = "Filter records by visit status",
                    example = "CHECKED_OUT"
            )
            @RequestParam(required = false)
            VisitStatus status,

            @Parameter(
                    description = "Filter records by visitor type",
                    example = "VISITOR"
            )
            @RequestParam(required = false)
            VisitorType visitorType,

            @Parameter(
                    description = "Employee ID of the visit host",
                    example = "EMP-001"
            )
            @RequestParam(required = false)
            String hostId,

            @Parameter(
                    description = "Zero-based page number",
                    example = "0"
            )
            @RequestParam(defaultValue = "0")
            int page,

            @Parameter(
                    description = "Number of records per page",
                    example = "10"
            )
            @RequestParam(defaultValue = "10")
            int size,

            @Parameter(
                    description = "Sort direction for visitor log records",
                    example = "DESC"
            )
            @RequestParam(defaultValue = "DESC")
            String sortDirection
    ) {

        Page<VisitorLogResponse> response =
                visitManagementService.getVisitorLog(
                        search,
                        fromDate,
                        toDate,
                        status,
                        visitorType,
                        hostId,
                        page,
                        size,
                        sortDirection
                );

        return ResponseEntity.ok(response);
    }
}
