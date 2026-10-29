package com.adminvisitor.service;

import com.adminvisitor.dto.responsedto.DocumentResponse;
import com.adminvisitor.dto.responsedto.IdentityProofResponse;
import com.adminvisitor.entity.Document;
import com.adminvisitor.entity.DocumentMetadata;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.enums.Nationality;
import com.adminvisitor.enums.ProofType;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    DocumentMetadata uploadVisitorPhoto(
            String visitorId,
            String visitId,
            MultipartFile file
    );

    DocumentMetadata getLatestVisitorPhoto(String visitorId);


    Document saveIdentityProofs(
            Visitor visitor,
            Nationality nationality,
            String aadharNumber,
            String panNumber,
            String passportNumber);

    Document getLatestIdentityDocument(String visitorId);

    boolean verifyIdentityProof(
            Visitor visitor,
            ProofType proofType,
            String proofNumber);

    DocumentMetadata getVisitPhoto(String visitId);

    List<DocumentResponse> getAllDocuments(String visitorId);

    List<DocumentMetadata> uploadProofDocuments(
            String visitorId,
            List<MultipartFile> files);

    DocumentMetadata getDocumentMetadata(String documentId);

    void deleteDocument(String metadataId);
}