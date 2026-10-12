package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.UpdateVisitorRequest;
import com.adminvisitor.dto.responsedto.VisitorEditResponse;
import com.adminvisitor.dto.responsedto.VisitorResponse;

import java.util.List;

public interface VisitorService {

    VisitorResponse getVisitor(String visitorId);

    List<VisitorResponse> getAllVisitors();

    VisitorResponse updateVisitor(
            String visitorId,
            UpdateVisitorRequest request);

    VisitorEditResponse getVisitorEditDetails(String visitorId);
}
