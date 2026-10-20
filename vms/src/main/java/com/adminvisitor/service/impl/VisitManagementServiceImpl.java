
package com.adminvisitor.service.impl;

import com.adminvisitor.dto.requestdto.CheckInRequest;
import com.adminvisitor.dto.responsedto.*;
import com.adminvisitor.entity.*;
import com.adminvisitor.enums.*;
import com.adminvisitor.exception.BusinessRuleException;
import com.adminvisitor.mapper.VisitMapper;
import com.adminvisitor.repository.VendorRepository;
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
public class VisitManagementServiceImpl implements VisitManagementService {

    private static final List<VisitStatus> OPERATIONAL_STATUSES =
            List.of(VisitStatus.REGISTERED, VisitStatus.CHECKED_IN);

    private static final List<VisitStatus> HISTORICAL_STATUSES =
            List.of(
                    VisitStatus.CHECKED_OUT,
                    VisitStatus.CANCELLED,
                    VisitStatus.NO_SHOW
            );

    private static final DateTimeFormatter EMAIL_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final QrMicroserviceClient qrMicroserviceClient;
    private final VisitRepository visitRepository;
    private final VendorRepository vendorRepository;
    private final ProofSecurityService proofSecurityService;
    private final VisitMapper visitMapper;
    private final EmailService emailService;
    private final DocumentService documentService;
    private final VisitBadgeService visitBadgeService;
    private final NdaService ndaService;

