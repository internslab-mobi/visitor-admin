package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.UpdateVisitorRequest;
import com.adminvisitor.dto.responsedto.VisitorEditResponse;
import com.adminvisitor.dto.responsedto.VisitorResponse;
import com.adminvisitor.entity.Visitor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VisitorService {

    VisitorResponse getVisitor(String visitorId);

    List<VisitorResponse> getAllVisitors();

    VisitorResponse updateVisitor(
            String visitorId,
            UpdateVisitorRequest request);

    VisitorEditResponse getVisitorEditDetails(String visitorId);

    VisitorResponse findVisitorByContact(String email, String mobileNumber);
}
