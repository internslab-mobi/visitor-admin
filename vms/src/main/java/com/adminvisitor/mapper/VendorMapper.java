package com.adminvisitor.mapper;

import com.adminvisitor.dto.responsedto.VendorEditResponse;
import com.adminvisitor.dto.responsedto.VendorResponse;
import com.adminvisitor.dto.responsedto.VisitorEditResponse;
import com.adminvisitor.entity.Vendor;
import com.adminvisitor.entity.Visitor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VendorMapper {

    // Existing vendor list/detail mapping
    @Mapping(source = "visitor.id", target = "visitorId")
    VendorResponse toResponse(Vendor vendor);

    // Vendor information for the edit response
    @Mapping(source = "id", target = "vendorId")
    @Mapping(source = "visitor.id", target = "visitorId")
    VendorEditResponse.VendorInfo toVendorInfo(Vendor vendor);

    // Visitor information
    VendorEditResponse.VisitorInfo toVisitorInfo(
            VisitorEditResponse.VisitorInfo visitor
    );

    // Document mapping
    @Mapping(target = "documentPath", ignore = true)
    VendorEditResponse.DocumentInfo toDocumentInfo(
            VisitorEditResponse.DocumentInfo document
    );

    List<VendorEditResponse.DocumentInfo> toDocumentInfoList(
            List<VisitorEditResponse.DocumentInfo> documents
    );

    // Visit mapping
    VendorEditResponse.VisitInfo toVisitInfo(
            VisitorEditResponse.VisitInfo visit
    );

    List<VendorEditResponse.VisitInfo> toVisitInfoList(
            List<VisitorEditResponse.VisitInfo> visits
    );

    // Blacklist mapping
    VendorEditResponse.BlacklistInfo toBlacklistInfo(
            VisitorEditResponse.BlacklistInfo blacklist
    );



    @Mapping(target = "vendor", source = "vendor")
    @Mapping(target = "visitor", source = "visitorEdit.visitor")
    @Mapping(target = "documents", source = "visitorEdit.documents")
    @Mapping(target = "ndas", source = "ndas")
    @Mapping(target = "visits", source = "visitorEdit.visits")
    @Mapping(target = "blacklist", source = "visitorEdit.blacklist")
    @Mapping(target = "blacklisted", source = "visitorEdit.blacklisted")
    VendorEditResponse toEditResponse(
            Vendor vendor,
            VisitorEditResponse visitorEdit,
            List<VendorEditResponse.NdaInfo> ndas
    );




        @Mapping(target = "id", source = "vendorId")
        @Mapping(target = "visitor", source = "visitor")
        @Mapping(target = "firstName", source = "visitor.firstName")
        @Mapping(target = "lastName", source = "visitor.lastName")
        @Mapping(target = "email", source = "visitor.email")
        @Mapping(target = "mobileNumber", source = "visitor.mobileNumber")
        @Mapping(target = "companyName", source = "visitor.companyName")
        Vendor toEntity(Visitor visitor, String vendorId);

}
