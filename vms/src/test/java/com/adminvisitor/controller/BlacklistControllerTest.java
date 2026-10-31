
package com.adminvisitor.controller;

import com.adminvisitor.dto.requestdto.BlacklistRequest;
import com.adminvisitor.exception.ResourceNotFoundException;
import com.adminvisitor.exception.ErrorResponseFactory;
import com.adminvisitor.service.BlacklistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;

/**
 * Tests the BlacklistController endpoints using MockMvc.
 * Mocks service dependencies to test HTTP behavior independently
 * of the database and business logic.
 */
@WebMvcTest(BlacklistController.class)
@AutoConfigureMockMvc(addFilters = false)
class BlacklistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BlacklistService blacklistService;

    @MockitoBean
    private ErrorResponseFactory errorResponseFactory;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMappingContext;

    /**
     * Verifies that retrieving all blacklist records returns HTTP 200
     * and an empty JSON array when no records exist.
     */
    @Test
    void getAllBlacklistRecords_shouldReturnOk() throws Exception {

        when(blacklistService.getAllBlacklistRecords())
                .thenReturn(List.of());

        mockMvc.perform(get("/api/blacklist"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(blacklistService).getAllBlacklistRecords();
    }

    /**
     * Verifies that retrieving a blacklist record by ID returns HTTP 200.
     */
    @Test
    void getBlacklistById_shouldReturnOk() throws Exception {

        mockMvc.perform(get("/api/blacklist/{id}", "BL-001"))
                .andExpect(status().isOk());

        verify(blacklistService).getBlacklistById("BL-001");
    }

    /**
     * Verifies that adding an existing visitor to the blacklist
     * returns HTTP 201 Created and delegates the request to the service.
     */
    @Test
    void addExistingVisitorToBlacklist_shouldReturnCreated()
            throws Exception {

        String requestBody = """
                {
                    "reason": "Security policy violation",
                    "createdBy": "EMP-001"
                }
                """;

        mockMvc.perform(post("/api/blacklist/visitor/{visitorId}", "VTR-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        verify(blacklistService).addExistingVisitorToBlacklist(
                eq("VTR-001"),
                any(BlacklistRequest.class)
        );
    }

    /**
     * Verifies that removing a visitor from the blacklist returns HTTP 200
     * and passes the blacklist ID and remover ID to the service.
     */
    @Test
    void removeFromBlacklist_shouldReturnOk() throws Exception {

        mockMvc.perform(put("/api/blacklist/{id}/remove", "BL-001")
                        .param("removedBy", "EMP-001"))
                .andExpect(status().isOk());

        verify(blacklistService).removeFromBlacklist(
                "BL-001",
                "EMP-001"
        );
    }

    /**
     * Verifies that checking a visitor's blacklist status returns HTTP 200
     * with true when the visitor is blacklisted.
     */
    @Test
    void isBlacklisted_shouldReturnBoolean() throws Exception {

        when(blacklistService.isBlacklisted("VTR-001"))
                .thenReturn(true);

        mockMvc.perform(get(
                        "/api/blacklist/visitor/{visitorId}/status",
                        "VTR-001"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(blacklistService).isBlacklisted("VTR-001");
    }

    /**
     * Verifies that omitting the required removedBy parameter returns
     * HTTP 400 Bad Request without invoking the service.
     */
    @Test
    void removeFromBlacklist_withoutRemovedBy_shouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(put("/api/blacklist/{id}/remove", "BL-001"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(blacklistService);
    }

    /**
     * Verifies that requesting a nonexistent blacklist record returns
     * HTTP 404 Not Found when the service throws ResourceNotFoundException.
     */
    @Test
    void getBlacklistById_whenRecordDoesNotExist_shouldReturnNotFound()
            throws Exception {

        when(blacklistService.getBlacklistById("BL-999"))
                .thenThrow(new ResourceNotFoundException(
                        "Blacklist record not found"));

        mockMvc.perform(get("/api/blacklist/{id}", "BL-999"))
                .andExpect(status().isNotFound());
    }

    /**
     * Verifies that an unexpected service exception results in
     * HTTP 500 Internal Server Error.
     */
    @Test
    void getAllBlacklistRecords_whenUnexpectedErrorOccurs_shouldReturnInternalServerError()
            throws Exception {

        when(blacklistService.getAllBlacklistRecords())
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(get("/api/blacklist"))
                .andExpect(status().isInternalServerError());
    }
}
