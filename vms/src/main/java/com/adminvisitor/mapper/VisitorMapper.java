package com.adminvisitor.mapper;

import com.adminvisitor.dto.requestdto.VisitorRequest;
import com.adminvisitor.dto.responsedto.VisitorResponse;
import com.adminvisitor.entity.Visitor;
import org.springframework.stereotype.Component;

@Component
public class VisitorMapper {

    public Visitor toEntity(VisitorRequest request) {

        Visitor visitor = new Visitor();

        visitor.setFirstName(request.getFirstName());
        visitor.setLastName(request.getLastName());
        visitor.setEmail(request.getEmail());
        visitor.setMobileNumber(request.getMobileNumber());
        visitor.setCompanyName(request.getCompanyName());

        return visitor;
    }

    public VisitorResponse toResponse(Visitor visitor) {

        return new VisitorResponse(
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
    }
}