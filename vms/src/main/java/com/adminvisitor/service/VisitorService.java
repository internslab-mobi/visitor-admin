package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.VisitorRequest;
import com.adminvisitor.dto.responsedto.VisitorResponse;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.mapper.VisitorMapper;
import com.adminvisitor.repository.VisitorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VisitorService {

    private final VisitorRepository visitorRepository;
    private final VisitorMapper visitorMapper;

     // Creates a new visitor or reuses an existing visitor identified by email.
    public VisitorResponse createOrReuseVisitor(
            VisitorRequest request) {

        log.info(
                "Processing visitor profile. email={}",
                request.getEmail()
        );

        // Reuse the existing visitor when the email is already registered.
        Visitor visitor = visitorRepository
                .findByEmail(request.getEmail())
                .orElseGet(() -> {
                    log.info(
                            "Creating new visitor profile. email={}",
                            request.getEmail()
                    );

                    return visitorMapper.toEntity(request);
                });

        visitor.setFirstName(request.getFirstName());
        visitor.setLastName(request.getLastName());
        visitor.setMobileNumber(request.getMobileNumber());
        visitor.setCompanyName(request.getCompanyName());

        Visitor savedVisitor = visitorRepository.save(visitor);

        log.info(
                "Visitor profile saved successfully. visitorId={}",
                savedVisitor.getId()
        );

        return visitorMapper.toResponse(savedVisitor);
    }
}