package com.adminvisitor.controller;

import com.adminvisitor.dto.responsedto.QrValidationResponse;
import com.adminvisitor.entity.Visit;
import com.adminvisitor.entity.VisitBadge;
import com.adminvisitor.enums.BadgeStatus;
import com.adminvisitor.exception.ResourceNotFoundException;
import com.adminvisitor.repository.VisitRepository;
import com.adminvisitor.service.QrTokenService;
import com.adminvisitor.service.VisitBadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/visit-badges")
@RequiredArgsConstructor
public class VisitBadgeController {

    private final VisitRepository visitRepository;
    private final VisitBadgeService visitBadgeService;
    private final QrTokenService qrTokenService;

    @PostMapping("/{visitId}")
    public ResponseEntity<Map<String, Object>> createTestBadge(
            @PathVariable String visitId
    ) {

        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Visit not found with id: " + visitId
                        )
                );

        VisitBadge badge =
                visitBadgeService.createBadge(visit);

        String qrCode =
                qrTokenService.generateQrCode(
                        badge.getQrContainingToken()
                );


        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("badgeId", badge.getId());

        response.put("visitId", badge.getVisit().getId());
        response.put(
                "qrContainingToken",
                badge.getQrContainingToken()
        );
        response.put("qrCode", qrCode);
        response.put("issuedAt", badge.getIssuedAt());
        response.put("validUntil", badge.getValidUntil());
        response.put("status", badge.getStatus());

        return ResponseEntity.ok(response);
    }


    @GetMapping("/validate")
    public ResponseEntity<QrValidationResponse> validateQr(
            @RequestParam String token
    ) {
        try {
            VisitBadge badge =
                    visitBadgeService.validateQrToken(token);

            Visit visit = badge.getVisit();

            QrValidationResponse response =
                    QrValidationResponse.builder()
                            .status(BadgeStatus.ACTIVE)
                            .message("QR code is valid")
                            .visitorName(
                                    visit.getVisitor().getFirstName() + " " +
                                            visit.getVisitor().getLastName()
                            )
                            .visitReference(visit.getVisitReference())
                            .hostName(
                                    visit.getHost().getFirstName() + " " +
                                            visit.getHost().getLastName()
                            )
                            .issuedAt(badge.getIssuedAt())
                            .validUntil(badge.getValidUntil())
                            .build();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException exception) {

            QrValidationResponse response =
                    QrValidationResponse.builder()
                            .status(BadgeStatus.INVALID)
                            .message("QR code is invalid or expired")
                            .build();

            return ResponseEntity.ok(response);
        }
    }


}