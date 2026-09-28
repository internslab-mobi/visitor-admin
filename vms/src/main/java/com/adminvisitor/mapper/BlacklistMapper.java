package com.adminvisitor.mapper;

import com.adminvisitor.dto.requestdto.BlacklistRequest;
import com.adminvisitor.dto.responsedto.BlacklistResponse;
import com.adminvisitor.entity.Blacklist;
import com.adminvisitor.entity.Visit;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.enums.VisitorType;
import com.adminvisitor.repository.VisitRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class BlacklistMapper {

    private final VisitRepository visitRepository;

    public BlacklistMapper(VisitRepository visitRepository) {
        this.visitRepository = visitRepository;
    }

    public Blacklist toEntity(
            BlacklistRequest request,
            Visitor visitor) {

        Blacklist blacklist = new Blacklist();

        blacklist.setVisitor(visitor);
        blacklist.setNationality(request.getNationality());
        blacklist.setReason(request.getReason());
        blacklist.setCreatedBy(request.getCreatedBy());

        return blacklist;
    }

    public BlacklistResponse toResponse(Blacklist blacklist) {

        Visitor visitor = blacklist.getVisitor();

        Optional<Visit> latestVisit =
                visitRepository.findTopByVisitorIdOrderByCreatedAtDesc(
                        visitor.getId()
                );

        VisitorType visitorType = latestVisit
                .map(Visit::getVisitorType)
                .orElse(null);

        return new BlacklistResponse(
                blacklist.getId(),
                visitor.getId(),
                visitor.getFirstName() + " " + visitor.getLastName(),

                // Visitor details
                visitor.getEmail(),
                visitor.getMobileNumber(),
                visitor.getCompanyName(),

                blacklist.getNationality(),
                blacklist.getReason(),
                blacklist.getStatus(),
                blacklist.getCreatedBy(),
                blacklist.getCreatedAt(),
                blacklist.getUpdatedAt(),
                blacklist.getUpdatedBy(),
                visitorType
        );
    }
}