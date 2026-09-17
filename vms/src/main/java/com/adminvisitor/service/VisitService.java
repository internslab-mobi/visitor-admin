package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.RegistrationRequest;
import com.adminvisitor.dto.requestdto.VisitorRequest;
import com.adminvisitor.dto.responsedto.*;
import com.adminvisitor.entity.Visit;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.enums.RegistrationType;
import com.adminvisitor.enums.VisitStatus;
import com.adminvisitor.repository.VisitRepository;
import com.adminvisitor.repository.VisitorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import com.adminvisitor.enums.VisitView;
import com.adminvisitor.specification.VisitSpecification;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VisitService {

    private final VisitRepository visitRepository;
    private final VisitorService visitorService;
    private final VisitorRepository visitorRepository;
    private final EmailService emailService;


    @Transactional
    public RegistrationResponse register(RegistrationRequest request) {

        validateVisitTiming(request);

         //1. Find existing Visitor or create a new Visitor

        Visitor visitor = visitorRepository
                .findByEmail(request.email())
                .orElseGet(() -> {

                    VisitorRequest visitorRequest = new VisitorRequest();

                    visitorRequest.setFirstName(request.firstName());
                    visitorRequest.setLastName(request.lastName());
                    visitorRequest.setEmail(request.email());
                    visitorRequest.setMobileNumber(request.mobileNumber());
                    visitorRequest.setCompanyName(request.companyName());

                    VisitorResponse response =
                            visitorService.createVisitor(visitorRequest);

                    return visitorRepository
                            .findById(response.id())
                            .orElseThrow();
                });

        VisitorResponse visitorResponse =
                new VisitorResponse(
                        visitor.getId(),
                        visitor.getFirstName(),
                        visitor.getLastName(),
                        visitor.getEmail(),
                        visitor.getMobileNumber(),
                        visitor.getCompanyName(),
                        visitor.getCooldownUntil(),
                        visitor.getCreatedAt(),
                        visitor.getUpdatedAt()
                );


        //2. Create Visit

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

    public List<VisitDashboardResponse> getDashboardVisits(
            VisitView view,
            Long visitorId,
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

    private VisitDashboardResponse toDashboardResponse(
            Visit visit
    ) {

        Visitor visitor = visit.getVisitor();

        String visitorName =
                visitor.getFirstName()
                        + " "
                        + visitor.getLastName();

        /*
         * Host name will be populated once the
         * Employee module is connected.
         */
        String hostName = null;

        return new VisitDashboardResponse(

                visit.getId(),

                visit.getVisitReference(),

                visitor.getId(),

                visitorName,

                visitor.getCompanyName(),

                visit.getVisitorType(),

                visit.getPurpose(),

                hostName,

                visit.getStatus()
        );
    }

    public VisitDetailResponse getVisitDetails(Long visitId) {

        Visit visit =
                visitRepository.findById(visitId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Visit not found with id: " + visitId
                                )
                        );

        return toVisitDetailResponse(visit);
    }
    private VisitDetailResponse toVisitDetailResponse(Visit visit) {

        Visitor visitor = visit.getVisitor();

        /*
         * Temporary hardcoded host/department mapping.
         * This will later be replaced with Employee/Department
         * service or repository lookup.
         */
        Long hostId = visit.getHostId();

        String hostName;
        String departmentName;

        switch (hostId.intValue()) {
            case 1 -> {
                hostName = "Arun Kumar";
                departmentName = "Human Resources";
            }
            case 2 -> {
                hostName = "Priya Sharma";
                departmentName = "Engineering";
            }
            case 3 -> {
                hostName = "Rahul Raj";
                departmentName = "Finance";
            }
            case 6 -> {
                hostName = "Kavi Kumar";
                departmentName = "Operations";
            }
            default -> {
                hostName = "Unknown Host";
                departmentName = "Unknown Department";
            }
        }

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
                        visitor.getCompanyName()
                ),

                visit.getVisitorType(),

                visit.getRegistrationType(),

                visit.getPurpose(),

                // Host information
                new VisitDetailResponse.HostDetails(
                        visit.getHostId(),
                        hostName,
                        visit.getDepartmentId(),
                        departmentName
                ),

                // Schedule
                visit.getExpectedArrivalAt(),
                visit.getExpectedDepartureAt(),

                // Remarks
                visit.getRemarks(),

                // ID proof
                new VisitDetailResponse.ProofDetails(
                        visit.getProofType() != null
                                ? visit.getProofType().name()
                                : null,
                        visit.getProofNumber(),
                        visit.getProofImagePath()
                ),

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
}