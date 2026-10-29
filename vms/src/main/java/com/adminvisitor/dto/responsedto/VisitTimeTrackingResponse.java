
package com.adminvisitor.dto.responsedto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
@Schema(description = "QR-based IN and OUT history for a visit")
public record VisitTimeTrackingResponse(

        String visitId,

        List<SessionResponse> sessions,

        Long totalDurationSeconds
) {

    @Schema(description = "Individual visitor IN/OUT session")
    public record SessionResponse(

            Long trackingId,

            String badgeId,

            LocalDateTime inTime,

            LocalDateTime outTime,

            Long durationSeconds
    ) {
    }
}
