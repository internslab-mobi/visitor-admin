package com.adminvisitor.service.impl;

import com.adminvisitor.service.DocumentStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
public class DocumentStorageServiceImpl
        implements DocumentStorageService {

    @Override
    public Path store(
            MultipartFile file,
            String baseDirectory,
            String visitorId,
            String metadataId
    ) throws IOException {

        Path visitorDirectory = Paths.get(
                baseDirectory,
                visitorId
        );

        Files.createDirectories(visitorDirectory);

        String originalFileName = file.getOriginalFilename();
        String extension = "";

        if (originalFileName != null) {
            String fileName = Paths.get(originalFileName)
                    .getFileName()
                    .toString();

            int dotIndex = fileName.lastIndexOf('.');

            if (dotIndex >= 0) {
                String candidate = fileName.substring(dotIndex);

                if (candidate.matches("\\.[a-zA-Z0-9]{1,10}")) {
                    extension = candidate;
                }
            }
        }

        Path filePath = visitorDirectory.resolve(
                metadataId + extension
        );

        Files.write(filePath, file.getBytes());

        log.debug(
                "DOCUMENT_FILE_STORED metadataId={} visitorId={} fileSizeBytes={}",
                metadataId,
                visitorId,
                file.getSize()
        );

        return filePath;
    }
}