
package com.adminvisitor.controller;

import com.adminvisitor.dto.requestdto.UpdateVisitorRequest;

import com.adminvisitor.exception.ErrorResponseFactory;
import com.adminvisitor.service.VisitorService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VisitorController.class)
class VisitorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VisitorService visitorService;

    private static final String VISITOR_ID = "VIS001";
    @MockitoBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @MockitoBean
    private ErrorResponseFactory errorResponseFactory;
    @Test

    void getVisitor_shouldReturn200_whenGetMethodIsUsed() throws Exception {

        when(visitorService.getVisitor(VISITOR_ID))
                .thenReturn(null);

        mockMvc.perform(get("/api/visitors/{visitorId}", VISITOR_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(visitorService).getVisitor(VISITOR_ID);
    }

    @Test
    void getVisitor_shouldReturn405_whenPostMethodIsUsed()
            throws Exception {

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .post("/api/visitors/{visitorId}", VISITOR_ID))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(visitorService);
    }

    @Test
    void updateVisitor_shouldReturn200() throws Exception {

        when(visitorService.updateVisitor(
                eq(VISITOR_ID),
                any(UpdateVisitorRequest.class)))
                .thenReturn(null);

        String requestBody = """
            {
                "firstName": "Arun",
                "lastName": "Kumar",
                "email": "arun@example.com",
                "mobileNumber": "9876543210"
            }
            """;

        mockMvc.perform(put("/api/visitors/{visitorId}", VISITOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(visitorService).updateVisitor(
                eq(VISITOR_ID),
                any(UpdateVisitorRequest.class));
    }
    @Test
    void updateVisitor_shouldReturn400_whenRequestBodyIsMissing()
            throws Exception {

        mockMvc.perform(put("/api/visitors/{visitorId}", VISITOR_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(visitorService);
    }

    @Test
    void updateVisitor_shouldReturn400_whenJsonIsMalformed()
            throws Exception {

        mockMvc.perform(put("/api/visitors/{visitorId}", VISITOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(visitorService);
    }


    @Test
    void getAllVisitors_shouldReturn200_whenVisitorsExist()
            throws Exception {

        when(visitorService.getAllVisitors())
                .thenReturn(List.of());

        mockMvc.perform(get("/api/visitors")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(visitorService).getAllVisitors();
    }

    @Test
    void getAllVisitors_shouldReturn405_whenPostMethodIsUsed()
            throws Exception {

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .post("/api/visitors"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(visitorService);
    }

    @Test
    void getVisitorEditDetails_shouldReturn200()
            throws Exception {

        when(visitorService.getVisitorEditDetails(VISITOR_ID))
                .thenReturn(null);

        mockMvc.perform(get(
                        "/api/visitors/{visitorId}/edit",
                        VISITOR_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(visitorService).getVisitorEditDetails(VISITOR_ID);
    }

    @Test
    void getVisitorEditDetails_shouldReturn405_whenPostMethodIsUsed()
            throws Exception {

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .post("/api/visitors/{visitorId}/edit",
                                        VISITOR_ID))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(visitorService);
    }
}
