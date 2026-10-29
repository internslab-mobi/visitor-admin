
package com.adminvisitor.repository;

import com.adminvisitor.dto.responsedto.VisitTimeTrackingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class VisitTrackingQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<VisitTimeTrackingResponse.SessionResponse>
    findByVisitIdOrderByInTimeAsc(
            String visitId,
            LocalDateTime referenceTime) {

        String sql = """
                SELECT id, badge_id, in_time, out_time
                FROM vms_meta_time_tracking
                WHERE visit_id = ?
                ORDER BY in_time ASC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {

            Timestamp inTimestamp = rs.getTimestamp("in_time");
            Timestamp outTimestamp = rs.getTimestamp("out_time");

            LocalDateTime inTime = inTimestamp != null
                    ? inTimestamp.toLocalDateTime()
                    : null;

            LocalDateTime outTime = outTimestamp != null
                    ? outTimestamp.toLocalDateTime()
                    : null;

            long durationSeconds = inTime == null
                    ? 0L
                    : Duration.between(
                    inTime,
                    outTime != null ? outTime : referenceTime
            ).getSeconds();

            return new VisitTimeTrackingResponse.SessionResponse(
                    rs.getObject("id", Long.class),
                    rs.getString("badge_id"),
                    inTime,
                    outTime,
                    durationSeconds
            );

        }, visitId);
    }
}
