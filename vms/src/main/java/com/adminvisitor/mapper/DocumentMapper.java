
package com.adminvisitor.mapper;

import com.adminvisitor.dto.responsedto.DocumentResponse;
import com.adminvisitor.entity.DocumentMetadata;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(source = "id", target = "documentId")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(target = "validFrom", ignore = true)
    @Mapping(target = "validUntil", ignore = true)
    @Mapping(target = "overwrittenBy", ignore = true)
    DocumentResponse toResponse(DocumentMetadata metadata);

    default String map(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toString();
    }
}