    private double elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000.0;
    }

    private Specification<Visit> addSpecification(
            Specification<Visit> current,
            Specification<Visit> addition
    ) {
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
                        root.get("status").in(OPERATIONAL_STATUSES);

        specification = addSpecification(
                specification, VisitSpecification.hasView(view));
        specification = addSpecification(
                specification, VisitSpecification.hasVisitorId(visitorId));
        specification = addSpecification(
                specification, VisitSpecification.hasVisitorName(visitorName));
        specification = addSpecification(
                specification, VisitSpecification.hasVisitorEmail(visitorEmail));
        specification = addSpecification(
                specification, VisitSpecification.hasStatus(status));
        specification = addSpecification(
                specification, VisitSpecification.hasDate(date));
        specification = addSpecification(
                specification, VisitSpecification.hasFromDate(fromDate));
        specification = addSpecification(
                specification, VisitSpecification.hasToDate(toDate));

        Sort.Direction direction =
                "DESC".equalsIgnoreCase(sortDirection)
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;

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

        Sort sort = Sort.by(direction, sortField)
                .and(Sort.by(Sort.Direction.ASC, "id"));

        long queryStart = System.nanoTime();

        List<Visit> visits =
                visitRepository.findAll(specification, sort);

        double fetchMs = elapsedMillis(queryStart);

        long mappingStart = System.nanoTime();

        List<VisitDashboardResponse> responses =
                visits.stream()
                        .map(visitMapper::toDashboardResponse)
                        .toList();

        double mappingMs = elapsedMillis(mappingStart);

        log.debug(
                "VISIT_DASHBOARD_RETRIEVED visitCount={} fetchMs={} mappingMs={}",
                visits.size(),
                fetchMs,
                mappingMs
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
                        root.get("status").in(HISTORICAL_STATUSES);

        specification = addSpecification(
                specification, VisitSpecification.hasSearch(search));
        specification = addSpecification(
                specification, VisitSpecification.hasFromDate(fromDate));
        specification = addSpecification(
                specification, VisitSpecification.hasToDate(toDate));
        specification = addSpecification(
                specification, VisitSpecification.hasStatus(status));
        specification = addSpecification(
                specification, VisitSpecification.hasVisitorType(visitorType));
        specification = addSpecification(
                specification, VisitSpecification.hasHostId(hostId));

        Sort.Direction direction =
                "ASC".equalsIgnoreCase(sortDirection)
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;

        Sort sort = Sort.by(direction, "expectedArrivalAt")
                .and(Sort.by(Sort.Direction.ASC, "id"));

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Visit> visits =
                visitRepository.findAll(specification, pageable);

        return visits.map(visitMapper::toVisitorLogResponse);
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

        Document document = documentService
                .getLatestIdentityDocument(visit.getVisitor().getId());

        boolean existingVendor = false;
        boolean ndaAvailable = false;
        String ndaDocumentId = null;

        if (visit.getVisitorType() == VisitorType.VENDOR) {

            existingVendor = vendorRepository
                    .findByVisitorId(visit.getVisitor().getId())
                    .isPresent();

            ndaAvailable = !ndaService.isNdaRequired(
                    visit.getVisitor());

            if (ndaAvailable) {
                DocumentMetadata latestNda =
                        ndaService.getLatestNda(
                                visit.getVisitor().getId());

                ndaDocumentId = latestNda.getId();
            }
        }

        VisitDetailResponse.VisitorDetails visitorDetails =
                visitMapper.toVisitorDetails(
                        visit.getVisitor(),
                        document.getNationality() == null
                                ? null
                                : document.getNationality().name(),
                        ndaAvailable,
                        ndaDocumentId,
                        existingVendor
                );

        return visitMapper.toVisitDetailResponseWithVisitor(
                response,
                visitorDetails
        );
    }

    // VISIT DURATION
    public Long calculateDuration(Visit visit) {

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

    // VISIT CANCELLATION
    @Transactional
    public VisitDetailResponse cancelVisit(String visitId) {

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
                        + " " + cancelledVisit.getVisitor().getLastName(),
                cancelledVisit.getVisitor().getEmail(),
                cancelledVisit.getVisitReference(),
                cancelledVisit.getPurpose(),
                cancelledVisit.getExpectedArrivalAt()
                        .format(EMAIL_DATE_TIME_FORMATTER),
                cancelledVisit.getExpectedDepartureAt()
                        .format(EMAIL_DATE_TIME_FORMATTER),
                cancelledVisit.getHost().getFirstName()
                        + " " + cancelledVisit.getHost().getLastName()
        );

        emailService.sendVisitCancellationEmail(emailData);

        log.info(
                "VISIT_CANCELLED visitId={} visitReference={} visitorId={}",
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

        validateVisitorIdentity(visit.getVisitor(), request);

        if (visit.getVisitorType() == VisitorType.VENDOR) {
            boolean ndaRequired =
                    ndaService.isNdaRequired(visit.getVisitor());

            if (ndaRequired) {
                throw new BusinessRuleException(
                        "A valid NDA is required for this vendor before check-in"
                );
            }
        }

        DocumentMetadata visitorPhoto =
                documentService.uploadVisitorPhoto(
                        visit.getVisitor().getId(),
                        visit.getId(),
                        photo
                );

        LocalDateTime checkedInAt = LocalDateTime.now();

        visit.setCheckedInAt(checkedInAt);
        visit.setStatus(VisitStatus.CHECKED_IN);

        Visit checkedInVisit = visitRepository.save(visit);

        String qrToken = qrMicroserviceClient.generateToken();

        VisitBadge badge = visitBadgeService.createBadge(
                checkedInVisit,
                qrToken
        );

        String qrCode = qrMicroserviceClient.generateQrCode(
                badge.getQrContainingToken()
        );

        BadgeEmailData emailData = new BadgeEmailData(
                checkedInVisit.getVisitor().getFirstName()
                        + " " + checkedInVisit.getVisitor().getLastName(),
                checkedInVisit.getVisitor().getEmail(),
                checkedInVisit.getVisitReference(),
                checkedInVisit.getHost().getFirstName()
                        + " " + checkedInVisit.getHost().getLastName(),
                checkedInVisit.getHost().getEmail(),
                badge.getIssuedAt().format(EMAIL_DATE_TIME_FORMATTER),
                badge.getValidUntil().format(EMAIL_DATE_TIME_FORMATTER),
                qrCode,
                visitorPhoto.getDocumentPath()
        );

        emailService.sendVisitBadgeEmail(emailData);

        HostCheckInEmailData hostEmailData =
                new HostCheckInEmailData(
                        checkedInVisit.getHost().getEmail(),
                        checkedInVisit.getHost().getFirstName()
                                + " " + checkedInVisit.getHost().getLastName(),
                        checkedInVisit.getVisitor().getFirstName()
                                + " " + checkedInVisit.getVisitor().getLastName(),
                        checkedInVisit.getVisitorType().name(),
                        checkedInVisit.getVisitor().getCompanyName(),
                        checkedInVisit.getVisitReference(),
                        checkedInVisit.getPurpose(),
                        checkedInAt.format(EMAIL_DATE_TIME_FORMATTER)
                );

        emailService.sendHostCheckInNotification(hostEmailData);

        log.info(
                "VISIT_CHECKED_IN visitId={} visitReference={} visitorId={} checkedInAt={}",
                checkedInVisit.getId(),
                checkedInVisit.getVisitReference(),
                checkedInVisit.getVisitor().getId(),
                checkedInVisit.getCheckedInAt()
        );

        return visitMapper.toVisitDetailResponse(checkedInVisit);
    }

    // CHECK-OUT
    @Transactional
    public VisitDetailResponse checkOut(String visitId) {

        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() ->
                        new BusinessRuleException(
                                "Visit not found with id: " + visitId
                        )
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
                "VISIT_CHECKED_OUT visitId={} visitReference={} visitorId={} checkedOutAt={}",
                checkedOutVisit.getId(),
                checkedOutVisit.getVisitReference(),
                checkedOutVisit.getVisitor().getId(),
                checkedOutVisit.getCheckedOutAt()
        );

        return visitMapper.toVisitDetailResponse(checkedOutVisit);
    }

    // IDENTITY VERIFICATION
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

        log.info(
                "VISITOR_IDENTITY_VERIFIED visitId={}",
                visitId
        );
    }

    // IDENTITY VALIDATION
    private void validateVisitorIdentity(
            Visitor visitor,
            CheckInRequest request
    ) {

        Document identityDocument =
                documentService.getLatestIdentityDocument(visitor.getId());

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
                boolean aadhaarMatches =
                        documentService.verifyIdentityProof(
                                visitor,
                                ProofType.AADHAAR,
                                request.aadharNumber()
                        );

                boolean panMatches =
                        documentService.verifyIdentityProof(
                                visitor,
                                ProofType.PAN,
                                request.panNumber()
                        );

                identityMatches = aadhaarMatches && panMatches;
            }

            case INTERNATIONAL ->
                    identityMatches =
                            documentService.verifyIdentityProof(
                                    visitor,
                                    ProofType.PASSPORT,
                                    request.passportNumber()
                            );

            default -> throw new BusinessRuleException(
                    "Unsupported nationality for identity verification"
            );
        }

        if (!identityMatches) {
            throw new BusinessRuleException(
                    "Identity verification failed"
            );
        }
    }
}
