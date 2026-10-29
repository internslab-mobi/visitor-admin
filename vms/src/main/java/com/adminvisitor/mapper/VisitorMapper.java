package com.adminvisitor.mapper;

import com.adminvisitor.dto.requestdto.RegistrationRequest;
import com.adminvisitor.dto.responsedto.VisitorResponse;
import com.adminvisitor.entity.Visit;
import com.adminvisitor.entity.Visitor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VisitorMapper {

    @Mapping(source = "visitor.id", target = "id")
    @Mapping(source = "latestVisit.visitorType", target = "visitorType")
    @Mapping(source = "latestVisit.expectedArrivalAt", target = "lastVisitDate")
    VisitorResponse toResponse(Visitor visitor, Visit latestVisit);


    // Registration request -> Visitor entity
    @Mapping(target = "id", source = "visitorId")
    @Mapping(target = "firstName", source = "request.firstName")
    @Mapping(target = "lastName", source = "request.lastName")
    @Mapping(target = "email", source = "request.email")
    @Mapping(target = "mobileNumber", source = "request.mobileNumber")
    @Mapping(target = "companyName", source = "request.companyName")
    Visitor toEntity(RegistrationRequest request, String visitorId);
}