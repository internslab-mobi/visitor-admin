package com.adminvisitor.service;

import com.adminvisitor.dto.responsedto.*;
import com.adminvisitor.dto.requestdto.RegistrationRequest;
import com.adminvisitor.dto.requestdto.CheckInRequest;
import com.adminvisitor.dto.responsedto.HostCheckInEmailData;
import com.adminvisitor.entity.*;
import com.adminvisitor.enums.*;
import com.adminvisitor.exception.BusinessRuleException;
import com.adminvisitor.exception.EmailAlreadyExistsException;
import com.adminvisitor.exception.MobileNumberAlreadyExistsException;
import com.adminvisitor.exception.ResourceNotFoundException;
import com.adminvisitor.repository.EmployeeRepository;
import com.adminvisitor.repository.VendorRepository;
import com.adminvisitor.repository.VisitRepository;
import com.adminvisitor.repository.VisitorRepository;
import com.adminvisitor.specification.VisitSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisitService {

    private final VisitRepository visitRepository;
    private final VisitorRepository visitorRepository;
    private final EmailService emailService;
    private final IdGeneratorService idGeneratorService;
    private final EmployeeRepository employeeRepository;
    private final VisitBadgeService visitBadgeService;
    private final QrCodeService qrCodeService;
    private final VendorRepository vendorRepository;

    private final ProofValidationService proofValidationService;
    private final BlacklistService blacklistService;

    /*
     * DocumentService is still required for identity-proof handling.
     *
     * The active saveIdentityProofs() method is used during registration
     * to store HMAC-SHA-256 blind indexes.
     *
     * NDA-related methods inside DocumentService are temporarily disabled.
     */
    private final DocumentService documentService;


    // ============================================================
    // VISIT REGISTRATION
    // ============================================================

    @Transactional
    public RegistrationResponse register(RegistrationRequest request) {

        log.debug(
                "Starting visit registration. registrationType={}, visitorType={}, hostId={}",
                request.registrationType(),
                request.visitorType(),
                request.hostId()
        );

        // 1. Validate ID proof and check blacklist
        validateProofAndBlacklist(request);

        // 2. Validate visit timing
        long start = System.currentTimeMillis();

        validateVisitTiming(request);

        log.debug(
                "Timing: validateVisitTiming={} ms",
                System.currentTimeMillis() - start
        );

        // 2. Find existing visitor or create a new visitor
        start = System.currentTimeMillis();

        Visitor visitor = findOrCreateVisitor(request);

        /*
         * Save identity proofs.
         *
         * The raw proof values are converted into HMAC-SHA-256
         * blind indexes inside DocumentService.
         *
         * No raw Aadhaar/PAN/Passport value is stored.
         */
        documentService.saveIdentityProofs(
                visitor,
                request.nationality(),
                request.aadharNumber(),
                request.panNumber(),
                request.passportNumber()
        );

        log.debug(
                "Timing: findOrCreateVisitor={} ms",
                System.currentTimeMillis() - start
        );

        createVendorIfRequired(visitor, request);

        // 3. Create Visit
        Visit visit = new Visit();

        // 4. Generate Visit ID
        start = System.currentTimeMillis();

        visit.setId(
                idGeneratorService.generateId("VISIT", "VIS")
        );

        log.debug(
                "Timing: generateVisitId={} ms",
                System.currentTimeMillis() - start
        );

        visit.setVisitReference(generateVisitReference());

        visit.setVisitor(visitor);

        visit.setVisitorType(request.visitorType());

        visit.setRegistrationType(request.registrationType());

        visit.setPurpose(request.purpose());

        Employee employee = employeeRepository.findById(request.hostId())
                .orElseThrow(() ->
                        new BusinessRuleException(
                                "Employee not found with id: " + request.hostId()
                        )
                );

        if (!"ACTIVE".equalsIgnoreCase(employee.getStatus())) {
            throw new BusinessRuleException(
                    "Selected employee is not active"
            );
        }

        visit.setHost(employee);

        LocalDateTime expectedArrivalAt =
                LocalDateTime.of(
                        request.visitDate(),
                        request.expectedArrivalTime()
                );

        LocalDateTime expectedDepartureAt =
                LocalDateTime.of(
                        request.visitDate(),
                        request.expectedDepartureTime()
                );

        visit.setExpectedArrivalAt(expectedArrivalAt);
        visit.setExpectedDepartureAt(expectedDepartureAt);

        visit.setRemarks(request.remarks());

        List<VisitStatus> activeStatuses = List.of(
                VisitStatus.REGISTERED,
                VisitStatus.CHECKED_IN
        );

        List<Visit> overlappingVisits = visitRepository.findOverlappingVisits(
                visitor.getId(),
                expectedArrivalAt,
                expectedDepartureAt,
                activeStatuses
        );

        if (!overlappingVisits.isEmpty()) {
            throw new BusinessRuleException(
                    "Visitor already has an active visit overlapping the requested time"
            );
        }

        /*
         * ID proof is optional for now.
         * Actual verification will happen during check-in.
         */

        visit.setStatus(VisitStatus.REGISTERED);

        // 5. Save Visit
        start = System.currentTimeMillis();

        Visit savedVisit = visitRepository.save(visit);

        log.debug(
                "Timing: visitRepository.save={} ms",
                System.currentTimeMillis() - start
        );

        long emailStart = System.currentTimeMillis();

        emailService.sendVisitConfirmationEmail(savedVisit);

        log.debug(
                "Timing: sendVisitConfirmationEmail={} ms",
                System.currentTimeMillis() - emailStart
        );

        log.info(
                "Visit registered successfully. visitId={}, visitReference={}, visitorId={}, registrationType={}, status={}",
                savedVisit.getId(),
                savedVisit.getVisitReference(),
                visitor.getId(),
                savedVisit.getRegistrationType(),
                savedVisit.getStatus()
        );

        // 6. Build response
        return new RegistrationResponse(
                savedVisit.getId(),
                savedVisit.getVisitReference(),
                visitor.getId(),
                visitor.getFirstName(),
                visitor.getLastName(),
                visitor.getEmail(),
                visitor.getMobileNumber(),
                visitor.getCompanyName(),
                savedVisit.getVisitorType(),
                savedVisit.getRegistrationType(),
                savedVisit.getPurpose(),
                savedVisit.getHost().getId(),
                savedVisit.getExpectedArrivalAt(),
                savedVisit.getExpectedDepartureAt(),
                savedVisit.getRemarks(),
                savedVisit.getStatus(),
                getSuccessMessage(request.registrationType())
        );
    }


    // ============================================================
    // FIND OR CREATE VISITOR
    // ============================================================

    private Visitor findOrCreateVisitor(RegistrationRequest request) {

        validateVisitorValidity(request);

        String email = request.email();
        String mobileNumber = request.mobileNumber();

        /*
         * Check whether a visitor already exists with the supplied email.
         */
        Visitor visitorByEmail = visitorRepository
                .findByEmail(email)
                .orElse(null);

        /*
         * Check whether a visitor already exists with the supplied mobile number.
         */
        Visitor visitorByMobileNumber = visitorRepository
                .findByMobileNumber(mobileNumber)
                .orElse(null);

        /*
         * Case 1:
         * Both email and mobile belong to the same visitor.
         *
         * This is the valid existing visitor case.
         */
        if (visitorByEmail != null
                && visitorByMobileNumber != null
                && visitorByEmail.getId().equals(visitorByMobileNumber.getId())) {

            log.info(
                    "Existing visitor found. visitorId={}",
                    visitorByEmail.getId()
            );

            if (request.visitorType() == VisitorType.VENDOR) {

                visitorByEmail.setValidity(request.validity());

                visitorRepository.save(visitorByEmail);
            }

            return visitorByEmail;
        }

        /*
         * Case 2:
         * Email exists, but mobile is different or belongs to another visitor.
         */
        if (visitorByEmail != null) {

            log.warn(
                    "Visitor registration rejected because the supplied email is already associated with another visitor"
            );

            throw new EmailAlreadyExistsException(
                    "This email is already associated with another visitor"
            );
        }

        /*
         * Case 3:
         * Mobile exists, but email is different or belongs to another visitor.
         */
        if (visitorByMobileNumber != null) {

            log.warn(
                    "Visitor registration rejected because the supplied mobile number is already associated with another visitor"
            );

            throw new MobileNumberAlreadyExistsException(
                    "This mobile number is already associated with another visitor"
            );
        }

        /*
         * Case 4:
         * Neither email nor mobile exists.
         * Create a new Visitor profile.
         */
        Visitor newVisitor = new Visitor();

        newVisitor.setId(
                idGeneratorService.generateId("VISITOR", "VTR")
        );

        newVisitor.setFirstName(request.firstName());
        newVisitor.setLastName(request.lastName());
        newVisitor.setEmail(email);
        newVisitor.setMobileNumber(mobileNumber);
        newVisitor.setCompanyName(request.companyName());
        newVisitor.setValidity(request.validity());

        Visitor savedVisitor = visitorRepository.save(newVisitor);

        log.info(
                "New visitor profile created. visitorId={}",
                savedVisitor.getId()
        );

        return savedVisitor;
    }


    // ============================================================
    // VISITOR VALIDITY
    // ============================================================

    private void validateVisitorValidity(RegistrationRequest request) {

        if (request.visitorType() == VisitorType.VENDOR
                && request.validity() == null) {

            throw new BusinessRuleException(
                    "Validity is required for vendor visitors"
            );
        }

        if (request.visitorType() != VisitorType.VENDOR
                && request.validity() != null) {

            throw new BusinessRuleException(
                    "Validity must be null for non-vendor visitors"
            );
        }
    }


    // ============================================================
    // VISIT TIMING
    // ============================================================

    private void validateVisitTiming(RegistrationRequest request) {

        if (!request.expectedDepartureTime()
                .isAfter(request.expectedArrivalTime())) {

            log.warn(
                    "Visit registration rejected because expected departure time is not after expected arrival time. visitDate={}, arrivalTime={}, departureTime={}",
                    request.visitDate(),
                    request.expectedArrivalTime(),
                    request.expectedDepartureTime()
            );

            throw new BusinessRuleException(
                    "Expected departure time must be after expected arrival time on the same visit date"
            );
        }
    }


    // ============================================================
    // SUCCESS MESSAGE
    // ============================================================

    private String getSuccessMessage(RegistrationType registrationType) {

        return switch (registrationType) {

            case PRE_REGISTRATION ->
                    "Visitor pre-registered successfully";

            case ARRIVAL_REGISTRATION ->
                    "Visitor arrival registered successfully";
        };
    }


    // ============================================================
    // VISIT REFERENCE
    // ============================================================

    private String generateVisitReference() {

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        String randomPart = UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();

        return "VIS-" + timestamp + "-" + randomPart;
    }


    // ============================================================
    // DASHBOARD
    // ============================================================

    public List<VisitDashboardResponse> getDashboardVisits(
            VisitView view,
            String visitorId,
            String visitorName,
            String visitorEmail,
            VisitStatus status,
            LocalDate date,
            LocalDate fromDate,
            LocalDate toDate,
            String sortDirection
    ) {

        Specification<Visit> specification =
                (root, query, criteriaBuilder) ->
                        criteriaBuilder.conjunction();

        Specification<Visit> viewSpecification =
                VisitSpecification.hasView(view);

        if (viewSpecification != null) {
            specification = specification.and(viewSpecification);
        }

        Specification<Visit> visitorIdSpecification =
                VisitSpecification.hasVisitorId(visitorId);

        if (visitorIdSpecification != null) {
            specification = specification.and(visitorIdSpecification);
        }

        Specification<Visit> visitorNameSpecification =
                VisitSpecification.hasVisitorName(visitorName);

        if (visitorNameSpecification != null) {
            specification = specification.and(visitorNameSpecification);
        }

        Specification<Visit> visitorEmailSpecification =
                VisitSpecification.hasVisitorEmail(visitorEmail);

        if (visitorEmailSpecification != null) {
            specification = specification.and(visitorEmailSpecification);
        }

        Specification<Visit> statusSpecification =
                VisitSpecification.hasStatus(status);

        if (statusSpecification != null) {
            specification = specification.and(statusSpecification);
        }

        Specification<Visit> dateSpecification =
                VisitSpecification.hasDate(date);

        if (dateSpecification != null) {
            specification = specification.and(dateSpecification);
        }

        Specification<Visit> fromDateSpecification =
                VisitSpecification.hasFromDate(fromDate);

        if (fromDateSpecification != null) {
            specification = specification.and(fromDateSpecification);
        }

        Specification<Visit> toDateSpecification =
                VisitSpecification.hasToDate(toDate);

        if (toDateSpecification != null) {
            specification = specification.and(toDateSpecification);
        }

        Sort.Direction direction =
                "DESC".equalsIgnoreCase(sortDirection)
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;

        Sort sort =
                Sort.by(
                        direction,
                        "expectedArrivalAt"
                );

        List<Visit> visits =
                visitRepository.findAll(
                        specification,
                        sort
                );

        return visits.stream()
                .map(this::toDashboardResponse)
                .toList();
    }


    private VisitDashboardResponse toDashboardResponse(Visit visit) {

        Visitor visitor = visit.getVisitor();

        String visitorName =
                visitor.getFirstName() + " " + visitor.getLastName();

        String hostName = null;

        return new VisitDashboardResponse(
                visit.getId(),
                visit.getVisitReference(),

                visitor.getId(),
                visitorName,
                visitor.getEmail(),
                visitor.getMobileNumber(),
                visitor.getCompanyName(),

                visit.getVisitorType(),

                visit.getPurpose(),
                hostName,

                visit.getExpectedArrivalAt().toLocalDate(),

                visit.getStatus()
        );
    }


    // ============================================================
    // VISIT DETAILS
    // ============================================================

    public VisitDetailResponse getVisitDetails(String visitId) {

        Visit visit =
                visitRepository.findById(visitId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Visit not found with id: " + visitId
                                )
                        );

        return toVisitDetailResponse(visit);
    }


    /*
     * ============================================================
     * NDA IMPLEMENTATION TEMPORARILY DISABLED
     * ============================================================
     *
     * This method is kept for future NDA implementation.
     *
     * Reason:
     * DocumentService.getValidNda() is currently disabled because
     * NDA/document metadata handling is being implemented separately.
     *
     * When the NDA implementation is ready, uncomment this method
     * and restore the NDA fields in VisitDetailResponse.
     */

