package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.VisitorRequest;
import com.adminvisitor.dto.responsedto.VisitorResponse;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.exception.EmailAlreadyExistsException;
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
                "Creating visitor profile. email={}",
                request.getEmail()
        );

        if (visitorRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn(
                    "Visitor creation rejected because email already exists. email={}",
                    request.getEmail()
            );

            throw new EmailAlreadyExistsException(
                    "A visitor with this email already exists"
            );
        }

        Visitor visitor = visitorMapper.toEntity(request);

        Visitor savedVisitor = visitorRepository.save(visitor);

        log.info(
                "Visitor profile created successfully. visitorId={}",
                savedVisitor.getId()
        );

        return visitorMapper.toResponse(savedVisitor);
    }
}