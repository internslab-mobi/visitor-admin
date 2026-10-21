
package com.adminvisitor.controller;

import com.adminvisitor.dto.requestdto.AddVisitorToBlacklistRequest;
import com.adminvisitor.exception.ResourceNotFoundException;
import com.adminvisitor.service.BlacklistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.adminvisitor.exception.ErrorResponseFactory;

@WebMvcTest(BlacklistController.class)
@AutoConfigureMockMvc(addFilters = false)
class BlacklistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BlacklistService blacklistService;

    @MockitoBean
    private ErrorResponseFactory errorResponseFactory;

    // Test 1: Get all blacklist records
    @Test
    void getAllBlacklistRecords_shouldReturnOk() throws Exception {

        when(blacklistService.getAllBlacklistRecords())
                .thenReturn(List.of());

        mockMvc.perform(get("/api/blacklist"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(blacklistService).getAllBlacklistRecords();
    }

    // Test 2: Get a blacklist record by ID
    @Test
    void getBlacklistById_shouldReturnOk() throws Exception {

        mockMvc.perform(get("/api/blacklist/{id}", "BL-001"))
                .andExpect(status().isOk());

        verify(blacklistService).getBlacklistById("BL-001");
    }

    // Test 3: Add an existing visitor to the blacklist
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
                any(AddVisitorToBlacklistRequest.class)
        );
    }

    // Test 4: Remove a visitor from the blacklist
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

    // Test 5: Check whether a visitor is blacklisted
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


    @Test
    void removeFromBlacklist_withoutRemovedBy_shouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(put("/api/blacklist/{id}/remove", "BL-001"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(blacklistService);
    }


    @Test
    void getBlacklistById_whenRecordDoesNotExist_shouldReturnNotFound()
            throws Exception {

        when(blacklistService.getBlacklistById("BL-999"))
                .thenThrow(new ResourceNotFoundException(
                        "Blacklist record not found"));

        mockMvc.perform(get("/api/blacklist/{id}", "BL-999"))
                .andExpect(status().isNotFound());
    }


    @Test
    void getAllBlacklistRecords_whenUnexpectedErrorOccurs_shouldReturnInternalServerError()
            throws Exception {

        when(blacklistService.getAllBlacklistRecords())
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(get("/api/blacklist"))
                .andExpect(status().isInternalServerError());
    }
}
