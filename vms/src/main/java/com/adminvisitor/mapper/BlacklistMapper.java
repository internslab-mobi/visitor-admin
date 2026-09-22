package com.adminvisitor.mapper;

import com.adminvisitor.dto.requestdto.BlacklistRequest;
import com.adminvisitor.dto.responsedto.BlacklistResponse;
import com.adminvisitor.entity.Blacklist;
import com.adminvisitor.entity.Visitor;
import org.springframework.stereotype.Component;

@Component
public class BlacklistMapper {

    public Blacklist toEntity(
            BlacklistRequest request,
            Visitor visitor) {

        Blacklist blacklist = new Blacklist();

        blacklist.setVisitor(visitor);
        blacklist.setNationality(request.getNationality());
        blacklist.setProofType(request.getProofType());
        blacklist.setReason(request.getReason());
        blacklist.setCreatedBy(request.getCreatedBy());

        return blacklist;
    }

    public BlacklistResponse toResponse(Blacklist blacklist) {

        return new BlacklistResponse(
                blacklist.getId(),
                blacklist.getVisitor().getId(),
                blacklist.getNationality(),
                blacklist.getProofType(),
                blacklist.getReason(),
                blacklist.getStatus(),
                blacklist.getCreatedBy(),
                blacklist.getCreatedAt(),
                blacklist.getUpdatedAt(),
                blacklist.getUpdatedBy()
        );
    }
}