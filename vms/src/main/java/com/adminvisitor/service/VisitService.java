package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.RegistrationRequest;
import com.adminvisitor.dto.requestdto.VisitorRequest;
import com.adminvisitor.dto.responsedto.RegistrationResponse;
import com.adminvisitor.dto.responsedto.VisitorResponse;
import com.adminvisitor.entity.Visit;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.enums.RegistrationType;
import com.adminvisitor.enums.VisitStatus;
import com.adminvisitor.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VisitService {

    private final VisitRepository visitRepository;
    private final VisitorService visitorService;

    @Transactional
    public RegistrationResponse register(RegistrationRequest request) {

        validateVisitTiming(request);

        /*
         * 1. Create or reuse Visitor
         */
        VisitorRequest visitorRequest = new VisitorRequest();

        visitorRequest.setFirstName(request.firstName());
        visitorRequest.setLastName(request.lastName());
        visitorRequest.setEmail(request.email());
        visitorRequest.setMobileNumber(request.mobileNumber());
        visitorRequest.setCompanyName(request.companyName());

        VisitorResponse visitorResponse =
                visitorService.createOrReuseVisitor(visitorRequest);

        /*
         * Get the actual Visitor entity.
         *
         * We need the entity to create the Visit relationship.
         */
        Visitor visitor = new Visitor();
        visitor.setId(visitorResponse.id());

        /*
         * 2. Create Visit
         */
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

        /*
         * 3. Build response
         */
        return new RegistrationResponse(
                savedVisit.getId(),
                savedVisit.getVisitReference(),
                visitorResponse.id(),
                visitorResponse.firstName(),
                visitorResponse.lastName(),
                visitorResponse.email(),
                visitorResponse.mobileNumber(),
                visitorResponse.companyName(),
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

    private void validateVisitTiming(RegistrationRequest request) {

        if (!request.expectedDepartureTime()
                .isAfter(request.expectedArrivalTime())) {

            throw new IllegalArgumentException(
                    "Expected departure time must be after expected arrival time"
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

        if (registrationType == RegistrationType.PRE_REGISTRATION) {
            return "Visitor pre-registered successfully";
        }

        return "Visitor arrival registered successfully";
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