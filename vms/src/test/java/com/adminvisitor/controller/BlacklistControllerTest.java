package com.adminvisitor.controller;

import com.adminvisitor.dto.requestdto.AddVisitorToBlacklistRequest;
import com.adminvisitor.dto.requestdto.BlacklistRequest;
import com.adminvisitor.dto.responsedto.BlacklistResponse;
import com.adminvisitor.enums.BlacklistStatus;
import com.adminvisitor.enums.Nationality;
import com.adminvisitor.enums.VisitorType;
import com.adminvisitor.service.BlacklistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BlacklistController.class)
class BlacklistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BlacklistService blacklistService;

    // Test methods will be added step by step
    @Test
    void getAllBlacklistRecords_ShouldReturnOk() throws Exception {

        when(blacklistService.getAllBlacklistRecords())
                .thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/blacklist"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getBlacklistById_ShouldReturnBlacklistRecord() throws Exception {

        LocalDateTime now = LocalDateTime.now();

        BlacklistResponse response = new BlacklistResponse(
                "BL-001",
                "VTR-001",
                "John",
                "john@example.com",
                "9876543210",
                "ABC Company",
                Nationality.DOMESTIC,
                "Security violation",
                BlacklistStatus.ACTIVE,
                "admin",
                now,
                now,
                null,
                VisitorType.VISITOR
        );

        when(blacklistService.getBlacklistById("BL-001"))
                .thenReturn(response);

        mockMvc.perform(get("/api/blacklist/BL-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("BL-001"))
                .andExpect(jsonPath("$.visitorName").value("John"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void removeFromBlacklist_ShouldReturnUpdatedRecord() throws Exception {

        LocalDateTime now = LocalDateTime.now();

        BlacklistResponse response = new BlacklistResponse(
                "BL-001",
                "VTR-001",
                "John Doe",
                "john@example.com",
                "9876543210",
                "ABC Company",
                Nationality.DOMESTIC,
                "Security violation",
                BlacklistStatus.REMOVED,
                "admin",
                now,
                now,
                "admin",
                VisitorType.VISITOR
        );

        when(blacklistService.removeFromBlacklist("BL-001", "admin"))
                .thenReturn(response);

        mockMvc.perform(
                        put("/api/blacklist/BL-001/remove")
                                .param("removedBy", "admin")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("BL-001"))
                .andExpect(jsonPath("$.status").value("REMOVED"))
                .andExpect(jsonPath("$.updatedBy").value("admin"));
    }

    @Test
    void addExistingVisitorToBlacklist_ShouldReturnCreated()
            throws Exception {

        LocalDateTime now = LocalDateTime.now();

        BlacklistResponse response = new BlacklistResponse(
                "BL-001",
                "VTR-001",
                "John Doe",
                "john@example.com",
                "9876543210",
                "ABC Company",
                Nationality.DOMESTIC,
                "Security violation",
                BlacklistStatus.ACTIVE,
                "EMP001",
                now,
                now,
                null,
                VisitorType.VISITOR
        );

        when(blacklistService.addExistingVisitorToBlacklist(
                org.mockito.ArgumentMatchers.eq("VTR-001"),
                any(AddVisitorToBlacklistRequest.class)
        )).thenReturn(response);

        mockMvc.perform(
                        post("/api/blacklist/visitor/VTR-001")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                    {
                                        "reason": "Security violation",
                                        "createdBy": "EMP001"
                                    }
                                    """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("BL-001"))
                .andExpect(jsonPath("$.visitorId").value("VTR-001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void addExistingVisitorToBlacklist_WhenReasonIsBlank_ShouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(
                        post("/api/blacklist/visitor/VTR-001")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                    {
                                        "reason": "",
                                        "createdBy": "admin"
                                    }
                                    """)
                )
                .andExpect(status().isBadRequest());

        verify(blacklistService, never())
                .addExistingVisitorToBlacklist(
                        anyString(),
                        any(AddVisitorToBlacklistRequest.class)
                );
    }

    @Test
    void addToBlacklist_ShouldReturnCreated() throws Exception {

        LocalDateTime now = LocalDateTime.now();

        BlacklistResponse response = new BlacklistResponse(
                "BL-002",
                "VTR-002",
                "Jane Doe",
                "jane@example.com",
                "9876543210",
                "XYZ Company",
                Nationality.DOMESTIC,
                "Security violation",
                BlacklistStatus.ACTIVE,
                "admin",
                now,
                now,
                null,
                VisitorType.VISITOR
        );

        when(blacklistService.addToBlacklist(
                any(BlacklistRequest.class)
        )).thenReturn(response);

        mockMvc.perform(
                        post("/api/blacklist")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                    {
                                        "visitorId": "VTR-002",
                                        "nationality": "DOMESTIC",
                                        "proofType": "AADHAAR",
                                        "proofNumber": "123456789012",
                                        "reason": "Security violation",
                                        "createdBy": "admin"
                                    }
                                    """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("BL-002"))
                .andExpect(jsonPath("$.visitorId").value("VTR-002"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void addToBlacklist_WhenVisitorIdIsMissing_ShouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(
                        post("/api/blacklist")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                    {
                                        "nationality": "DOMESTIC",
                                        "proofType": "AADHAAR",
                                        "proofNumber": "123456789012",
                                        "reason": "Security violation",
                                        "createdBy": "admin"
                                    }
                                    """)
                )
                .andExpect(status().isBadRequest());

        verify(blacklistService, never())
                .addToBlacklist(any(BlacklistRequest.class));
    }
}