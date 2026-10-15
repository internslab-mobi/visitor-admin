package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.CheckInRequest;
import com.adminvisitor.dto.requestdto.RegistrationRequest;
import com.adminvisitor.dto.responsedto.*;
import com.adminvisitor.enums.VisitStatus;
import com.adminvisitor.enums.VisitView;
import com.adminvisitor.enums.VisitorType;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface VisitService {

    RegistrationResponse register(RegistrationRequest request);

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

//    NdaStatusResponse getNdaStatus(String visitId);
//
    void verifyIdentity(String visitId, CheckInRequest request);

    String getVisitPhotoPath(String visitId);
}