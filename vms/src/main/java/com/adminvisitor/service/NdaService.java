
package com.adminvisitor.service;

import com.adminvisitor.dto.responsedto.DocumentResponse;
import com.adminvisitor.dto.responsedto.NdaExtensionResult;
import com.adminvisitor.dto.responsedto.NdaLatestResult;
import com.adminvisitor.entity.DocumentMetadata;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface NdaService {

    DocumentMetadata uploadNewNda(
            String visitorId,
            MultipartFile ndaFile,
            LocalDate validUntil
    );

    NdaExtensionResult extendNdaValidity(
            String visitorId,
            MultipartFile supportingDocument,
            LocalDate newValidUntil
    );

    NdaLatestResult getLatestNda(String visitorId);

    List<DocumentResponse> getNdaHistory(String visitorId);
}
