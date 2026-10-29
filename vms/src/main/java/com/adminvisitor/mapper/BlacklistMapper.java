
package com.adminvisitor.mapper;

import com.adminvisitor.dto.responsedto.BlacklistResponse;
import com.adminvisitor.entity.Blacklist;
import com.adminvisitor.enums.ProofType;
import com.adminvisitor.enums.VisitorType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BlacklistMapper {

    @Mapping(target = "id", source = "blacklist.id")
    @Mapping(target = "visitorId", source = "blacklist.visitor.id")
    @Mapping(
            target = "visitorName",
            expression = "java(blacklist.getVisitor().getFirstName() + \" \" + blacklist.getVisitor().getLastName())"
    )
    @Mapping(target = "email", source = "blacklist.visitor.email")
    @Mapping(target = "mobileNumber", source = "blacklist.visitor.mobileNumber")
    @Mapping(target = "companyName", source = "blacklist.visitor.companyName")
    @Mapping(target = "nationality", source = "blacklist.nationality")
    @Mapping(target = "reason", source = "blacklist.reason")
    @Mapping(target = "status", source = "blacklist.status")
    @Mapping(target = "createdBy", source = "blacklist.createdBy")
    @Mapping(target = "createdAt", source = "blacklist.createdAt")
    @Mapping(target = "updatedAt", source = "blacklist.updatedAt")
    @Mapping(target = "updatedBy", source = "blacklist.updatedBy")
    @Mapping(target = "visitorType", source = "visitorType")
    @Mapping(target = "matchingProofTypes", source = "matchingProofTypes")
    BlacklistResponse toResponse(
            Blacklist blacklist,
            VisitorType visitorType,
            List<ProofType> matchingProofTypes
    );
}
