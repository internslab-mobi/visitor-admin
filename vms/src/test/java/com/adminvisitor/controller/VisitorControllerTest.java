package com.adminvisitor.controller;

import com.adminvisitor.dto.requestdto.UpdateVisitorRequest;
import com.adminvisitor.dto.responsedto.VisitorEditResponse;
import com.adminvisitor.dto.responsedto.VisitorResponse;
import com.adminvisitor.enums.DocumentType;
import com.adminvisitor.enums.Nationality;
import com.adminvisitor.enums.RegistrationType;
import com.adminvisitor.enums.VisitorType;
import com.adminvisitor.enums.VisitStatus;
import com.adminvisitor.service.VisitorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VisitorController.class)
class VisitorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VisitorService visitorService;

    private final ObjectMapper objectMapper =
            new ObjectMapper().findAndRegisterModules();


    

    

    @Test
    void getVisitor_shouldReturn200_whenVisitorExists() throws Exception {

        String visitorId = "VTR-001";

        VisitorResponse response = new VisitorResponse(
                "VTR-001",
                "John",
                "Doe",
                "john@example.com",
                "9876543210",
                "ABC Company",
                "VISITOR",
                LocalDateTime.now()
        );

        when(visitorService.getVisitor(visitorId))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/visitors/{visitorId}", visitorId)
                )
                .andExpect(status().isOk());
    }


    


    @Test
    void updateVisitor_shouldReturn200_whenRequestIsValid()
            throws Exception {

        String visitorId = "VTR-001";

        UpdateVisitorRequest request = new UpdateVisitorRequest(
                "John",
                "Doe",
                "john@example.com",
                "9876543210",
                "ABC Company"
        );

        VisitorResponse response = new VisitorResponse(
                "VTR-001",
                "John",
                "Doe",
                "john@example.com",
                "9876543210",
                "ABC Company",
                "VISITOR",
                LocalDateTime.now()
        );

        when(visitorService.updateVisitor(
                eq(visitorId),
                any(UpdateVisitorRequest.class)
        )).thenReturn(response);

        mockMvc.perform(
                        put("/api/visitors/{visitorId}", visitorId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk());
    }


    

    

    @Test
    void getAllVisitors_shouldReturn200() throws Exception {

        VisitorResponse response = new VisitorResponse(
                "VTR-001",
                "John",
                "Doe",
                "john@example.com",
                "9876543210",
                "ABC Company",
                "VISITOR",
                LocalDateTime.now()
        );

        when(visitorService.getAllVisitors())
                .thenReturn(List.of(response));

        mockMvc.perform(
                        get("/api/visitors")
                )
                .andExpect(status().isOk());
    }


    


    @Test
    void getVisitorEditDetails_shouldReturn200_whenVisitorExists()
            throws Exception {

        String visitorId = "VTR-001";

        


        VisitorEditResponse.VisitorInfo visitorInfo =
                new VisitorEditResponse.VisitorInfo(
                        "VTR-001",
                        "John",
                        "Doe",
                        "john@example.com",
                        "9876543210",
                        "ABC Company",
                        VisitorType.VISITOR,
                        Nationality.DOMESTIC
                );


        


        VisitorEditResponse.VendorInfo vendorInfo =
                new VisitorEditResponse.VendorInfo(
                        null
                );


        


        VisitorEditResponse.DocumentInfo documentInfo =
                new VisitorEditResponse.DocumentInfo(
                        "DOC-001",
                        DocumentType.AADHAAR,
                        "2026-10-01T10:00:00"
                );





        LocalDateTime arrival =
                LocalDateTime.of(2026, 10, 1, 10, 0);

        LocalDateTime departure =
                LocalDateTime.of(2026, 10, 1, 12, 0);

        VisitorEditResponse.VisitInfo visitInfo =
                new VisitorEditResponse.VisitInfo(
                        "VIS-001",
                        "VIS-001",
                        VisitorType.VISITOR,
                        RegistrationType.PRE_REGISTRATION,
                        "Business meeting",
                        "EMP-001",
                        "Jane Smith",
                        "DEP-001",
                        "HR",
                        arrival,
                        departure,
                        null,
                        null,
                        "First visit",
                        VisitStatus.REGISTERED
                );


        


        VisitorEditResponse.BlacklistInfo blacklistInfo = null;


        


        VisitorEditResponse response =
                new VisitorEditResponse(
                        visitorInfo,
                        vendorInfo,
                        List.of(documentInfo),
                        List.of(visitInfo),
                        blacklistInfo,
                        false
                );


        when(visitorService.getVisitorEditDetails(visitorId))
                .thenReturn(response);


        mockMvc.perform(
                        get(
                                "/api/visitors/{visitorId}/edit",
                                visitorId
                        )
                )
                .andExpect(status().isOk());
    }
}