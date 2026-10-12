package com.adminvisitor.dto.responsedto;

import com.adminvisitor.entity.DocumentMetadata;

import java.time.LocalDateTime;

public record NdaLatestResult(
        DocumentMetadata metadata,
        LocalDateTime validFrom
) {}