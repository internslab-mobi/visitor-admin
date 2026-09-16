package com.adminvisitor.mapper;

import com.adminvisitor.dto.requestdto.BlacklistRequestDTO;
import com.adminvisitor.dto.responsedto.BlacklistResponseDTO;
import com.adminvisitor.entity.Blacklist;
import com.adminvisitor.entity.Visitor;
import org.springframework.stereotype.Component;

@Component
public class BlacklistMapper {

    public Blacklist toEntity(
            BlacklistRequestDTO request,
            Visitor visitor) {

        Blacklist blacklist = new Blacklist();

        blacklist.setVisitor(visitor);
        blacklist.setIdType(request.getIdType());
        blacklist.setIdNumber(request.getIdNumber());
        blacklist.setReason(request.getReason());
        blacklist.setAddedBy(request.getAddedBy());

        return blacklist;
    }

    public BlacklistResponseDTO toResponse(Blacklist blacklist) {

        return new BlacklistResponseDTO(
                blacklist.getId(),
                blacklist.getVisitor().getId(),
                blacklist.getIdType(),
                blacklist.getIdNumber(),
                blacklist.getReason(),
                blacklist.getStatus(),
                blacklist.getAddedBy(),
                blacklist.getAddedAt(),
                blacklist.getRemovedAt(),
                blacklist.getRemovedBy()
        );
    }
}