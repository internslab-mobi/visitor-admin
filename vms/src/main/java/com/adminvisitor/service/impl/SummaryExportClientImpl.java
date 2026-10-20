
package com.adminvisitor.service.impl;

import com.adminvisitor.dto.requestdto.ExportRequest;
import com.adminvisitor.service.SummaryExportClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class SummaryExportClientImpl implements SummaryExportClient {

    private final RestClient restClient;

    public SummaryExportClientImpl(

            @Value("${summary-export.service.export-url}") String exportUrl)
     {

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
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Export service returned an empty file"
                );
            }

            return file;

        } catch (ResourceAccessException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Summary export service is unavailable",
                    ex
            );

        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Summary export service failed to process the request",
                    ex
            );
        }
    }

}
