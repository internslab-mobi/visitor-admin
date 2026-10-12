package com.adminvisitor.service;

import com.adminvisitor.dto.responsedto.VendorEditResponse;
import com.adminvisitor.dto.responsedto.VendorResponse;
import java.util.List;

public interface VendorService {

    List<VendorResponse> getAllVendors();

    VendorResponse getVendorById(String id);

    VendorEditResponse getVendorEditDetails(String vendorId);
}