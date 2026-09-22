package com.adminvisitor.controller;

import com.adminvisitor.dto.requestdto.RegistrationRequest;
import com.adminvisitor.dto.responsedto.NdaStatusResponse;
import com.adminvisitor.dto.responsedto.RegistrationResponse;
import com.adminvisitor.dto.responsedto.VisitDashboardResponse;
import com.adminvisitor.dto.responsedto.VisitDetailResponse;
import com.adminvisitor.service.VisitService;
import com.adminvisitor.enums.VisitStatus;
import com.adminvisitor.enums.VisitView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/visits")
@RequiredArgsConstructor
public class VisitController {

    private final VisitService visitService;

    @PostMapping
    public ResponseEntity<RegistrationResponse> register(
            @Valid @RequestBody RegistrationRequest request) {

        RegistrationResponse response =
                visitService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    @GetMapping
    public ResponseEntity<List<VisitDashboardResponse>> getDashboardVisits(

            @RequestParam(required = false)
            VisitView view,

            @RequestParam(required = false)
            String visitorId,

            @RequestParam(required = false)
            String visitorName,

            @RequestParam(required = false)
            String visitorEmail,

            @RequestParam(required = false)
            VisitStatus status,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(defaultValue = "ASC")
            String sortDirection
    ) {

        List<VisitDashboardResponse> visits =
                visitService.getDashboardVisits(
                        view,
                        visitorId,
                        visitorName,
                        visitorEmail,
                        status,
                        date,
                        fromDate,
                        toDate,
                        sortDirection
                );

        return ResponseEntity.ok(visits);
    }

    @GetMapping("/{visitId}")
    public ResponseEntity<VisitDetailResponse> getVisitDetails(
            @PathVariable String visitId
    ) {

        VisitDetailResponse response =
                visitService.getVisitDetails(visitId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{visitId}/cancel")
    public ResponseEntity<VisitDetailResponse> cancelVisit(
            @PathVariable String visitId
    ) {

        VisitDetailResponse response =
                visitService.cancelVisit(visitId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{visitId}/check-in")
    public ResponseEntity<VisitDetailResponse> checkIn(
            @PathVariable String visitId) {

        return ResponseEntity.ok(
                visitService.checkIn(visitId)
        );
    }

    @PatchMapping("/{visitId}/check-out")
    public ResponseEntity<VisitDetailResponse> checkOut(
            @PathVariable String visitId) {

        VisitDetailResponse response =
                visitService.checkOut(visitId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{visitId}/nda-status")
    public ResponseEntity<NdaStatusResponse> getNdaStatus(
            @PathVariable String visitId) {

        return ResponseEntity.ok(
                visitService.getNdaStatus(visitId)
        );
    }
}