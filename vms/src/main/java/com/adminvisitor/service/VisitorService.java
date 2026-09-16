package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.VisitorRequest;
import com.adminvisitor.dto.responsedto.VisitorResponse;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.exception.EmailAlreadyExistsException;
import com.adminvisitor.exception.MobileNumberAlreadyExistsException;
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

    public VisitorResponse createVisitor(VisitorRequest request) {

        log.info(
                "Creating visitor profile. email={}, mobileNumber={}",
                request.getEmail(),
                request.getMobileNumber()
        );

        // Check whether the email is already registered.
        if (visitorRepository.findByEmail(request.getEmail()).isPresent()) {

            log.warn(
                    "Visitor creation rejected because email already exists. email={}",
                    request.getEmail()
            );

            throw new EmailAlreadyExistsException(
                    "A visitor with this email already exists"
            );
        }

        // Check whether the mobile number is already registered.
        if (visitorRepository.findByMobileNumber(request.getMobileNumber()).isPresent()) {

            log.warn(
                    "Visitor creation rejected because mobile number already exists. mobileNumber={}",
                    request.getMobileNumber()
            );

            throw new MobileNumberAlreadyExistsException(
                    "A visitor with this mobile number already exists"
            );
        }

        // Convert the request DTO into a Visitor entity.
        Visitor visitor = visitorMapper.toEntity(request);

        // Save the new visitor profile.
        Visitor savedVisitor = visitorRepository.save(visitor);

        log.info(
                "Visitor profile created successfully. visitorId={}",
                savedVisitor.getId()
        );

        // Convert the saved entity into a response DTO.
        return visitorMapper.toResponse(savedVisitor);
    }
}