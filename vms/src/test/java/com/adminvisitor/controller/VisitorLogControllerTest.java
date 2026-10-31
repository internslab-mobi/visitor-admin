
package com.adminvisitor.controller;

import com.adminvisitor.dto.responsedto.VisitTimeTrackingResponse;
import com.adminvisitor.dto.responsedto.VisitorLogResponse;
import com.adminvisitor.enums.VisitStatus;
import com.adminvisitor.enums.VisitorType;
import com.adminvisitor.exception.ResourceNotFoundException;
import com.adminvisitor.exception.ErrorResponseFactory;
import com.adminvisitor.service.VisitManagementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for VisitorLogController.
 *
 * Tests HTTP responses, request parameter binding, pagination defaults,
 * filtering, and time-tracking retrieval without accessing the database.
 */
@WebMvcTest(VisitorLogController.class)
@AutoConfigureMockMvc(addFilters = false)
class VisitorLogControllerTest {

    private static final String VISITOR_LOG_URL = "/api/visitor-log";
    private static final String VISIT_ID = "VIS-001";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VisitManagementService visitManagementService;

    /**
     * Mocks the dependency required by the global exception handler.
     */
    @MockitoBean
    private ErrorResponseFactory errorResponseFactory;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMappingContext;

    /**
     * Verifies that the visitor log endpoint returns HTTP 200
     * when no matching records are found.
     */
    @Test
    @DisplayName("GET visitor log - should return 200 with empty page")
    void getVisitorLog_shouldReturnOk() throws Exception {
        when(visitManagementService.getVisitorLog(
                null, null, null, null, null, null, 0, 10, "DESC"
        )).thenReturn(Page.empty());

        mockMvc.perform(get(VISITOR_LOG_URL)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(visitManagementService).getVisitorLog(
                null, null, null, null, null, null, 0, 10, "DESC");
    }

    /**
     * Verifies that omitted pagination and sorting parameters use
     * the controller's configured defaults.
     */
    @Test
    @DisplayName("GET visitor log - should apply default pagination and sorting")
    void getVisitorLog_shouldUseDefaultParameters() throws Exception {
        when(visitManagementService.getVisitorLog(
                null, null, null, null, null, null, 0, 10, "DESC"
        )).thenReturn(Page.empty());

        mockMvc.perform(get(VISITOR_LOG_URL))
                .andExpect(status().isOk());

        verify(visitManagementService).getVisitorLog(
                null, null, null, null, null, null, 0, 10, "DESC");
    }

    /**
     * Verifies that search, date range, status, visitor type, host,
     * pagination, and sorting parameters are passed to the service.
     */
    @Test
    @DisplayName("GET visitor log - should pass all supplied filters")
    void getVisitorLog_shouldPassAllFilters() throws Exception {
        LocalDate fromDate = LocalDate.of(2026, 10, 1);
        LocalDate toDate = LocalDate.of(2026, 10, 25);

        when(visitManagementService.getVisitorLog(
                anyString(), any(LocalDate.class), any(LocalDate.class),
                any(VisitStatus.class), any(VisitorType.class),
                anyString(), anyInt(), anyInt(), anyString()
        )).thenReturn(Page.empty());

        mockMvc.perform(get(VISITOR_LOG_URL)
                        .param("search", "visitor")
                        .param("fromDate", "2026-10-01")
                        .param("toDate", "2026-10-25")
                        .param("status", "CHECKED_OUT")
                        .param("visitorType", "VISITOR")
                        .param("hostId", "EMP-001")
                        .param("page", "1")
                        .param("size", "20")
                        .param("sortDirection", "ASC"))
                .andExpect(status().isOk());

        verify(visitManagementService).getVisitorLog(
                "visitor", fromDate, toDate,
                VisitStatus.CHECKED_OUT, VisitorType.VISITOR,
                "EMP-001", 1, 20, "ASC");
    }

    /**
     * Verifies that a search-only request works without optional filters.
     */
    @Test
    @DisplayName("GET visitor log - should allow search without filters")
    void getVisitorLog_shouldAllowSearchOnly() throws Exception {
        when(visitManagementService.getVisitorLog(
                "John", null, null, null, null, null, 0, 10, "DESC"
        )).thenReturn(Page.empty());

        mockMvc.perform(get(VISITOR_LOG_URL)
                        .param("search", "John"))
                .andExpect(status().isOk());

        verify(visitManagementService).getVisitorLog(
                "John", null, null, null, null, null, 0, 10, "DESC");
    }

    /**
     * Verifies that an invalid date format is rejected during
     * Spring MVC request parameter conversion.
     */
    @Test
    @DisplayName("GET visitor log - should return 400 for invalid date")
    void getVisitorLog_withInvalidDate_shouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(get(VISITOR_LOG_URL)
                        .param("fromDate", "not-a-date"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(visitManagementService);
    }

    /**
     * Verifies that an unsupported enum value is rejected during
     * Spring MVC request parameter conversion.
     */
    @Test
    @DisplayName("GET visitor log - should return 400 for invalid status")
    void getVisitorLog_withInvalidStatus_shouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(get(VISITOR_LOG_URL)
                        .param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(visitManagementService);
    }

    /**
     * Verifies that a service-level not-found exception is translated
     * into HTTP 404 by the application's exception handler.
     */
    @Test
    @DisplayName("GET visitor log - should return 404 when resource is not found")
    void getVisitorLog_whenResourceNotFound_shouldReturnNotFound()
            throws Exception {

        when(visitManagementService.getVisitorLog(
                null, null, null, null, null, null, 0, 10, "DESC"
        )).thenThrow(new ResourceNotFoundException("Visitor log not found"));

        mockMvc.perform(get(VISITOR_LOG_URL))
                .andExpect(status().isNotFound());
    }

    /**
     * Verifies that an unexpected service exception results in HTTP 500.
     */
    @Test
    @DisplayName("GET visitor log - should return 500 for unexpected error")
    void getVisitorLog_whenUnexpectedErrorOccurs_shouldReturnInternalServerError()
            throws Exception {

        when(visitManagementService.getVisitorLog(
                null, null, null, null, null, null, 0, 10, "DESC"
        )).thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(get(VISITOR_LOG_URL))
                .andExpect(status().isInternalServerError());
    }

    /**
     * Verifies that the time-tracking endpoint returns HTTP 200
     * when the requested visit's tracking data is available.
     */
    @Test
    @DisplayName("GET time tracking - should return 200")
    void getVisitTimeTracking_shouldReturnOk() throws Exception {
        when(visitManagementService.getVisitTimeTracking(VISIT_ID))
                .thenReturn(mock(VisitTimeTrackingResponse.class));

        mockMvc.perform(get(VISITOR_LOG_URL + "/" + VISIT_ID
                        + "/time-tracking")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(visitManagementService).getVisitTimeTracking(VISIT_ID);
    }

    /**
     * Verifies that an unknown visit produces HTTP 404 when the service
     * throws the application's resource-not-found exception.
     */
    @Test
    @DisplayName("GET time tracking - should return 404 for unknown visit")
    void getVisitTimeTracking_whenVisitNotFound_shouldReturnNotFound()
            throws Exception {

        when(visitManagementService.getVisitTimeTracking(VISIT_ID))
                .thenThrow(new ResourceNotFoundException("Visit not found"));

        mockMvc.perform(get(VISITOR_LOG_URL + "/" + VISIT_ID
                        + "/time-tracking"))
                .andExpect(status().isNotFound());
    }

    /**
     * Verifies that an unexpected time-tracking service error produces
     * HTTP 500.
     */
    @Test
    @DisplayName("GET time tracking - should return 500 for unexpected error")
    void getVisitTimeTracking_whenUnexpectedErrorOccurs_shouldReturnInternalServerError()
            throws Exception {

        when(visitManagementService.getVisitTimeTracking(VISIT_ID))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(get(VISITOR_LOG_URL + "/" + VISIT_ID
                        + "/time-tracking"))
                .andExpect(status().isInternalServerError());
    }
}
