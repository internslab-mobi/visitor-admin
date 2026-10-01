package com.adminvisitor.controller;

import com.adminvisitor.dto.responsedto.VendorResponse;
import com.adminvisitor.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;

    @GetMapping
    public ResponseEntity<List<VendorResponse>> getAllVendors() {

        return ResponseEntity.ok(vendorService.getAllVendors());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VendorResponse> getVendorById(
            @PathVariable String id) {

        return ResponseEntity.ok(vendorService.getVendorById(id));
    }
}