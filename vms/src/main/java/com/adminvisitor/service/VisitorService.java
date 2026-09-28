package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.UpdateVisitorRequest;
import com.adminvisitor.dto.responsedto.VisitorResponse;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.exception.EmailAlreadyExistsException;
import com.adminvisitor.exception.MobileNumberAlreadyExistsException;
import com.adminvisitor.exception.VisitorNotFoundException;
import com.adminvisitor.repository.VisitRepository;
import com.adminvisitor.repository.VisitorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.adminvisitor.entity.Visit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisitorService {

    private final VisitorRepository visitorRepository;

    private final VisitRepository visitRepository;
    @Transactional(readOnly = true)
    public VisitorResponse getVisitor(String visitorId) {

        log.info("Fetching visitor. visitorId={}", visitorId);

        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() ->
                        new VisitorNotFoundException(
                                "Visitor not found with id: " + visitorId));

        return toVisitorResponse(visitor);
    }

    @Transactional(readOnly = true)
    public List<VisitorResponse> getAllVisitors() {

        log.info("Fetching all visitors");

        return visitorRepository.findAll()
                .stream()
                .map(this::toVisitorResponse)
                .toList();
    }
    @Transactional
    public VisitorResponse updateVisitor(
            String visitorId,
            UpdateVisitorRequest request) {

        log.info("Updating visitor. visitorId={}", visitorId);

        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() ->
                        new VisitorNotFoundException(
                                "Visitor not found with id: " + visitorId));

        // Check whether email belongs to another visitor
        visitorRepository.findByEmail(request.email())
                .ifPresent(existingVisitor -> {

                    if (!existingVisitor.getId().equals(visitorId)) {
                        throw new EmailAlreadyExistsException(
                                "This email is already associated with another visitor");
                    }
                });

        // Check whether mobile number belongs to another visitor
        visitorRepository.findByMobileNumber(request.mobileNumber())
                .ifPresent(existingVisitor -> {

                    if (!existingVisitor.getId().equals(visitorId)) {
                        throw new MobileNumberAlreadyExistsException(
                                "This mobile number is already associated with another visitor");
                    }
                });

        visitor.setFirstName(request.firstName());
        visitor.setLastName(request.lastName());
        visitor.setEmail(request.email());
        visitor.setMobileNumber(request.mobileNumber());
        visitor.setCompanyName(request.companyName());
       // visitor.setValidity(request.validity());

        Visitor updatedVisitor = visitorRepository.save(visitor);

        log.info(
                "Visitor updated successfully. visitorId={}",
                updatedVisitor.getId()
        );

        return toVisitorResponse(updatedVisitor);
    }

    private VisitorResponse toVisitorResponse(Visitor visitor) {

        Visit latestVisit =
                visitRepository
                        .findTopByVisitorIdOrderByCreatedAtDesc(
                                visitor.getId()
                        )
                        .orElse(null);

        return new VisitorResponse(

                visitor.getId(),

                visitor.getFirstName(),

                visitor.getLastName(),

                visitor.getEmail(),

                visitor.getMobileNumber(),

                visitor.getCompanyName(),

                latestVisit != null
                        ? latestVisit.getVisitorType().name()
                        : null,

                latestVisit != null
                        ? latestVisit.getExpectedArrivalAt()
                        : null
        );
    }
}