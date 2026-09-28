package com.adminvisitor.controller;

import com.adminvisitor.dto.requestdto.CheckInRequest;
import com.adminvisitor.dto.requestdto.RegistrationRequest;
import com.adminvisitor.dto.responsedto.NdaStatusResponse;
import com.adminvisitor.dto.responsedto.RegistrationResponse;
import com.adminvisitor.dto.responsedto.VisitDashboardResponse;
import com.adminvisitor.dto.responsedto.VisitDetailResponse;
import com.adminvisitor.service.VisitService;
import com.adminvisitor.enums.VisitStatus;
import com.adminvisitor.enums.VisitView;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.media.Content;
import org.springframework.http.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;

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

    @PatchMapping(
            value = "/{visitId}/check-in",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<VisitDetailResponse> checkIn(
            @PathVariable String visitId,

            @RequestPart("request")
            @Parameter(
                    description = "Check-in identity verification data",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = CheckInRequest.class
                            )
                    )
            )
            @Valid CheckInRequest request,

            @RequestPart("photo")
            MultipartFile photo
    ) {
        return ResponseEntity.ok(
                visitService.checkIn(
                        visitId,
                        request,
                        photo
                )
        );
    }

    @PatchMapping("/{visitId}/check-out")
    public ResponseEntity<VisitDetailResponse> checkOut(
            @PathVariable String visitId) {

        VisitDetailResponse response =
                visitService.checkOut(visitId);

        return ResponseEntity.ok(response);
    }

//    @GetMapping("/{visitId}/nda-status")
//    public ResponseEntity<NdaStatusResponse> getNdaStatus(
//            @PathVariable String visitId) {
//
//        return ResponseEntity.ok(
//                visitService.getNdaStatus(visitId)
//        );
//    }


}