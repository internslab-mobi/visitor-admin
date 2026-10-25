//package com.adminvisitor.mapper;
//
//import com.adminvisitor.dto.responsedto.IdentityProofResponse;
//import com.adminvisitor.entity.Document;
//import org.mapstruct.Mapper;
//import org.mapstruct.Mapping;
//
//@Mapper(componentModel = "spring")
//public interface IdentityProofMapper {
//
//    @Mapping(source = "id", target = "documentId")
//    @Mapping(source = "visitor.id", target = "visitorId")
//    @Mapping(
//            expression = "java(document.getNationality() == null ? null : document.getNationality().name())",
//            target = "nationality"
//    )
//    @Mapping(target = "aadharNumber", ignore = true)
//    @Mapping(target = "panNumber", ignore = true)
//    @Mapping(target = "passportNumber", ignore = true)
//    IdentityProofResponse toResponse(Document document);
//}