//    private VisitDetailResponse toVisitDetailResponse(Visit visit) {
//
//        Visitor visitor = visit.getVisitor();
//
//        Document validNda = documentService.getValidNda(visitor);
//
//        Employee employee = visit.getHost();
//
//        String hostName =
//                employee.getFirstName()
//                        + " "
//                        + employee.getLastName();
//
//        String departmentName =
//                employee.getDepartment().getDepartmentName();
//
//        return new VisitDetailResponse(
//
//                // Visit information
//                visit.getId(),
//                visit.getVisitReference(),
//
//                // Visitor information
//                new VisitDetailResponse.VisitorDetails(
//                        visitor.getId(),
//                        visitor.getFirstName(),
//                        visitor.getLastName(),
//                        visitor.getEmail(),
//                        visitor.getMobileNumber(),
//                        visitor.getCompanyName(),
//                        validNda != null,
//                        validNda != null ? validNda.getId() : null,
//                        validNda != null ? visitor.getValidity() : null
//                ),
//
//                visit.getVisitorType(),
//                visit.getRegistrationType(),
//                visit.getPurpose(),
//
//                // Host information
//                new VisitDetailResponse.HostDetails(
//                        employee.getId(),
//                        hostName,
//                        employee.getDepartment().getId(),
//                        departmentName
//                ),
//
//                // Schedule
//                visit.getExpectedArrivalAt(),
//                visit.getExpectedDepartureAt(),
//
//                // Actual visit times
//                visit.getCheckedInAt(),
//                visit.getCheckedOutAt(),
//
//                // Remarks
//                visit.getRemarks(),
//
//                // Status
//                visit.getStatus(),
//
//                // Audit information
//                new VisitDetailResponse.AuditDetails(
//                        visit.getCreatedAt(),
//                        visit.getUpdatedAt(),
//                        visit.getCreatedBy(),
//                        visit.getUpdatedBy()
//                )
//        );
//    }


    /*
     * Temporary VisitDetailResponse implementation.
     *
     * NDA fields are currently returned as:
     *
     *     false
     *     null
     *     null
     *
     * Once the coworker's NDA implementation is ready,
     * replace this method with the commented NDA implementation above.
     */
    private VisitDetailResponse toVisitDetailResponse(Visit visit) {

        Visitor visitor = visit.getVisitor();

        Employee employee = visit.getHost();

        String hostName =
                employee.getFirstName()
                        + " "
                        + employee.getLastName();

        String departmentName =
                employee.getDepartment().getDepartmentName();

        return new VisitDetailResponse(

                // Visit information
                visit.getId(),
                visit.getVisitReference(),

                // Visitor information
                new VisitDetailResponse.VisitorDetails(
                        visitor.getId(),
                        visitor.getFirstName(),
                        visitor.getLastName(),
                        visitor.getEmail(),
                        visitor.getMobileNumber(),
                        visitor.getCompanyName(),

                        /*
                         * NDA temporarily disabled.
                         */
                        false,
                        null,
                        null
                ),

                visit.getVisitorType(),
                visit.getRegistrationType(),
                visit.getPurpose(),

                // Host information
                new VisitDetailResponse.HostDetails(
                        employee.getId(),
                        hostName,
                        employee.getDepartment().getId(),
                        departmentName
                ),

                // Schedule
                visit.getExpectedArrivalAt(),
                visit.getExpectedDepartureAt(),

                // Actual visit times
                visit.getCheckedInAt(),
                visit.getCheckedOutAt(),

                // Remarks
                visit.getRemarks(),

                // Status
                visit.getStatus(),

                // Audit information
                new VisitDetailResponse.AuditDetails(
                        visit.getCreatedAt(),
                        visit.getUpdatedAt(),
                        visit.getCreatedBy(),
                        visit.getUpdatedBy()
                )
        );
    }


    // ============================================================
    // CANCEL VISIT
    // ============================================================

    @Transactional
    public VisitDetailResponse cancelVisit(String visitId) {

        log.info("Cancellation started. visitId={}", visitId);

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

        if (visit.getStatus() != VisitStatus.REGISTERED) {
            throw new BusinessRuleException(
                    "Only a registered visit can be cancelled"
            );
        }

        log.info("Setting visit status to CANCELLED");

        visit.setStatus(VisitStatus.CANCELLED);

        log.info("Saving cancelled visit");

        Visit cancelledVisit = visitRepository.save(visit);

        log.info(
                "Cancelled visit saved. visitId={}",
                cancelledVisit.getId()
        );

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

        CancellationEmailData emailData =
                new CancellationEmailData(
                        cancelledVisit.getVisitor().getFirstName()
                                + " "
                                + cancelledVisit.getVisitor().getLastName(),

                        cancelledVisit.getVisitor().getEmail(),

                        cancelledVisit.getVisitReference(),

                        cancelledVisit.getPurpose(),

                        cancelledVisit.getExpectedArrivalAt()
                                .format(formatter),

                        cancelledVisit.getExpectedDepartureAt()
                                .format(formatter),

                        cancelledVisit.getHost().getFirstName()
                                + " "
                                + cancelledVisit.getHost().getLastName()
                );

        log.info("Sending cancellation email");

        emailService.sendVisitCancellationEmail(emailData);

        log.info("Cancellation email triggered");

        log.info(
                "Visit cancelled successfully. visitId={}, visitReference={}, visitorId={}",
                cancelledVisit.getId(),
                cancelledVisit.getVisitReference(),
                cancelledVisit.getVisitor().getId()
        );

        return toVisitDetailResponse(cancelledVisit);
    }


    // ============================================================
    // CHECK-IN
    // ============================================================

    @Transactional
    public VisitDetailResponse checkIn(
            String visitId,
            CheckInRequest request
    ){

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

        if (!visit.getExpectedArrivalAt().toLocalDate().equals(LocalDate.now())) {
            throw new BusinessRuleException(
                    "Visit can only be checked in on the scheduled visit date"
            );
        }

        Document identityDocument =
                documentService.getLatestIdentityDocument(
                        visit.getVisitor().getId()
                );

        Nationality nationality =
                identityDocument.getNationality();

        proofValidationService.validate(
                nationality,
                request.aadharNumber(),
                request.panNumber(),
                request.passportNumber()
        );

        if (nationality == Nationality.DOMESTIC) {

            boolean aadhaarMatches =
                    documentService.verifyIdentityProof(
                            visit.getVisitor(),
                            ProofType.AADHAAR,
                            request.aadharNumber()
                    );

            boolean panMatches =
                    documentService.verifyIdentityProof(
                            visit.getVisitor(),
                            ProofType.PAN,
                            request.panNumber()
                    );

            if (!aadhaarMatches || !panMatches) {
                throw new BusinessRuleException(
                        "Identity verification failed"
                );
            }
        }

        if (nationality == Nationality.INTERNATIONAL) {

            boolean passportMatches =
                    documentService.verifyIdentityProof(
                            visit.getVisitor(),
                            ProofType.PASSPORT,
                            request.passportNumber()
                    );

            if (!passportMatches) {
                throw new BusinessRuleException(
                        "Identity verification failed"
                );
            }
        }


        /*
         * ========================================================
         * NDA SAFETY CHECK TEMPORARILY DISABLED
         * ========================================================
         *
         * This code is preserved for the coworker's future NDA
         * implementation.
         *
         * It must NOT be deleted.
         */

//        // NDA safety check
//        if (visit.getVisitorType() == VisitorType.VENDOR) {
//
//            Document validNda =
//                    documentService.getValidNda(
//                            visit.getVisitor()
//                    );
//
//            if (validNda == null) {
//                throw new BusinessRuleException(
//                        "A valid NDA is required for this vendor before check-in"
//                );
//            }
//        }


        visit.setCheckedInAt(LocalDateTime.now());

        visit.setStatus(VisitStatus.CHECKED_IN);

        Visit checkedInVisit = visitRepository.save(visit);

        VisitBadge badge =
                visitBadgeService.createBadge(checkedInVisit);

        String qrCode = qrCodeService.generateQrCode(
                badge.getQrContainingToken()
        );

        log.info(
                "QR code generated successfully. visitId={}, qrLength={}",
                checkedInVisit.getId(),
                qrCode.length()
        );

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

        BadgeEmailData emailData =
                new BadgeEmailData(
                        checkedInVisit.getVisitor().getFirstName()
                                + " "
                                + checkedInVisit.getVisitor().getLastName(),

                        checkedInVisit.getVisitor().getEmail(),

                        checkedInVisit.getVisitReference(),

                        checkedInVisit.getHost().getFirstName()
                                + " "
                                + checkedInVisit.getHost().getLastName(),

                        badge.getIssuedAt().format(formatter),

                        badge.getValidUntil().format(formatter),

                        qrCode,

                        null
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

                        checkedInVisit.getCheckedInAt()
                                .format(formatter)
                );

        emailService.sendHostCheckInNotification(
                hostEmailData
        );

        log.info(
                "Visitor checked in successfully. visitId={}, visitReference={}, visitorId={}, checkedInAt={}",
                checkedInVisit.getId(),
                checkedInVisit.getVisitReference(),
                checkedInVisit.getVisitor().getId(),
                checkedInVisit.getCheckedInAt()
        );

        return toVisitDetailResponse(checkedInVisit);
    }


    // ============================================================
    // CHECK-OUT
    // ============================================================

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

        log.info(
                "Visitor checked out successfully. visitId={}, visitReference={}, visitorId={}, checkedOutAt={}",
                checkedOutVisit.getId(),
                checkedOutVisit.getVisitReference(),
                checkedOutVisit.getVisitor().getId(),
                checkedOutVisit.getCheckedOutAt()
        );

        return toVisitDetailResponse(checkedOutVisit);
    }


    /*
     * ============================================================
     * NDA STATUS TEMPORARILY DISABLED
     * ============================================================
     *
     * This entire method is preserved for the coworker's NDA
     * implementation.
     *
     * It currently cannot compile because DocumentService no longer
     * exposes getValidNda().
     *
     * Do not delete it.
     */

