package com.adminvisitor.controller;

import com.adminvisitor.dto.requestdto.UpdateVendorRequest;
import com.adminvisitor.dto.responsedto.VendorResponse;
import com.adminvisitor.service.VendorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.adminvisitor.dto.responsedto.VendorEditResponse;
import java.util.List;

@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;

    @GetMapping
    public ResponseEntity<List<VendorResponse>> getAllVendors() {
        List<VendorResponse> vendors =
                vendorService.getAllVendors();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(vendors);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VendorResponse> getVendorById(
            @PathVariable String id
    ) {
        VendorResponse vendor =
                vendorService.getVendorById(id);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(vendor);
    }


//    @GetMapping("/{id}/edit")
//    public ResponseEntity<VendorEditResponse> getVendorEditDetails(
//            @PathVariable String id
//    ) {
//        VendorEditResponse response =
//                vendorService.getVendorEditDetails(id);
//
//        return ResponseEntity.ok(response);
//    }



}