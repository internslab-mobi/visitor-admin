package com.adminvisitor.service.impl;

import com.adminvisitor.dto.requestdto.RegistrationRequest;
import com.adminvisitor.dto.responsedto.*;
import com.adminvisitor.entity.*;
import com.adminvisitor.enums.*;
import com.adminvisitor.exception.*;
import com.adminvisitor.mapper.VendorMapper;
import com.adminvisitor.mapper.VisitMapper;
import com.adminvisitor.mapper.VisitorMapper;
import com.adminvisitor.mapper.VisitMapper;
import com.adminvisitor.repository.*;
import com.adminvisitor.service.*;
import com.adminvisitor.util.VisitReferenceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implements visitor registration and visit-related operations.
 * Coordinates visitor identity verification, blacklist validation,
 * host validation, visit scheduling, vendor creation, and confirmation email delivery.
 *
 * @author Nethra
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VisitServiceImpl implements VisitService {

    /**
     * Visit statuses considered when checking for overlapping visits.
     */
    private static final List<VisitStatus> OVERLAP_STATUSES = List.of(
            VisitStatus.REGISTERED,
            VisitStatus.CHECKED_IN
    );

    private final VisitRepository visitRepository;
    private final VisitorRepository visitorRepository;
    private final EmailService emailService;
    private final IdGeneratorService idGeneratorService;
    private final EmployeeRepository employeeRepository;
    private final VendorRepository vendorRepository;
    private final BlacklistService blacklistService;
    private final DocumentRepository documentRepository;
    private final DocumentService documentService;
    private final ProofSecurityService proofSecurityService;
    private final VisitManagementServiceImpl visitManagementService;
    private final VendorMapper vendorMapper;
    private final VisitMapper visitMapper;
    private final VisitorMapper visitorMapper;
    private final VisitMapper visitMapper;

    /**
     * Registers a visitor's visit after validating identity proofs, blacklist status,
     * visit timing, visitor identity, host eligibility, and scheduling conflicts.
     * Saves the visit and sends a confirmation email after successful registration.
     *
     * @param request the visitor registration details and visit information
     * @return the registration response containing the saved visit and visitor details
     * @throws BusinessRuleException if a business validation rule is violated
     * @throws ResourceAlreadyExistsException if the email or mobile number is already associated with another visitor
     */
    @Transactional
    @Override
    public RegistrationResponse register(RegistrationRequest request) {

        long totalStart = System.nanoTime();

        try {
            log.debug(
                    "VISIT_REGISTRATION_STARTED registrationType={} visitorType={} hostId={}",
                    request.registrationType(),
                    request.visitorType(),
                    request.hostId()
            );

            long stageStart = System.nanoTime();

            validateProofAndBlacklist(request);

            log.debug(
                    "VISIT_REGISTRATION_STAGE_COMPLETED stage=PROOF_AND_BLACKLIST_VALIDATION elapsedMs={}",
                    elapsedMillis(stageStart)
            );

            stageStart = System.nanoTime();

            validateVisitTiming(request);

            log.debug(
                    "VISIT_REGISTRATION_STAGE_COMPLETED stage=VISIT_TIMING_VALIDATION elapsedMs={}",
                    elapsedMillis(stageStart)
            );

            stageStart = System.nanoTime();

            Visitor visitor = findOrCreateVisitor(request);

            boolean existingVisitor =
                    documentRepository.existsByVisitor_IdAndNationalityIsNotNull(
                            visitor.getId()
                    );

            if (existingVisitor) {

                Document existingDocument =
                        documentService.getLatestIdentityDocument(visitor.getId());

                if (existingDocument.getNationality() != request.nationality()) {
                    throw new BusinessRuleException(
                            "Nationality does not match the existing visitor's identity record"
                    );
                }

                switch (request.nationality()) {

                    case INDIA -> {
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

                        if (!aadhaarMatches || !panMatches) {
                            throw new BusinessRuleException(
                                    "Aadhaar or PAN does not match the existing visitor's identity records"
                            );
                        }
                    }

                    case INTERNATIONAL -> {
                        boolean passportMatches =
                                documentService.verifyIdentityProof(
                                        visitor,
                                        ProofType.PASSPORT,
                                        request.passportNumber()
                                );

                        if (!passportMatches) {
                            throw new BusinessRuleException(
                                    "Passport does not match the existing visitor's identity record"
                            );
                        }
                    }
                }

                log.debug(
                        "VISITOR_IDENTITY_VERIFIED visitorId={}",
                        visitor.getId()
                );

            } else {
                documentService.saveIdentityProofs(
                        visitor,
                        request.nationality(),
                        request.aadharNumber(),
                        request.panNumber(),
                        request.passportNumber()
                );
            }

            log.debug(
                    "VISIT_REGISTRATION_STAGE_COMPLETED stage=VISITOR_AND_IDENTITY_PROCESSING elapsedMs={}",
                    elapsedMillis(stageStart)
            );

            stageStart = System.nanoTime();

            createVendorIfRequired(visitor, request);

            log.debug(
                    "VISIT_REGISTRATION_STAGE_COMPLETED stage=VENDOR_PROCESSING elapsedMs={}",
                    elapsedMillis(stageStart)
            );

            stageStart = System.nanoTime();

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

            log.debug(
                    "VISIT_REGISTRATION_STAGE_COMPLETED stage=HOST_VALIDATION elapsedMs={}",
                    elapsedMillis(stageStart)
            );

            Visit visit = new Visit();

            stageStart = System.nanoTime();

            visit.setId(idGeneratorService.generateId("VISIT", "VIS"));

            log.debug(
                    "VISIT_REGISTRATION_STAGE_COMPLETED stage=VISIT_ID_GENERATION elapsedMs={}",
                    elapsedMillis(stageStart)
            );

            visit.setVisitReference(VisitReferenceUtil.generateVisitReference());
            visit.setVisitor(visitor);
            visit.setVisitorType(request.visitorType());
            visit.setRegistrationType(request.registrationType());
            visit.setPurpose(request.purpose());
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

            stageStart = System.nanoTime();

            List<Visit> overlappingVisits =
                    visitRepository.findOverlappingVisits(
                            visitor.getId(),
                            expectedArrivalAt,
                            expectedDepartureAt,
                            OVERLAP_STATUSES
                    );

            log.debug(
                    "VISIT_REGISTRATION_STAGE_COMPLETED stage=OVERLAP_CHECK elapsedMs={}",
                    elapsedMillis(stageStart)
            );

            if (!overlappingVisits.isEmpty()) {
                throw new BusinessRuleException(
                        "Visitor already has an active visit overlapping the requested time"
                );
            }

            visit.setStatus(VisitStatus.REGISTERED);

            Visit savedVisit = visitRepository.save(visit);

            emailService.sendVisitConfirmationEmail(savedVisit);

            log.info(
                    "VISIT_REGISTERED visitId={} visitReference={} visitorId={} registrationType={} status={}",
                    savedVisit.getId(),
                    savedVisit.getVisitReference(),
                    visitor.getId(),
                    savedVisit.getRegistrationType(),
                    savedVisit.getStatus()
            );

            return visitMapper.toRegistrationResponse(
                    savedVisit,
                    visitor,
                    getSuccessMessage(request.registrationType())
            );
        } catch (BusinessRuleException | ResourceAlreadyExistsException exception) {

            log.warn(
                    "VISIT_REGISTRATION_REJECTED registrationType={} visitorType={} hostId={} elapsedMs={} reason={}",
                    request.registrationType(),
                    request.visitorType(),
                    request.hostId(),
                    elapsedMillis(totalStart),
                    exception.getMessage()
            );

            throw exception;

        } catch (RuntimeException exception) {

            log.error(
                    "VISIT_REGISTRATION_FAILED registrationType={} visitorType={} hostId={} elapsedMs={}",
                    request.registrationType(),
                    request.visitorType(),
                    request.hostId(),
                    elapsedMillis(totalStart),
                    exception
            );

            throw exception;

        } finally {
            log.debug(
                    "VISIT_REGISTRATION_EXECUTION_COMPLETED elapsedMs={}",
                    elapsedMillis(totalStart)
            );
        }
    }

    /**
     * Finds an existing visitor using email and mobile number, or creates a new visitor.
     *
     * @param request the registration details used to identify or create the visitor
     * @return the existing visitor or the newly saved visitor
     * @throws ResourceAlreadyExistsException if the email or mobile number belongs to a different visitor
     */
    private Visitor findOrCreateVisitor(RegistrationRequest request) {

        String email = request.email();
        String mobileNumber = request.mobileNumber();

        Visitor visitorByEmail = visitorRepository
                .findByEmail(email)
                .orElse(null);

        Visitor visitorByMobileNumber = visitorRepository
                .findByMobileNumber(mobileNumber)
                .orElse(null);

        if (visitorByEmail != null
                && visitorByMobileNumber != null
                && visitorByEmail.getId().equals(visitorByMobileNumber.getId())) {

            log.debug(
                    "EXISTING_VISITOR_IDENTIFIED visitorId={}",
                    visitorByEmail.getId()
            );

            return visitorByEmail;
        }

        if (visitorByEmail != null) {
            log.warn("VISITOR_REGISTRATION_REJECTED reason=EMAIL_ALREADY_ASSOCIATED");

            throw new ResourceAlreadyExistsException(
                    "This email is already associated with another visitor"
            );
        }

        if (visitorByMobileNumber != null) {
            log.warn("VISITOR_REGISTRATION_REJECTED reason=MOBILE_ALREADY_ASSOCIATED");

            throw new ResourceAlreadyExistsException(
                    "This mobile number is already associated with another visitor"
            );
        }

        String visitorId = idGeneratorService.generateId("VISITOR", "VTR");

        Visitor newVisitor = visitorMapper.toEntity(request, visitorId);

        Visitor savedVisitor = visitorRepository.save(newVisitor);

        log.info(
                "VISITOR_PROFILE_CREATED visitorId={}",
                savedVisitor.getId()
        );

        return savedVisitor;
    }

    /**
     * Validates that the expected departure time is later than the expected arrival time.
     *
     * @param request the registration details containing the visit date and expected times
     * @throws BusinessRuleException if departure is not after arrival
     */
    private void validateVisitTiming(RegistrationRequest request) {

        if (!request.expectedDepartureTime().isAfter(request.expectedArrivalTime())) {

            log.warn(
                    "VISIT_REGISTRATION_REJECTED reason=INVALID_TIME_RANGE visitDate={} arrivalTime={} departureTime={}",
                    request.visitDate(),
                    request.expectedArrivalTime(),
                    request.expectedDepartureTime()
            );

            throw new BusinessRuleException(
                    "Expected departure time must be after expected arrival time on the same visit date"
            );
        }
    }

    /**
     * Returns a success message based on the registration type.
     *
     * @param registrationType the type of registration performed
     * @return the corresponding registration success message
     */
    private String getSuccessMessage(RegistrationType registrationType) {
        return switch (registrationType) {
            case PRE_REGISTRATION ->
                    "Visitor pre-registered successfully";

            case ARRIVAL_REGISTRATION ->
                    "Visitor arrival registered successfully";
        };
    }

    /**
     * Calculates the elapsed time since the supplied start time.
     *
     * @param startNanos the start time measured using {@link System#nanoTime()}
     * @return elapsed time in milliseconds
     */
    private double elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000.0;
    }

    /**
     * Creates a vendor record when the visitor's type is VENDOR.
     * Skips creation if a vendor record already exists for the visitor.
     *
     * @param visitor the visitor associated with the vendor
     * @param request the registration details
     */
    private void createVendorIfRequired(
            Visitor visitor,
            RegistrationRequest request
    ) {

        if (request.visitorType() != VisitorType.VENDOR) {
            return;
        }

        Optional<Vendor> existingVendor =
                vendorRepository.findByVisitorId(visitor.getId());

        if (existingVendor.isPresent()) {
            log.debug(
                    "VENDOR_CREATION_SKIPPED visitorId={} vendorId={} reason=ALREADY_EXISTS",
                    visitor.getId(),
                    existingVendor.get().getId()
            );
            return;
        }

        String vendorId = idGeneratorService.generateId("VENDOR", "VND");

        Vendor vendor = vendorMapper.toEntity(visitor, vendorId);


        vendorRepository.save(vendor);

        log.info(
                "VENDOR_CREATED vendorId={} visitorId={}",
                vendor.getId(),
                visitor.getId()
        );
    }

    /**
     * Validates identity proof details and checks whether the supplied proof
     * belongs to a blacklisted person.
     *
     * @param request the registration details containing nationality and identity proofs
     * @throws BusinessRuleException if an identity proof is invalid or matches a blacklist record
     */
    private void validateProofAndBlacklist(RegistrationRequest request) {

        proofSecurityService.validate(
                request.nationality(),
                request.aadharNumber(),
                request.panNumber(),
                request.passportNumber()
        );

        switch (request.nationality()) {

            case INDIA -> {

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

    /**
     * Retrieves the visitor photo path for a visit and verifies that the file exists
     * and is readable.
     *
     * @param visitId the unique identifier of the visit
     * @return the validated visitor photo file path
     * @throws ResourceNotFoundException if the photo file does not exist or cannot be read
     */
    @Override
    @Transactional(readOnly = true)
    public String getVisitPhotoPath(String visitId) {

        String photoPath =
                documentService.getVisitPhoto(visitId).getDocumentPath();

        Path path = Path.of(photoPath);

        if (!Files.exists(path) || !Files.isReadable(path)) {
            throw new ResourceNotFoundException(
                    "Visitor photo file not found for visit: " + visitId
            );
        }

        return photoPath;
    }



}