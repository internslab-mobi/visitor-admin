package com.adminvisitor.mapper;

import com.adminvisitor.dto.responsedto.RegistrationResponse;
import com.adminvisitor.dto.responsedto.VisitDashboardResponse;
import com.adminvisitor.dto.responsedto.VisitDetailResponse;
import com.adminvisitor.dto.responsedto.VisitorLogResponse;
import com.adminvisitor.entity.Visit;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.entity.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.adminvisitor.entity.MetaTimeTracking;
import com.adminvisitor.dto.responsedto.VisitTimeTrackingResponse;
import java.time.LocalDateTime;
import java.time.Duration;

@Mapper(componentModel = "spring")
public interface VisitMapper {

    @Mapping(target = "visitId", source = "id")
    @Mapping(target = "visitorId", source = "visitor.id")
    @Mapping(target = "visitorName",
            expression = "java(visitorName(visit.getVisitor()))")
    @Mapping(target = "hostName",
            expression = "java(employeeName(visit.getHost()))")
    @Mapping(target = "visitDate",
            expression = "java(visit.getExpectedArrivalAt() != null ? visit.getExpectedArrivalAt().toLocalDate() : null)")
    VisitorLogResponse toVisitorLogResponse(Visit visit);

    @Mapping(target = "visitId", source = "id")
    @Mapping(target = "visitorId", source = "visitor.id")
    @Mapping(target = "visitorName",
            expression = "java(visitorName(visit.getVisitor()))")
    @Mapping(target = "visitorEmail", source = "visitor.email")
    @Mapping(target = "visitorMobile", source = "visitor.mobileNumber")
    @Mapping(target = "hostName",
            expression = "java(employeeName(visit.getHost()))")
    @Mapping(target = "lastVisit",
            expression = "java(visit.getExpectedArrivalAt() != null ? visit.getExpectedArrivalAt().toLocalDate() : null)")
    VisitDashboardResponse toDashboardResponse(Visit visit);

    @Mapping(target = "visitId", source = "id")
    @Mapping(target = "visitor", source = "visitor")
    @Mapping(target = "host", source = "host")
    @Mapping(target = "audit", source = "visit")
    @Mapping(target = "totalDurationSeconds",
            expression = "java(calculateDuration(visit))")
    VisitDetailResponse toVisitDetailResponse(Visit visit);

    @Mapping(target = "visitorId", source = "id")
    @Mapping(target = "nationality", ignore = true)
    @Mapping(target = "ndaAvailable", ignore = true)
    @Mapping(target = "ndaDocumentId", ignore = true)
    VisitDetailResponse.VisitorDetails toVisitorDetails(Visitor visitor);

    @Mapping(target = "hostId", source = "id")
    @Mapping(target = "hostName",
            expression = "java(employeeName(employee))")
    @Mapping(target = "departmentId", source = "department.id")
    @Mapping(target = "departmentName", source = "department.departmentName")
    VisitDetailResponse.HostDetails toHostDetails(Employee employee);

    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "createdBy", source = "createdBy")
    @Mapping(target = "updatedBy", source = "updatedBy")
    VisitDetailResponse.AuditDetails toAuditDetails(Visit visit);

    @Mapping(target = "visitorId", source = "visitor.id")
    @Mapping(target = "nationality", source = "nationality")
    @Mapping(target = "ndaAvailable", source = "ndaAvailable")
    @Mapping(target = "ndaDocumentId", source = "ndaDocumentId")
    @Mapping(target = "existingVendor", source = "existingVendor")
    VisitDetailResponse.VisitorDetails toVisitorDetails(
            Visitor visitor,
            String nationality,
            boolean ndaAvailable,
            String ndaDocumentId,
            boolean existingVendor
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

        String firstName = visitor.getFirstName() == null
                ? "" : visitor.getFirstName();
        String lastName = visitor.getLastName() == null
                ? "" : visitor.getLastName();

        return (firstName + " " + lastName).trim();
    }

    default String employeeName(Employee employee) {
        if (employee == null) {
            return null;
        }

        String firstName = employee.getFirstName() == null
                ? "" : employee.getFirstName();
        String lastName = employee.getLastName() == null
                ? "" : employee.getLastName();

        return (firstName + " " + lastName).trim();
    }

    default Long calculateDuration(Visit visit) {
        if (visit == null
                || visit.getCheckedInAt() == null
                || visit.getCheckedOutAt() == null) {
            return null;
        }

        return Duration.between(
                visit.getCheckedInAt(),
                visit.getCheckedOutAt()
        ).getSeconds();
    }

    @Mapping(target = "visitId", source = "visit.id")
    @Mapping(target = "visitReference", source = "visit.visitReference")
    @Mapping(target = "visitorId", source = "visitor.id")
    @Mapping(target = "firstName", source = "visitor.firstName")
    @Mapping(target = "lastName", source = "visitor.lastName")
    @Mapping(target = "email", source = "visitor.email")
    @Mapping(target = "mobileNumber", source = "visitor.mobileNumber")
    @Mapping(target = "companyName", source = "visitor.companyName")
    @Mapping(target = "visitorType", source = "visit.visitorType")
    @Mapping(target = "registrationType", source = "visit.registrationType")
    @Mapping(target = "purpose", source = "visit.purpose")
    @Mapping(target = "hostId", source = "visit.host.id")
    @Mapping(target = "expectedArrivalAt", source = "visit.expectedArrivalAt")
    @Mapping(target = "expectedDepartureAt", source = "visit.expectedDepartureAt")
    @Mapping(target = "remarks", source = "visit.remarks")
    @Mapping(target = "status", source = "visit.status")
    @Mapping(target = "message", source = "message")
    RegistrationResponse toRegistrationResponse(
            Visit visit,
            Visitor visitor,
            String message
    );

    @Mapping(target = "trackingId", source = "tracking.id")
    @Mapping(target = "badgeId", source = "tracking.badgeId")
    @Mapping(target = "inTime", source = "tracking.inTime")
    @Mapping(target = "outTime", source = "tracking.outTime")
    @Mapping(
            target = "durationSeconds",
            expression = "java(java.time.Duration.between(tracking.getInTime(), tracking.getOutTime() != null ? tracking.getOutTime() : referenceTime).getSeconds())"
    )
    VisitTimeTrackingResponse.SessionResponse toTrackingSessionResponse(
            MetaTimeTracking tracking,
            LocalDateTime referenceTime
    );
}