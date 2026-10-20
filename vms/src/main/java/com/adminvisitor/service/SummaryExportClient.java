package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.ExportRequest;

public interface SummaryExportClient {

    byte[] export(ExportRequest request);
}