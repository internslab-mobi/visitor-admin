
package com.adminvisitor.mapper;

import com.adminvisitor.dto.responsedto.DocumentResponse;
import com.adminvisitor.entity.DocumentMetadata;
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
}
