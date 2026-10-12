package com.adminvisitor.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

public interface DocumentStorageService {

    Path store(
            MultipartFile file,
            String baseDirectory,
            String visitorId,
            String metadataId
    ) throws IOException;
}