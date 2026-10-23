
package com.adminvisitor.controller;

import com.adminvisitor.dto.responsedto.VendorResponse;
import com.adminvisitor.dto.responsedto.VendorEditResponse;
import com.adminvisitor.service.VendorService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;


import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
@Tag(
        name = "Vendor Management",
        description = "APIs for retrieving vendor information and vendor edit details"
)
public class VendorController {

    private final VendorService vendorService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get all vendors",
            description = "Retrieves all vendors registered in the VMS application."

    )

    public ResponseEntity<List<VendorResponse>> getAllVendors() {

        List<VendorResponse> vendors =
                vendorService.getAllVendors();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(vendors);
    }

    @GetMapping(
            value = "/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get vendor by ID",
            description = "Retrieves the details of a specific vendor using its unique identifier."

    )

    public ResponseEntity<VendorResponse> getVendorById(

            @Parameter(
                    name = "id",
                    description = "Unique identifier of the vendor",
                    required = true,
                    example = "VEND001"
            )
            @PathVariable("id") String id

    ) {
        VendorResponse vendor =
                vendorService.getVendorById(id);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(vendor);
    }

    @GetMapping(
            value = "/{id}/edit",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get vendor edit details",
            description = "Retrieves the vendor information required to populate the vendor edit page."
            )

    public ResponseEntity<VendorEditResponse> getVendorEditDetails(

            @Parameter(
                    name = "id",
                    description = "Unique identifier of the vendor",
                    required = true,
                    example = "VEND001"
            )
            @PathVariable("id") String id

    ) {
        VendorEditResponse response =
                vendorService.getVendorEditDetails(id);

        return ResponseEntity.ok(response);
    }
}
