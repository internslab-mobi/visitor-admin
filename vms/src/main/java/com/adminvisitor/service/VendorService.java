package com.adminvisitor.service;

import com.adminvisitor.dto.responsedto.VendorResponse;
import com.adminvisitor.entity.Vendor;
import com.adminvisitor.exception.VisitorNotFoundException;
import com.adminvisitor.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;

    public List<VendorResponse> getAllVendors() {

        return vendorRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public VendorResponse getVendorById(String id) {

        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() ->
                        new VisitorNotFoundException("Vendor not found with id: " + id));

        return mapToResponse(vendor);
    }

    private VendorResponse mapToResponse(Vendor vendor) {

        return new VendorResponse(
                vendor.getId(),
                vendor.getVisitor().getId(),
                vendor.getFirstName(),
                vendor.getLastName(),
                vendor.getEmail(),
                vendor.getMobileNumber(),
                vendor.getCompanyName(),
                vendor.getValidity()
        );
    }
}