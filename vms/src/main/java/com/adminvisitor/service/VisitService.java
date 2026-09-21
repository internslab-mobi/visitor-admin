package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.RegistrationRequest;
import com.adminvisitor.dto.responsedto.RegistrationResponse;
import com.adminvisitor.dto.responsedto.VisitDashboardResponse;
import com.adminvisitor.dto.responsedto.VisitDetailResponse;
import com.adminvisitor.entity.Employee;
import com.adminvisitor.entity.Visit;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.enums.RegistrationType;
import com.adminvisitor.enums.VisitStatus;
import com.adminvisitor.exception.BusinessRuleException;
import com.adminvisitor.exception.EmailAlreadyExistsException;
import com.adminvisitor.exception.MobileNumberAlreadyExistsException;
import com.adminvisitor.repository.EmployeeRepository;
import com.adminvisitor.repository.VisitRepository;
import com.adminvisitor.repository.VisitorRepository;
import com.adminvisitor.specification.VisitSpecification;
import com.adminvisitor.enums.VisitView;
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

    @Transactional
    public RegistrationResponse register(RegistrationRequest request) {

        log.debug(
                "Starting visit registration. registrationType={}, visitorType={}, hostId={}",
                request.registrationType(),
                request.visitorType(),
                request.hostId()
        );

        // 1. Validate visit timing
        long start = System.currentTimeMillis();

        validateVisitTiming(request);

        log.debug(
                "Timing: validateVisitTiming={} ms",
                System.currentTimeMillis() - start
        );

        // 2. Find existing visitor or create a new visitor
        start = System.currentTimeMillis();

        Visitor visitor = findOrCreateVisitor(request);

        log.debug(
                "Timing: findOrCreateVisitor={} ms",
                System.currentTimeMillis() - start
        );

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
        visit.setDepartment(employee.getDepartment());

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
                savedVisit.getDepartment().getId(),
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

        newVisitor.setId(
                idGeneratorService.generateId("VISITOR", "VTR")
        );

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
                        visitor.getCompanyName()
                ),

                visit.getVisitorType(),

                visit.getRegistrationType(),

                visit.getPurpose(),

                // Host information
                new VisitDetailResponse.HostDetails(
                        visit.getHost().getId(),
                        hostName,
                        visit.getDepartment().getId(),
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