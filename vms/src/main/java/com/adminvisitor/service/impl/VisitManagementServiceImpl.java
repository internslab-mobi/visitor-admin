package com.adminvisitor.service.impl;

import com.adminvisitor.dto.requestdto.CheckInRequest;
import com.adminvisitor.dto.responsedto.*;
import com.adminvisitor.entity.*;
import com.adminvisitor.enums.*;
import com.adminvisitor.exception.BusinessRuleException;
import com.adminvisitor.mapper.VisitMapper;
import com.adminvisitor.repository.VisitRepository;
import com.adminvisitor.service.*;
import com.adminvisitor.specification.VisitSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisitManagementServiceImpl{

    private static final DateTimeFormatter EMAIL_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final VisitRepository visitRepository;
    private final ProofSecurityService proofSecurityService;
    private final VisitMapper visitMapper;
    private final EmailService emailService;
    private final DocumentService documentService;
    private final VisitBadgeService visitBadgeService;
    private final QrTokenService qrTokenService;

    private double elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000.0;
    }

    private Specification<Visit> addSpecification(
            Specification<Visit> current,
            Specification<Visit> addition) {

        return addition == null ? current : current.and(addition);
    }

    // VISITS DASHBOARD
    @Transactional(readOnly = true)
    public List<VisitDashboardResponse> getDashboardVisits(
            VisitView view,
            String visitorId,
            String visitorName,
            String visitorEmail,
            VisitStatus status,
            LocalDate date,
            LocalDate fromDate,
            LocalDate toDate,
            String sortDirection,
            String sortBy
    ) {

        Specification<Visit> specification =
                (root, query, criteriaBuilder) ->
                        criteriaBuilder.conjunction();

        specification = addSpecification(
                specification,
                VisitSpecification.hasView(view)
        );

        specification = addSpecification(
                specification,
                VisitSpecification.hasVisitorId(visitorId)
        );

        specification = addSpecification(
                specification,
                VisitSpecification.hasVisitorName(visitorName)
        );

        specification = addSpecification(
                specification,
                VisitSpecification.hasVisitorEmail(visitorEmail)
        );

        specification = addSpecification(
                specification,
                VisitSpecification.hasStatus(status)
        );

        specification = addSpecification(
                specification,
                VisitSpecification.hasDate(date)
        );

        specification = addSpecification(
                specification,
                VisitSpecification.hasFromDate(fromDate)
        );

        specification = addSpecification(
                specification,
                VisitSpecification.hasToDate(toDate)
        );

        Sort.Direction direction =
                "DESC".equalsIgnoreCase(sortDirection)
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;

        // Only supported fields are accepted for sorting.
        String sortField = switch (
                sortBy == null ? "" : sortBy.trim()
                ) {
            case "visitDate" -> "expectedArrivalAt";
            case "status" -> "status";
            case "visitorName" -> "visitor.firstName";
            case "visitorId" -> "visitor.id";
            case "visitorEmail" -> "visitor.email";
            case "visitorType" -> "visitorType";
            case "visitReference" -> "visitReference";
            default -> "expectedArrivalAt";
        };

        // Use visit ID as a stable tie-breaker for equal sort values.
        Sort sort = Sort.by(direction, sortField)
                .and(Sort.by(Sort.Direction.ASC, "id"));

        long queryStart = System.nanoTime();

        List<Visit> visits =
                visitRepository.findAll(specification, sort);

        long queryEnd = System.nanoTime();

        List<VisitDashboardResponse> responses =
                visits.stream()
                        .map(visitMapper::toDashboardResponse)
                        .toList();

        long mappingEnd = System.nanoTime();

        log.info(
                "Dashboard visits performance: visitCount={}, fetchMs={}, mappingMs={}",
                visits.size(),
                (queryEnd - queryStart) / 1_000_000.0,
                (mappingEnd - queryEnd) / 1_000_000.0
        );

        return responses;
    }

    // VISITOR LOG
    @Transactional(readOnly = true)
    public Page<VisitorLogResponse> getVisitorLog(
            String search,
            LocalDate fromDate,
            LocalDate toDate,
            VisitStatus status,
            VisitorType visitorType,
            String hostId,
            int page,
            int size,
            String sortDirection
    ) {
        long startNanos = System.nanoTime();

        try {
            if (page < 0) {
                throw new BusinessRuleException(
                        "Page number cannot be negative"
                );
            }

            if (size < 1 || size > 100) {
                throw new BusinessRuleException(
                        "Page size must be between 1 and 100"
                );
            }

            Specification<Visit> specification =
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.conjunction();

            specification = addSpecification(
                    specification,
                    VisitSpecification.hasSearch(search)
            );

            specification = addSpecification(
                    specification,
                    VisitSpecification.hasFromDate(fromDate)
            );

            specification = addSpecification(
                    specification,
                    VisitSpecification.hasToDate(toDate)
            );

            specification = addSpecification(
                    specification,
                    VisitSpecification.hasStatus(status)
            );

            specification = addSpecification(
                    specification,
                    VisitSpecification.hasVisitorType(visitorType)
            );

            specification = addSpecification(
                    specification,
                    VisitSpecification.hasHostId(hostId)
            );

            Sort.Direction direction =
                    "ASC".equalsIgnoreCase(sortDirection)
                            ? Sort.Direction.ASC
                            : Sort.Direction.DESC;

            // Stable ordering prevents records with equal arrival times
            // from changing order arbitrarily between page requests.
            Sort sort = Sort.by(direction, "expectedArrivalAt")
                    .and(Sort.by(Sort.Direction.ASC, "id"));

            Pageable pageable = PageRequest.of(page, size, sort);

            Page<Visit> visits =
                    visitRepository.findAll(specification, pageable);

            return visits.map(visitMapper::toVisitorLogResponse);

        } catch (RuntimeException ex) {
            log.error(
                    "Visitor log retrieval failed. page={}, size={}, elapsedMs={}",
                    page,
                    size,
                    elapsedMillis(startNanos),
                    ex
            );
            throw ex;

        } finally {
            log.info(
                    "Visitor log execution finished. page={}, size={}, elapsedMs={}",
                    page,
                    size,
                    elapsedMillis(startNanos)
            );
        }
    }

    // VISIT DETAILS
    @Transactional(readOnly = true)
    public VisitDetailResponse getVisitDetails(String visitId) {

        Visit visit = visitRepository.findDetailsById(visitId)
                .orElseThrow(() ->
                        new BusinessRuleException(
                                "Visit not found with id: " + visitId
                        )
                );

        VisitDetailResponse response =
                visitMapper.toVisitDetailResponse(visit);

        return VisitDetailResponse.builder()
                .visitId(response.visitId())
                .visitReference(response.visitReference())
                .visitor(response.visitor())
                .visitorType(response.visitorType())
                .registrationType(response.registrationType())
                .purpose(response.purpose())
                .host(response.host())
                .expectedArrivalAt(response.expectedArrivalAt())
                .expectedDepartureAt(response.expectedDepartureAt())
                .checkedInAt(response.checkedInAt())
                .checkedOutAt(response.checkedOutAt())
                .totalDurationSeconds(calculateDuration(visit))
                .remarks(response.remarks())
                .status(response.status())
                .audit(response.audit())
                .build();
    }

    private Long calculateDuration(Visit visit) {
        if (visit == null
                || visit.getCheckedInAt() == null
                || visit.getCheckedOutAt() == null) {
            return null;
        }

        return Duration.between(
                visit.getCheckedInAt(),
                visit.getCheckedOutAt()
        ).getSeconds();
    }

    // NDA STATUS
    @Transactional(readOnly = true)
    public NdaStatusResponse getNdaStatus(String visitId) {

        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() ->
                        new BusinessRuleException(
                                "Visit not found with id: " + visitId
                        )
                );

        Visitor visitor = visit.getVisitor();

        boolean ndaRequired =
                visit.getVisitorType() == VisitorType.VENDOR
                        && documentService.isNdaRequired(visitor);

        boolean ndaAvailable =
                visit.getVisitorType() == VisitorType.VENDOR
                        && !ndaRequired;

        DocumentMetadata latestNda = null;

        if (ndaAvailable) {
            latestNda = documentService.getLatestNda(visitor.getId());
        }

        return NdaStatusResponse.builder()
                .visitorId(visitor.getId())
                .visitorType(visit.getVisitorType().name())
                .ndaRequired(ndaRequired)
                .ndaAvailable(ndaAvailable)
                .validUntil(latestNda != null ? latestNda.getValidUntil() : null)
                .documentId(latestNda != null ? latestNda.getId() : null)
                .build();

    }

    // CANCEL VISIT
    @Transactional
    public VisitDetailResponse cancelVisit(String visitId) {

        log.info("Cancellation started. visitId={}", visitId);

        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() ->
                        new BusinessRuleException(
                                "Visit not found with id: " + visitId
                        )
                );

        if (visit.getStatus() != VisitStatus.REGISTERED) {
            throw new BusinessRuleException(
                    "Only a registered visit can be cancelled"
            );
        }

        visit.setStatus(VisitStatus.CANCELLED);

        Visit cancelledVisit = visitRepository.save(visit);

        CancellationEmailData emailData = new CancellationEmailData(
                cancelledVisit.getVisitor().getFirstName()
                        + " "
                        + cancelledVisit.getVisitor().getLastName(),
                cancelledVisit.getVisitor().getEmail(),
                cancelledVisit.getVisitReference(),
                cancelledVisit.getPurpose(),
                cancelledVisit.getExpectedArrivalAt()
                        .format(EMAIL_DATE_TIME_FORMATTER),
                cancelledVisit.getExpectedDepartureAt()
                        .format(EMAIL_DATE_TIME_FORMATTER),
                cancelledVisit.getHost().getFirstName()
                        + " "
                        + cancelledVisit.getHost().getLastName()
        );

        emailService.sendVisitCancellationEmail(emailData);

        log.info(
                "Visit cancelled successfully. visitId={}, visitReference={}, visitorId={}",
                cancelledVisit.getId(),
                cancelledVisit.getVisitReference(),
                cancelledVisit.getVisitor().getId()
        );

        return visitMapper.toVisitDetailResponse(cancelledVisit);
    }

    // CHECK-IN
    @Transactional
    public VisitDetailResponse checkIn(
            String visitId,
            CheckInRequest request,
            MultipartFile photo
    ) {
        long startNanos = System.nanoTime();

        try {
            Visit visit = visitRepository.findById(visitId)
                    .orElseThrow(() ->
                            new BusinessRuleException(
                                    "Visit not found with id: " + visitId
                            )
                    );

            if (visit.getStatus() != VisitStatus.REGISTERED) {
                throw new BusinessRuleException(
                        "Only a registered visit can be checked in"
                );
            }

            if (!visit.getExpectedArrivalAt()
                    .toLocalDate()
                    .equals(LocalDate.now())) {
                throw new BusinessRuleException(
                        "Visit can only be checked in on the scheduled visit date"
                );
            }

            // Validate the visitor's submitted identity.
            validateVisitorIdentity(visit.getVisitor(), request);

            if (visit.getVisitorType() == VisitorType.VENDOR) {
                boolean ndaRequired =
                        documentService.isNdaRequired(visit.getVisitor());

                if (ndaRequired) {
                    throw new BusinessRuleException(
                            "A valid NDA is required for this vendor before check-in"
                    );
                }
            }

            DocumentMetadata visitorPhoto =
                    documentService.uploadVisitorPhoto(
                            visit.getVisitor().getId(),
                            photo
                    );

            // Capture the timestamp once and reuse it consistently.
            LocalDateTime checkedInAt = LocalDateTime.now();

            visit.setCheckedInAt(checkedInAt);
            visit.setStatus(VisitStatus.CHECKED_IN);

            Visit checkedInVisit = visitRepository.save(visit);

            // Create the badge and QR code after check-in.
            VisitBadge badge =
                    visitBadgeService.createBadge(checkedInVisit);

            String qrCode =
                    qrTokenService.generateQrCode(
                            badge.getQrContainingToken()
                    );

            log.info(
                    "QR code generated successfully. visitId={}, qrLength={}",
                    checkedInVisit.getId(),
                    qrCode.length()
            );

            BadgeEmailData emailData = new BadgeEmailData(
                    checkedInVisit.getVisitor().getFirstName()
                            + " "
                            + checkedInVisit.getVisitor().getLastName(),
                    checkedInVisit.getVisitor().getEmail(),
                    checkedInVisit.getVisitReference(),
                    checkedInVisit.getHost().getFirstName()
                            + " "
                            + checkedInVisit.getHost().getLastName(),
                    checkedInVisit.getHost().getEmail(),
                    badge.getIssuedAt().format(EMAIL_DATE_TIME_FORMATTER),
                    badge.getValidUntil().format(EMAIL_DATE_TIME_FORMATTER),
                    qrCode,
                    visitorPhoto.getDocumentPath()
            );

            log.info(
                    "Visitor badge created successfully. visitId={}, badgeId={}",
                    checkedInVisit.getId(),
                    badge.getId()
            );

            emailService.sendVisitBadgeEmail(emailData);

            HostCheckInEmailData hostEmailData =
                    new HostCheckInEmailData(
                            checkedInVisit.getHost().getEmail(),
                            checkedInVisit.getHost().getFirstName()
                                    + " "
                                    + checkedInVisit.getHost().getLastName(),
                            checkedInVisit.getVisitor().getFirstName()
                                    + " "
                                    + checkedInVisit.getVisitor().getLastName(),
                            checkedInVisit.getVisitorType().name(),
                            checkedInVisit.getVisitor().getCompanyName(),
                            checkedInVisit.getVisitReference(),
                            checkedInVisit.getPurpose(),
                            checkedInAt.format(EMAIL_DATE_TIME_FORMATTER)
                    );

            emailService.sendHostCheckInNotification(hostEmailData);

            log.info(
                    "Visitor checked in successfully. visitId={}, visitReference={}, visitorId={}, checkedInAt={}",
                    checkedInVisit.getId(),
                    checkedInVisit.getVisitReference(),
                    checkedInVisit.getVisitor().getId(),
                    checkedInVisit.getCheckedInAt()
            );

            return visitMapper.toVisitDetailResponse(checkedInVisit);

        } catch (RuntimeException ex) {
            log.error(
                    "Check-in failed. visitId={}, elapsedMs={}",
                    visitId,
                    elapsedMillis(startNanos),
                    ex
            );
            throw ex;

        } finally {
            log.info(
                    "Check-in execution finished. visitId={}, elapsedMs={}",
                    visitId,
                    elapsedMillis(startNanos)
            );
        }
    }

    // CHECK-OUT
    @Transactional
    public VisitDetailResponse checkOut(String visitId) {

        log.info("Checkout started. visitId={}", visitId);

        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() ->
                        new BusinessRuleException(
                                "Visit not found with id: " + visitId
                        )
                );

        log.info(
                "Visit loaded. visitId={}, status={}",
                visit.getId(),
                visit.getStatus()
        );

        if (visit.getStatus() != VisitStatus.CHECKED_IN) {
            throw new BusinessRuleException(
                    "Only a checked-in visit can be checked out"
            );
        }

        LocalDateTime checkedOutAt = LocalDateTime.now();

        visit.setCheckedOutAt(checkedOutAt);
        visit.setStatus(VisitStatus.CHECKED_OUT);

        Visit checkedOutVisit = visitRepository.save(visit);

        visitBadgeService.invalidateBadgeOnCheckout(
                visitId,
                checkedOutAt
        );

        log.info(
                "Visitor checked out successfully. visitId={}, visitReference={}, visitorId={}, checkedOutAt={}",
                checkedOutVisit.getId(),
                checkedOutVisit.getVisitReference(),
                checkedOutVisit.getVisitor().getId(),
                checkedOutVisit.getCheckedOutAt()
        );

        return visitMapper.toVisitDetailResponse(checkedOutVisit);
    }

    @Transactional(readOnly = true)
    public void verifyIdentity(String visitId, CheckInRequest request) {
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() ->
                        new BusinessRuleException(
                                "Visit not found with id: " + visitId
                        )
                );

        if (visit.getStatus() != VisitStatus.REGISTERED) {
            throw new BusinessRuleException(
                    "Only a registered visit can have its identity verified"
            );
        }

        validateVisitorIdentity(visit.getVisitor(), request);

        log.info("Visitor identity verified successfully. visitId={}", visitId);
    }

    // IDENTITY VALIDATION
    private void validateVisitorIdentity(
            Visitor visitor,
            CheckInRequest request
    ) {

        Document identityDocument = documentService.getLatestIdentityDocument(visitor.getId());
        Nationality nationality = identityDocument.getNationality();

        proofSecurityService.validate(
                nationality,
                request.aadharNumber(),
                request.panNumber(),
                request.passportNumber()
        );
        boolean identityMatches;

        switch (nationality) {
            case DOMESTIC -> {
                boolean aadhaarMatches = documentService.verifyIdentityProof(
                                visitor,
                                ProofType.AADHAAR,
                                request.aadharNumber());

                boolean panMatches = documentService.verifyIdentityProof(
                                visitor,
                                ProofType.PAN,
                                request.panNumber());
                identityMatches = aadhaarMatches && panMatches;
            }

            case INTERNATIONAL ->
                    identityMatches = documentService.verifyIdentityProof(
                                    visitor,
                                    ProofType.PASSPORT,
                                    request.passportNumber());

            default -> throw new BusinessRuleException(
                    "Unsupported nationality for identity verification");
        }

        if (!identityMatches) {
            throw new BusinessRuleException("Identity verification failed");
        }
    }
}