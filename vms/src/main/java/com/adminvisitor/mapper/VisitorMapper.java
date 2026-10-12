package com.adminvisitor.mapper;

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
}