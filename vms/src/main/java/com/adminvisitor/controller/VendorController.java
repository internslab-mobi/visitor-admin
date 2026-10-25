
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

/**
 * REST controller for managing vendor information.
 *
 * <p>Provides APIs for retrieving all registered vendors,
 * retrieving an individual vendor by ID, and obtaining the
 * information required to populate the vendor edit page.</p>
 *
 * @author Anisha Z
 */
@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
@Tag(
        name = "Vendor Management",
        description = "APIs for retrieving vendor information and vendor edit details"
)
public class VendorController {

    private final VendorService vendorService;

    /**
     * Retrieves all vendors registered in the VMS application.
     *
     * @return the list of vendor details with HTTP status 200
     */

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


    /**
     * Retrieves the details of a specific vendor using its unique identifier.
     *
     * @param id the unique identifier of the vendor
     * @return the vendor details with HTTP status 200
     */
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


    /**
     * Retrieves the vendor information required to populate the vendor edit page.
     *
     * <p>Returns the edit-specific response for the vendor identified
     * by the supplied vendor ID.</p>
     *
     * @param id the unique identifier of the vendor
     * @return the vendor edit details with HTTP status 200
     */
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
