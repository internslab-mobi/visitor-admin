package com.adminvisitor.controller;

import com.adminvisitor.dto.responsedto.VisitorLogResponse;
import com.adminvisitor.enums.VisitStatus;
import com.adminvisitor.enums.VisitorType;
import com.adminvisitor.service.VisitService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/visitor-log")
@RequiredArgsConstructor
public class VisitorLogController {

    private final VisitService visitService;

    @GetMapping
    public ResponseEntity<Page<VisitorLogResponse>> getVisitorLog(

            @RequestParam(required = false)
            String search,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(required = false)
            VisitStatus status,

            @RequestParam(required = false)
            VisitorType visitorType,

            @RequestParam(required = false)
            String hostId,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "DESC")
            String sortDirection
    ) {

        Page<VisitorLogResponse> response =
                visitService.getVisitorLog(
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