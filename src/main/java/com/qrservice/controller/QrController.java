package com.qrservice.controller;

import com.qrservice.dto.CheckoutBadgeInvalidationRequest;
import com.qrservice.dto.QrBadgeRequest;
import com.qrservice.dto.QrBadgeResponse;
import com.qrservice.dto.QrScanResponse;
import com.qrservice.service.QrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller responsible for QR badge generation, QR code scanning,
 * and badge invalidation operations.
 *
 * <p>This controller delegates QR-related business logic to {@link QrService}
 * and exposes endpoints for communication with the VMS application.</p>
 *
 * <p>Base URL: /api/qr</p>
 *  @author Anisha Z
 */
@Slf4j
@RestController
@RequestMapping("/api/qr")
@RequiredArgsConstructor

@Tag(
        name = "QR Management",
        description = "APIs for generating QR badges, processing QR scans, and invalidating badges"
)
public class QrController {

    private final QrService qrService;

    /**
     * Generates a QR badge for a visitor's registered visit.
     *
     * <p>The request contains the visit details required to generate
     * the QR badge. The request body is validated before processing.</p>
     *
     * @param request the request containing the visit details required
     *                for QR badge generation
     * @return {@link ResponseEntity} containing the generated QR badge
     *         details with HTTP 200 OK
     * @throws jakarta.validation.ConstraintViolationException if
     *         constraint validation fails at the method level
     */



    @PostMapping(
            value = "/generate-badge",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Generate a QR badge",
            description = "Generates a QR badge for a registered visitor visit using the supplied visit details. The request body is validated before processing."
    )
    public ResponseEntity<QrBadgeResponse> generateQrBadge(

            @Parameter(
                    description = "Request containing the visit details required to generate the QR badge",
                    required = true
            )
            @Valid @RequestBody QrBadgeRequest request) {

        return ResponseEntity.ok(
                qrService.generateQrBadge(request)
        );
    }

/**
 * Processes a visitor's QR code scan.
 *
 * <p>The scanned token is received as a request parameter and passed
 * to the service layer for validation and scan processing.</p>
 *
 * <p>The service determines the appropriate scan action, such as
 * recording an entry or exit, according to the application's
 * QR scan business rules.</p>
 *
 * @param token the QR badge token extracted from the scanned QR code
 * @return {@link ResponseEntity} containing the QR scan result,
 *         including the action performed and scan details,
 *         with HTTP 200 OK
 */
@PostMapping(
        value = "/process-scan",
        produces = MediaType.APPLICATION_JSON_VALUE
)
@Operation(
        summary = "Process a QR scan",
        description = "Validates the supplied QR badge token and processes the scan according to the application's entry and exit tracking rules."
)

public ResponseEntity<QrScanResponse> processScan(
        @Parameter(
                name = "token",
                description = "QR badge token extracted from the scanned QR code",
                required = true,
                example = "sample-qr-token"
        )
        @RequestParam("token") String token
) {
        log.info(
                "QR scan input: length={}, startsWithTokenPrefix={}, containsEquals={}",
                token == null ? 0 : token.length(),
                token != null && token.startsWith("http"),
                token != null && token.contains("=")
        );

        return ResponseEntity.ok(
                qrService.processScan(token)
        );
    }

    /**
     * Invalidates a visitor's QR badge when the visit is checked out.
     *
     * <p>The request contains the visit information required to identify
     * the badge that must be invalidated.</p>
     *
     * @param request the request containing the details required
     *                to invalidate the QR badge
     * @return an empty response with HTTP 204 No Content when
     *         the invalidation operation completes successfully
     */

    @PostMapping(
            value = "/invalidate-badge",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Invalidate a QR badge",
            description = "Invalidates the visitor's QR badge during checkout so that the badge can no longer be used for subsequent scans."
    )
    public ResponseEntity<Void> invalidateBadgeOnCheckout(

            @Parameter(
                    description = "Request containing the visit and badge details required for invalidation",
                    required = true
            )
            @RequestBody CheckoutBadgeInvalidationRequest request
    ) {
        qrService.invalidateBadgeOnCheckout(request);
        return ResponseEntity.noContent().build();
    }

}