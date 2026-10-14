package com.adminvisitor.service.impl;

import com.adminvisitor.dto.requestdto.RegistrationRequest;
import com.adminvisitor.dto.responsedto.*;
import com.adminvisitor.entity.*;
import com.adminvisitor.enums.*;
import com.adminvisitor.exception.BusinessRuleException;
import com.adminvisitor.exception.EmailAlreadyExistsException;
import com.adminvisitor.exception.MobileNumberAlreadyExistsException;
import com.adminvisitor.exception.ResourceNotFoundException;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class VisitServiceImpl implements VisitService {

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

    // VISIT REGISTRATION
    @Transactional
    @Override
    public RegistrationResponse register(RegistrationRequest request) {

        long totalStart = System.nanoTime();

        try {
            log.debug(
                    "Starting visit registration. registrationType={}, visitorType={}, hostId={}",
                    request.registrationType(),
                    request.visitorType(),
                    request.hostId()
            );

            // 1. Validate identity proofs and blacklist status.
            long stageStart = System.nanoTime();

            validateProofAndBlacklist(request);

            log.debug(
                    "Timing: validateProofAndBlacklist={} ms",
                    elapsedMillis(stageStart)
            );

            // 2. Validate the visit schedule.
            stageStart = System.nanoTime();

            validateVisitTiming(request);

            log.debug(
                    "Timing: validateVisitTiming={} ms",
                    elapsedMillis(stageStart)
            );

            // 3. Find or create the visitor and verify/store identity proofs.
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

                log.info(
                        "Existing visitor identity verified. visitorId={}",
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
                    "Timing: findOrCreateVisitorAndIdentityProcessing={} ms",
                    elapsedMillis(stageStart)
            );

            // 4. Create a vendor record when required.
            stageStart = System.nanoTime();

            createVendorIfRequired(visitor, request);

            log.debug(
                    "Timing: createVendorIfRequired={} ms",
                    elapsedMillis(stageStart)
            );

            // 5. Load and validate the selected host.
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
                    "Timing: loadAndValidateHost={} ms",
                    elapsedMillis(stageStart)
            );

            // 6. Build the visit.
            Visit visit = new Visit();

            stageStart = System.nanoTime();

            visit.setId(idGeneratorService.generateId("VISIT", "VIS"));

            log.debug(
                    "Timing: generateVisitId={} ms",
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

            // 7. Prevent overlapping registered or checked-in visits.
            stageStart = System.nanoTime();

            List<Visit> overlappingVisits =
                    visitRepository.findOverlappingVisits(
                            visitor.getId(),
                            expectedArrivalAt,
                            expectedDepartureAt,
                            OVERLAP_STATUSES
                    );

            log.debug(
                    "Timing: checkOverlappingVisits={} ms",
                    elapsedMillis(stageStart)
            );

            if (!overlappingVisits.isEmpty()) {
                throw new BusinessRuleException(
                        "Visitor already has an active visit overlapping the requested time"
                );
            }

            visit.setStatus(VisitStatus.REGISTERED);

            // 8. Persist the visit.
            stageStart = System.nanoTime();

        Visit savedVisit = visitRepository.save(visit);

            log.debug(
                    "Timing: visitRepository.save={} ms",
                    elapsedMillis(stageStart)
            );

            // 9. Send the confirmation email.
            stageStart = System.nanoTime();

            emailService.sendVisitConfirmationEmail(savedVisit);

            log.debug(
                    "Timing: sendVisitConfirmationEmail={} ms",
                    elapsedMillis(stageStart)
            );

        log.info(
                "Visit registered successfully. visitId={}, visitReference={}, visitorId={}, registrationType={}, status={}",
                savedVisit.getId(),
                savedVisit.getVisitReference(),
                visitor.getId(),
                savedVisit.getRegistrationType(),
                savedVisit.getStatus()
        );

            // 10. Return the existing registration response structure.
            return RegistrationResponse.builder()
                    .visitId(savedVisit.getId())
                    .visitReference(savedVisit.getVisitReference())
                    .visitorId(visitor.getId())
                    .firstName(visitor.getFirstName())
                    .lastName(visitor.getLastName())
                    .email(visitor.getEmail())
                    .mobileNumber(visitor.getMobileNumber())
                    .companyName(visitor.getCompanyName())
                    .visitorType(savedVisit.getVisitorType())
                    .registrationType(savedVisit.getRegistrationType())
                    .purpose(savedVisit.getPurpose())
                    .hostId(savedVisit.getHost().getId())
                    .expectedArrivalAt(savedVisit.getExpectedArrivalAt())
                    .expectedDepartureAt(savedVisit.getExpectedDepartureAt())
                    .remarks(savedVisit.getRemarks())
                    .status(savedVisit.getStatus())
                    .message(getSuccessMessage(request.registrationType()))
                    .build();

        } catch (RuntimeException exception) {
            log.error(
                    "Visit registration failed. registrationType={}, visitorType={}, hostId={}, elapsedMs={}",
                    request.registrationType(),
                    request.visitorType(),
                    request.hostId(),
                    elapsedMillis(totalStart),
                    exception);
            throw exception;
        } finally {
            log.info(
                    "Timing: total register method execution={} ms",elapsedMillis(totalStart));
        }
    }

    // FIND OR CREATE VISITOR
    private Visitor findOrCreateVisitor(RegistrationRequest request) {

        String email = request.email();
        String mobileNumber = request.mobileNumber();

        Visitor visitorByEmail = visitorRepository
                .findByEmail(email)
                .orElse(null);

        Visitor visitorByMobileNumber = visitorRepository
                .findByMobileNumber(mobileNumber)
                .orElse(null);

        // Both contact details must belong to the same visitor.
        if (visitorByEmail != null
                && visitorByMobileNumber != null
                && visitorByEmail.getId().equals(visitorByMobileNumber.getId())) {

            log.info(
                    "Existing visitor found. visitorId={}",
                    visitorByEmail.getId());
            return visitorByEmail;
        }

        if (visitorByEmail != null) {
            log.warn(
                    "Visitor registration rejected because the supplied email is already associated with another visitor");

            throw new EmailAlreadyExistsException(
                    "This email is already associated with another visitor");
        }

        if (visitorByMobileNumber != null) {
            log.warn(
                    "Visitor registration rejected because the supplied mobile number is already associated with another visitor");

            throw new MobileNumberAlreadyExistsException(
                    "This mobile number is already associated with another visitor");
        }

        Visitor newVisitor = new Visitor();
        newVisitor.setId(idGeneratorService.generateId("VISITOR", "VTR"));
        newVisitor.setFirstName(request.firstName());
        newVisitor.setLastName(request.lastName());
        newVisitor.setEmail(email);
        newVisitor.setMobileNumber(mobileNumber);
        newVisitor.setCompanyName(request.companyName());

        Visitor savedVisitor = visitorRepository.save(newVisitor);
        log.info(
                "New visitor profile created. visitorId={}",
                savedVisitor.getId());
        return savedVisitor;
    }

    // VISIT TIMING AND MESSAGES
    private void validateVisitTiming(RegistrationRequest request) {
        if (!request.expectedDepartureTime() .isAfter(request.expectedArrivalTime())) {
            log.warn(
                    "Visit registration rejected because expected departure time is not after expected arrival time. visitDate={}, arrivalTime={}, departureTime={}",
                    request.visitDate(),
                    request.expectedArrivalTime(),
                    request.expectedDepartureTime());

            throw new BusinessRuleException(
                    "Expected departure time must be after expected arrival time on the same visit date");
        }
    }

    private String getSuccessMessage(RegistrationType registrationType) {
        return switch (registrationType) {
            case PRE_REGISTRATION ->
                    "Visitor pre-registered successfully";

            case ARRIVAL_REGISTRATION ->
                    "Visitor arrival registered successfully";
        };
    }

    private double elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000.0;
    }

    // VENDOR
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
                    existingVendor.get().getId());
            return;
        }

        Vendor vendor = new Vendor();
        vendor.setId(idGeneratorService.generateId("VENDOR", "VND"));
        vendor.setVisitor(visitor);
        vendor.setFirstName(visitor.getFirstName());
        vendor.setLastName(visitor.getLastName());
        vendor.setEmail(visitor.getEmail());
        vendor.setMobileNumber(visitor.getMobileNumber());
        vendor.setCompanyName(visitor.getCompanyName());

        vendorRepository.save(vendor);
        log.info(
                "Vendor created successfully. vendorId={}, visitorId={}",
                vendor.getId(),
                visitor.getId());
    }

    // PROOF AND BLACKLIST VALIDATION
    private void validateProofAndBlacklist(RegistrationRequest request) {
        proofSecurityService.validate(
                request.nationality(),
                request.aadharNumber(),
                request.panNumber(),
                request.passportNumber());

        switch (request.nationality()) {
            case DOMESTIC -> {
                boolean aadhaarBlacklisted = blacklistService.isBlacklistedByProof(
                                ProofType.AADHAAR,
                                request.aadharNumber());

                if (aadhaarBlacklisted) {
                    throw new BusinessRuleException(
                            "Person is in blacklist. Visit registration is not allowed");
                }

                boolean panBlacklisted = blacklistService.isBlacklistedByProof(
                                ProofType.PAN,
                                request.panNumber());

                if (panBlacklisted) {
                    throw new BusinessRuleException(
                            "Person is in blacklist. Visit registration is not allowed");
                }
            }

            case INTERNATIONAL -> {
                boolean passportBlacklisted = blacklistService.isBlacklistedByProof(
                                ProofType.PASSPORT,
                                request.passportNumber());

                if (passportBlacklisted) {
                    throw new BusinessRuleException(
                            "Person is in blacklist. Visit registration is not allowed");
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getVisitPhotoPath(String visitId) {

        String photoPath = documentService.getVisitPhoto(visitId).getDocumentPath();
        Path path = Path.of(photoPath);

        if (!Files.exists(path) || !Files.isReadable(path)) {
            throw new ResourceNotFoundException(
                    "Visitor photo file not found for visit: " + visitId);
        }
        return photoPath;
    }
}