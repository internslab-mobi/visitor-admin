package com.adminvisitor.service.impl;

import com.adminvisitor.dto.requestdto.ExportRequest;
import com.adminvisitor.service.SummaryExportClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
public class SummaryExportClientImpl implements SummaryExportClient {

    private final RestClient restClient;

    public SummaryExportClientImpl(
            @Value("${summary-export.service.export-url}") String exportUrl) {

        this.restClient = RestClient.create(exportUrl);
    }

    @Override
    public byte[] export(ExportRequest request) {

        try {
            byte[] file = restClient.post()
                    .uri("/api/exports")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_OCTET_STREAM)
                    .body(request)
                    .retrieve()
                    .body(byte[].class);

            if (file == null || file.length == 0) {
                log.warn("SUMMARY_EXPORT_EMPTY_RESPONSE");

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Export service returned an empty file"
                );
            }

            log.info(
                    "SUMMARY_EXPORT_SUCCEEDED fileSizeBytes={}",
                    file.length
            );

            return file;

        } catch (ResourceAccessException ex) {
            log.warn(
                    "SUMMARY_EXPORT_SERVICE_UNAVAILABLE exceptionType={}",
                    ex.getClass().getSimpleName()
            );

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Summary export service is unavailable",
                    ex
            );

        } catch (RestClientResponseException ex) {
            log.warn(
                    "SUMMARY_EXPORT_SERVICE_ERROR upstreamStatus={}",
                    ex.getStatusCode().value()
            );

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Summary export service failed to process the request",
                    ex
            );
        }
    }
}