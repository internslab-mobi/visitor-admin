package com.adminvisitor.controller;

import com.adminvisitor.dto.responsedto.QrValidationResponse;
import com.adminvisitor.entity.Visit;
import com.adminvisitor.entity.VisitBadge;
import com.adminvisitor.enums.BadgeStatus;
import com.adminvisitor.repository.VisitRepository;
import com.adminvisitor.service.EmailService;
import com.adminvisitor.service.QrCodeService;
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
    private final QrCodeService qrCodeService;
    private final EmailService emailService;

    @PostMapping("/{visitId}")
    public ResponseEntity<Map<String, Object>> createTestBadge(
            @PathVariable String visitId
    ) {

        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Visit not found with id: " + visitId
                        )
                );

        VisitBadge badge =
                visitBadgeService.createBadge(visit);

        String qrCode =
                qrCodeService.generateQrCode(
                        badge.getQrContainingToken()
                );

        emailService.sendVisitBadgeEmail(
                visit,
                badge,
                qrCode,
                null //That is our temporary visitor-photo value.
        );
        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("badgeId", badge.getId());
        response.put("visitorId", badge.getVisitor().getId());
        response.put("visitId", badge.getVisit().getId());
        response.put(
                "qrContainingToken",
                badge.getQrContainingToken()
        );
        response.put("qrCode", qrCode);
        response.put("issuedAt", badge.getIssuedAt());
        response.put("validFrom", badge.getValidFrom());
        response.put("validUntil", badge.getValidUntil());
        response.put("status", badge.getStatus());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate")
    public ResponseEntity<QrValidationResponse> validateQr(
            @RequestParam String token
    ) {

        try {

            BadgeStatus status =
                    visitBadgeService.validateQrToken(token);

            if (status == BadgeStatus.ACTIVE) {

                return ResponseEntity.ok(
                        new QrValidationResponse(
                                BadgeStatus.ACTIVE,
                                "QR code is valid"
                        )
                );
            }

            return ResponseEntity.ok(
                    new QrValidationResponse(
                            BadgeStatus.INVALID,
                            "QR code is no longer valid"
                    )
            );

        } catch (IllegalArgumentException exception) {

            return ResponseEntity.ok(
                    new QrValidationResponse(
                            BadgeStatus.INVALID,
                            "Invalid QR code"
                    )
            );
        }
    }
}