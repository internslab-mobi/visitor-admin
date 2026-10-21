
package com.adminvisitor.service;

import com.adminvisitor.dto.responsedto.DocumentResponse;
import com.adminvisitor.entity.DocumentMetadata;
import com.adminvisitor.entity.Visitor;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface NdaService {

    DocumentMetadata uploadNewNda(
            String visitorId,
            MultipartFile ndaFile,
            LocalDate validUntil
    );

    DocumentMetadata extendNdaValidity(
            String visitorId,
            MultipartFile supportingDocument,
            LocalDate newValidUntil
    );

    DocumentMetadata getLatestNda(String visitorId);

    List<DocumentResponse> getNdaHistory(String visitorId);

    boolean isNdaRequired(Visitor visitor);
    Resource getNdaFile(String visitorId);

}
