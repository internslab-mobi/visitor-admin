package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.CheckInRequest;
import com.adminvisitor.dto.responsedto.*;
import com.adminvisitor.enums.*;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import com.adminvisitor.dto.responsedto.VisitTimeTrackingResponse;

public interface VisitManagementService {

    void verifyIdentity(String visitId, CheckInRequest request);

    List<VisitDashboardResponse> getDashboardVisits(
            VisitView view,
            String visitorId,
            String visitorName,
            String visitorEmail,
            VisitStatus status,
            LocalDate date,
            LocalDate fromDate,
            LocalDate toDate,
            String sortDirection,
            String sortBy
    );

    Page<VisitorLogResponse> getVisitorLog(
            String search,
            LocalDate fromDate,
            LocalDate toDate,
            VisitStatus status,
            VisitorType visitorType,
            String hostId,
            int page,
            int size,
            String sortDirection
    );

    VisitDetailResponse getVisitDetails(String visitId);

    VisitDetailResponse cancelVisit(String visitId);

    VisitDetailResponse checkIn(
            String visitId,
            CheckInRequest request,
            MultipartFile photo
    );

    VisitDetailResponse checkOut(String visitId);

    VisitTimeTrackingResponse getVisitTimeTracking(String visitId);

}