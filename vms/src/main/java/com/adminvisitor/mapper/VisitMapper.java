package com.adminvisitor.mapper;

import com.adminvisitor.dto.responsedto.VisitDashboardResponse;
import com.adminvisitor.dto.responsedto.VisitDetailResponse;
import com.adminvisitor.dto.responsedto.VisitorLogResponse;
import com.adminvisitor.entity.Visit;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.entity.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VisitMapper {

    @Mapping(target = "visitId", source = "id")
    @Mapping(target = "visitorId", source = "visitor.id")
    @Mapping(target = "visitorName", expression = "java(visitorName(visit.getVisitor()))")
    @Mapping(target = "hostName", expression = "java(employeeName(visit.getHost()))")
    @Mapping(target = "visitDate", expression = "java(visit.getExpectedArrivalAt() != null ? visit.getExpectedArrivalAt().toLocalDate() : null)")
    VisitorLogResponse toVisitorLogResponse(Visit visit);

    @Mapping(target = "visitId", source = "id")
    @Mapping(target = "visitorId", source = "visitor.id")
    @Mapping(target = "visitorName", expression = "java(visitorName(visit.getVisitor()))")
    @Mapping(target = "visitorEmail", source = "visitor.email")
    @Mapping(target = "visitorMobile", source = "visitor.mobileNumber")
    @Mapping(target = "hostName", expression = "java(employeeName(visit.getHost()))")
    @Mapping(target = "lastVisit", expression = "java(visit.getExpectedArrivalAt() != null ? visit.getExpectedArrivalAt().toLocalDate() : null)")
    VisitDashboardResponse toDashboardResponse(Visit visit);

    @Mapping(target = "visitId", source = "id")
    @Mapping(target = "visitor", source = "visitor")
    @Mapping(target = "host", source = "host")
    @Mapping(target = "audit", source = "visit")
    @Mapping(target = "totalDurationSeconds", ignore = true)
    VisitDetailResponse toVisitDetailResponse(Visit visit);

    @Mapping(target = "visitorId", source = "id")
    @Mapping(target = "nationality", ignore = true)
    @Mapping(target = "ndaAvailable", ignore = true)
    @Mapping(target = "ndaDocumentId", ignore = true)
    VisitDetailResponse.VisitorDetails toVisitorDetails(Visitor visitor);

    @Mapping(target = "hostId", source = "id")
    @Mapping(target = "hostName", expression = "java(employeeName(employee))")
    @Mapping(target = "departmentId", source = "department.id")
    @Mapping(target = "departmentName", source = "department.departmentName")
    VisitDetailResponse.HostDetails toHostDetails(Employee employee);

    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "createdBy", source = "createdBy")
    @Mapping(target = "updatedBy", source = "updatedBy")
    VisitDetailResponse.AuditDetails toAuditDetails(Visit visit);

    @Mapping(target = "nationality", source = "nationality")
    @Mapping(target = "ndaAvailable", source = "ndaAvailable")
    @Mapping(target = "ndaDocumentId", source = "ndaDocumentId")
    VisitDetailResponse.VisitorDetails toVisitorDetails(
            Visitor visitor,
            String nationality,
            boolean ndaAvailable,
            String ndaDocumentId
    );

    @Mapping(target = "visitor", source = "visitorDetails")
    VisitDetailResponse toVisitDetailResponseWithVisitor(
            VisitDetailResponse response,
            VisitDetailResponse.VisitorDetails visitorDetails
    );

    default String visitorName(Visitor visitor) {
        if (visitor == null) {
            return null;
        }

        String firstName = visitor.getFirstName() == null ? "" : visitor.getFirstName();
        String lastName = visitor.getLastName() == null ? "" : visitor.getLastName();

        return (firstName + " " + lastName).trim();
    }

    default String employeeName(Employee employee) {
        if (employee == null) {
            return null;
        }

        String firstName = employee.getFirstName() == null ? "" : employee.getFirstName();
        String lastName = employee.getLastName() == null ? "" : employee.getLastName();

        return (firstName + " " + lastName).trim();
    }

//    default Long calculateDuration(Visit visit) {
//        if (visit == null
//                || visit.getCheckedInAt() == null
//                || visit.getCheckedOutAt() == null) {
//            return null;
//        }
//
//        return Duration.between(
//                visit.getCheckedInAt(),
//                visit.getCheckedOutAt()
//        ).getSeconds();
//    }
}
