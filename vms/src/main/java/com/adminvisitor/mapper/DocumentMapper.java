
package com.adminvisitor.mapper;

import com.adminvisitor.dto.responsedto.DocumentResponse;
import com.adminvisitor.entity.Document;
import com.adminvisitor.entity.DocumentMetadata;
import com.adminvisitor.entity.Visit;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.enums.Nationality;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(source = "id", target = "documentId")
    @Mapping(source = "documentPath", target = "documentPath")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "effective_validity", target = "effective_validity")
    @Mapping(source = "validUntil", target = "validUntil")
    @Mapping(source = "overwrittenBy", target = "overwrittenBy")
    @Mapping(target = "status", expression =
            "java(metadata.getStatus() == null ? null : metadata.getStatus().name())")
    DocumentResponse toResponse(DocumentMetadata metadata);

    default String map(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toString();
    }

    @Mapping(target = "id", source = "metadataId")
    @Mapping(target = "document", source = "document")
    @Mapping(target = "visit", source = "visit")
    @Mapping(target = "documentType", constant = "VISITOR_PHOTO")
    @Mapping(target = "documentPath", source = "documentPath")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "validUntil", ignore = true)
    @Mapping(target = "overwrittenBy", ignore = true)
    @Mapping(target = "effective_validity", ignore = true)
    @Mapping(target = "status", ignore = true)
    DocumentMetadata toVisitorPhotoMetadata(
            String metadataId,
            Document document,
            Visit visit,
            String documentPath
    );


    @Mapping(target = "id", source = "documentId")
    @Mapping(target = "visitor", source = "visitor")
    @Mapping(target = "nationality", source = "nationality")
    @Mapping(target = "aadharNumber", source = "aadharNumber")
    @Mapping(target = "panNumber", source = "panNumber")
    @Mapping(target = "passportNumber", source = "passportNumber")
    @Mapping(target = "metadata", ignore = true)
    Document toIdentityProofDocument(
            String documentId,
            Visitor visitor,
            Nationality nationality,
            String aadharNumber,
            String panNumber,
            String passportNumber
    );

    @Mapping(target = "id", source = "metadataId")
    @Mapping(target = "document", source = "document")
    @Mapping(target = "documentPath", source = "documentPath")

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    DocumentMetadata toProofDocumentMetadata(
            String metadataId,
            Document document,
            String documentPath
    );
}
