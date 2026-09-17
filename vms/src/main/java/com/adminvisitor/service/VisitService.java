package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.RegistrationRequest;
import com.adminvisitor.dto.responsedto.RegistrationResponse;
import com.adminvisitor.entity.Visit;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.enums.RegistrationType;
import com.adminvisitor.enums.VisitStatus;
import com.adminvisitor.exception.BusinessRuleException;
import com.adminvisitor.exception.EmailAlreadyExistsException;
import com.adminvisitor.exception.MobileNumberAlreadyExistsException;
import com.adminvisitor.repository.VisitRepository;
import com.adminvisitor.repository.VisitorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisitService {

    private final VisitRepository visitRepository;
    private final VisitorRepository visitorRepository;
    private final EmailService emailService;

    @Transactional
    public RegistrationResponse register(RegistrationRequest request) {

        log.debug(
                "Starting visit registration. registrationType={}, visitorType={}, hostId={}",
                request.registrationType(),
                request.visitorType(),
                request.hostId()
        );

        validateVisitTiming(request);

        // 1. Find an existing Visitor or create a new Visitor.
        Visitor visitor = findOrCreateVisitor(request);

        // 2. Create Visit.
        Visit visit = new Visit();

        visit.setVisitReference(generateVisitReference());

        visit.setVisitor(visitor);

        visit.setVisitorType(request.visitorType());

        visit.setRegistrationType(request.registrationType());

        visit.setPurpose(request.purpose());

        visit.setHostId(request.hostId());

        /*
         * Temporary department mapping.
         *
         * This method will later be replaced with
         * EmployeeService/EmployeeRepository logic.
         */
        Long departmentId = getDepartmentIdForHost(request.hostId());
        visit.setDepartmentId(departmentId);

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

        /*
         * ID proof is optional for now.
         * Actual verification will happen during check-in.
         */
        visit.setProofType(request.proofType());
        visit.setProofNumber(request.proofNumber());

        visit.setStatus(VisitStatus.REGISTERED);

        Visit savedVisit = visitRepository.save(visit);

        emailService.sendVisitConfirmationEmail(savedVisit);

        log.info(
                "Visit registered successfully. visitId={}, visitReference={}, visitorId={}, registrationType={}, status={}",
                savedVisit.getId(),
                savedVisit.getVisitReference(),
                visitor.getId(),
                savedVisit.getRegistrationType(),
                savedVisit.getStatus()
        );

        // 3. Build response.
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
                savedVisit.getHostId(),
                savedVisit.getDepartmentId(),
                savedVisit.getExpectedArrivalAt(),
                savedVisit.getExpectedDepartureAt(),
                savedVisit.getRemarks(),
                savedVisit.getStatus(),
                getSuccessMessage(request.registrationType())
        );
    }

    private Visitor findOrCreateVisitor(RegistrationRequest request) {

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

        newVisitor.setFirstName(request.firstName());
        newVisitor.setLastName(request.lastName());
        newVisitor.setEmail(email);
        newVisitor.setMobileNumber(mobileNumber);
        newVisitor.setCompanyName(request.companyName());

        Visitor savedVisitor = visitorRepository.save(newVisitor);

        log.info(
                "New visitor profile created. visitorId={}",
                savedVisitor.getId()
        );

        return savedVisitor;
    }

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

    /*
     * TEMPORARY implementation.
     *
     * Later this method will call the Employee module.
     * Keeping the logic isolated means the rest of VisitService
     * does not need to change.
     */
    private Long getDepartmentIdForHost(Long hostId) {

        /*
         * Temporary hardcoded host → department mapping.
         *
         * Replace these values with actual employee/department
         * data when the Employee files are available.
         */
        return switch (hostId.intValue()) {
            case 1 -> 1L;
            case 2 -> 2L;
            case 3 -> 3L;
            default -> 1L;
        };
    }

    private String getSuccessMessage(RegistrationType registrationType) {

        return switch (registrationType) {
            case PRE_REGISTRATION -> "Visitor pre-registered successfully";
            case ARRIVAL_REGISTRATION -> "Visitor arrival registered successfully";
        };
    }

    private String generateVisitReference() {

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        String randomPart = UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();

        return "VIS-" + timestamp + "-" + randomPart;
    }
}