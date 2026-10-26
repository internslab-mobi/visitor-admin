
package com.adminvisitor.controller;

import com.adminvisitor.service.VendorService;
import com.adminvisitor.exception.ErrorResponseFactory;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;

import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VendorController.class)
class VendorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VendorService vendorService;

    private static final String VENDOR_ID = "VND001";

    @MockitoBean
    private JpaMetamodelMappingContext jpaMappingContext;
    @MockitoBean
    private ErrorResponseFactory errorResponseFactory;
    @Test
    void getAllVendors_shouldReturn200() throws Exception {

        when(vendorService.getAllVendors())
                .thenReturn(List.of());

        mockMvc.perform(get("/api/vendors")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(vendorService).getAllVendors();
    }

    @Test
    void getAllVendors_shouldReturn405_whenPostMethodIsUsed()
            throws Exception {

        mockMvc.perform(post("/api/vendors"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(vendorService);
    }

    @Test
    void getVendorById_shouldReturn200() throws Exception {

        when(vendorService.getVendorById(VENDOR_ID))
                .thenReturn(null);

        mockMvc.perform(get("/api/vendors/{id}", VENDOR_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(vendorService).getVendorById(VENDOR_ID);
    }

    @Test
    void getVendorById_shouldReturn405_whenPostMethodIsUsed()
            throws Exception {

        mockMvc.perform(post("/api/vendors/{id}", VENDOR_ID))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(vendorService);
    }

    @Test
    void getVendorEditDetails_shouldReturn200()
            throws Exception {

        when(vendorService.getVendorEditDetails(VENDOR_ID))
                .thenReturn(null);

        mockMvc.perform(get(
                        "/api/vendors/{id}/edit",
                        VENDOR_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(vendorService).getVendorEditDetails(VENDOR_ID);
    }

    @Test
    void getVendorEditDetails_shouldReturn405_whenPostMethodIsUsed()
            throws Exception {

        mockMvc.perform(post(
                        "/api/vendors/{id}/edit",
                        VENDOR_ID))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(vendorService);
    }

    @Test
    void getVendorEditDetails_shouldReturn405_whenPutMethodIsUsed()
            throws Exception {

        mockMvc.perform(put(
                        "/api/vendors/{id}/edit",
                        VENDOR_ID))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(vendorService);
    }
}
