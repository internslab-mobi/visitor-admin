package com.adminvisitor.service;

import com.adminvisitor.entity.Visit;
import com.adminvisitor.entity.VisitBadge;
import com.adminvisitor.enums.BadgeStatus;
import com.adminvisitor.repository.VisitBadgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class VisitBadgeService {

    private final VisitBadgeRepository visitBadgeRepository;
    private final IdGeneratorService idGeneratorService;
    private final QrTokenService qrTokenService;

    @Transactional
    public VisitBadge createBadge(Visit visit) {

        if (visitBadgeRepository.findByVisit_Id(visit.getId()).isPresent()) {
            throw new IllegalStateException(
                    "Badge already exists for visit: " + visit.getId()
            );
        }
        String badgeId =
                idGeneratorService.generateId(
                        "VISITBADGE",
                        "VB"
                );

        String qrToken =
                qrTokenService.generateToken();

        LocalDateTime issuedAt = LocalDateTime.now();
        LocalDateTime validFrom = issuedAt;

        LocalDateTime validUntil =
                visit.getExpectedArrivalAt()
                        .toLocalDate()
                        .atTime(23, 59, 59);

        VisitBadge badge = new VisitBadge();

        badge.setId(badgeId);
        badge.setVisitor(visit.getVisitor());
        badge.setVisit(visit);
        badge.setQrContainingToken(qrToken);
        badge.setIssuedAt(issuedAt);
        badge.setValidFrom(validFrom);
        badge.setValidUntil(validUntil);
        badge.setStatus(BadgeStatus.ACTIVE);

        return visitBadgeRepository.save(badge);
    }

    @Transactional
    public void invalidateBadgeOnCheckout(
            String visitId,
            LocalDateTime checkoutTime
    ) {

        VisitBadge badge =
                visitBadgeRepository.findByVisit_Id(visitId)
                        .orElse(null);

        if (badge == null) {
            return;
        }

        badge.setValidUntil(checkoutTime);
        badge.setStatus(BadgeStatus.INVALID);

        visitBadgeRepository.save(badge);
    }
}