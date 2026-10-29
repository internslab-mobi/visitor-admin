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

    String getVisitPhotoPath(String visitId);
}