//    @Transactional(readOnly = true)
//    public NdaStatusResponse getNdaStatus(String visitId) {
//
//        Visit visit = visitRepository.findById(visitId)
//                .orElseThrow(() -> new ResourceNotFoundException(
//                        "Visit not found with id: " + visitId));
//
//        Visitor visitor = visit.getVisitor();
//
//        Document validNda = documentService.getValidNda(visitor);
//
//        boolean ndaAvailable = validNda != null;
//
//        boolean ndaRequired =
//                visit.getVisitorType() == VisitorType.VENDOR
//                        && !ndaAvailable;
//
//        return new NdaStatusResponse(
//                visitor.getId(),
//                visit.getVisitorType().name(),
//                ndaRequired,
//                ndaAvailable,
//                ndaAvailable ? visitor.getValidity() : null,
//                ndaAvailable ? validNda.getId() : null
//        );
//    }


    // ============================================================
    // VENDOR CREATION
    // ============================================================

    private void createVendorIfRequired(
            Visitor visitor,
            RegistrationRequest request) {

        if (request.visitorType() != VisitorType.VENDOR) {
            return;
        }

        Optional<Vendor> existingVendor =
                vendorRepository.findByVisitorId(visitor.getId());

        if (existingVendor.isPresent()) {

            log.info(
                    "Vendor already exists for visitor. visitorId={}, vendorId={}",
                    visitor.getId(),
                    existingVendor.get().getId()
            );

            return;
        }

        Vendor vendor = new Vendor();

        vendor.setId(
                idGeneratorService.generateId("VENDOR", "VND")
        );

        vendor.setVisitor(visitor);

        vendor.setFirstName(visitor.getFirstName());

        vendor.setLastName(visitor.getLastName());

        vendor.setEmail(visitor.getEmail());

        vendor.setMobileNumber(visitor.getMobileNumber());

        vendor.setCompanyName(visitor.getCompanyName());

        vendor.setValidity(visitor.getValidity());

        vendorRepository.save(vendor);

        log.info(
                "Vendor created successfully. vendorId={}, visitorId={}",
                vendor.getId(),
                visitor.getId()
        );
    }


    // ============================================================
    // ID PROOF VALIDATION + BLACKLIST CHECK
    // ============================================================

    private void validateProofAndBlacklist(
            RegistrationRequest request) {

        /*
         * 1. Validate nationality and required proof combination.
         *
         * ProofValidationService validates the RAW proof values
         * before they are converted into HMAC blind indexes.
         */
        proofValidationService.validate(
                request.nationality(),
                request.aadharNumber(),
                request.panNumber(),
                request.passportNumber()
        );

        /*
         * 2. Check blacklist based on the applicable identity proofs.
         *
         * The blacklist service receives the RAW proof value here.
         * It generates the HMAC blind index internally and performs
         * the database lookup against vms_blacklist_proof.
         */
        switch (request.nationality()) {

            case DOMESTIC -> {

                boolean aadhaarBlacklisted =
                        blacklistService.isBlacklistedByProof(
                                ProofType.AADHAAR,
                                request.aadharNumber()
                        );

                if (aadhaarBlacklisted) {

                    throw new BusinessRuleException(
                            "Person is in blacklist. Visit registration is not allowed"
                    );
                }

                boolean panBlacklisted =
                        blacklistService.isBlacklistedByProof(
                                ProofType.PAN,
                                request.panNumber()
                        );

                if (panBlacklisted) {

                    throw new BusinessRuleException(
                            "Person is in blacklist. Visit registration is not allowed"
                    );
                }
            }

            case INTERNATIONAL -> {

                boolean passportBlacklisted =
                        blacklistService.isBlacklistedByProof(
                                ProofType.PASSPORT,
                                request.passportNumber()
                        );

                if (passportBlacklisted) {

                    throw new BusinessRuleException(
                            "Person is in blacklist. Visit registration is not allowed"
                    );
                }
            }
        }
    }
}