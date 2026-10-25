package com.adminvisitor.service.impl;

import com.adminvisitor.service.DocumentStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service implementation for storing uploaded visitor documents.
 *
 * <p>Creates visitor-specific directories, extracts a valid file extension
 * from the original filename, and stores uploaded files using their
 * document metadata identifiers as filenames.</p>
 *
 * <p>Uses the configured base directory and visitor identifier to organize
 * files and logs successful storage operations for debugging purposes.</p>
 *
 * @author Anisha Z
 */
@Service
@Slf4j
public class DocumentStorageServiceImpl
        implements DocumentStorageService {



    /**
     * Stores an uploaded document in a visitor-specific directory.
     *
     * <p>Creates the visitor directory if it does not exist, extracts the
     * file extension from the original filename when it matches the accepted
     * format, and saves the file using the supplied metadata identifier.</p>
     *
     * @param file the uploaded file to store
     * @param baseDirectory the base directory in which visitor files are stored
     * @param visitorId the unique identifier of the visitor
     * @param metadataId the unique identifier used as the stored filename
     * @return the path to the stored file
     * @throws IOException if an I/O error occurs while creating directories
     *                     or writing the uploaded file
     */
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