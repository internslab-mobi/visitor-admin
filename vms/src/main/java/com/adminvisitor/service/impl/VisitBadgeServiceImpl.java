package com.adminvisitor.service.impl;

import com.adminvisitor.entity.Visit;
import com.adminvisitor.entity.VisitBadge;
import com.adminvisitor.enums.BadgeStatus;
import com.adminvisitor.exception.ResourceAlreadyExistsException;
import com.adminvisitor.repository.VisitBadgeRepository;
import com.adminvisitor.service.QrTokenService;
import com.adminvisitor.service.VisitBadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VisitBadgeServiceImpl implements VisitBadgeService {

    private final VisitBadgeRepository visitBadgeRepository;
    private final IdGeneratorServiceImpl idGeneratorService;
   // private final QrTokenService qrTokenService;

//    @Transactional
//    public VisitBadge createBadge(Visit visit) {
//
//        if (visitBadgeRepository.findByVisit_Id(visit.getId()).isPresent()) {
//            throw new ResourceAlreadyExistsException(
//                    "A visitor badge already exists for visit: " + visit.getId()
//            );
//        }
//        String badgeId =
//                idGeneratorService.generateId(
//                        "VISITBADGE",
//                        "VB"
//                );
//
//        String qrToken =
//                qrTokenService.generateToken();
//
//        LocalDateTime issuedAt = LocalDateTime.now();
//     //   LocalDateTime validFrom = issuedAt;
//
//        LocalDateTime validUntil =
//                visit.getExpectedArrivalAt()
//                        .toLocalDate()
//                        .atTime(23, 59, 59);
//
//        VisitBadge badge = new VisitBadge();
//
//        badge.setId(badgeId);
//        badge.setVisit(visit);
//        badge.setQrContainingToken(qrToken);
//        badge.setIssuedAt(issuedAt);
//        badge.setValidUntil(validUntil);
//        badge.setStatus(BadgeStatus.ACTIVE);
//
//        return visitBadgeRepository.save(badge);
//    }


    @Override
    @Transactional
    public VisitBadge createBadge(Visit visit, String qrToken) {

        if (visitBadgeRepository.findByVisit_Id(visit.getId()).isPresent()) {
            throw new ResourceAlreadyExistsException(
                    "A visitor badge already exists for visit: "
                            + visit.getId()
            );
        }

        if (qrToken == null || qrToken.isBlank()) {
            throw new IllegalArgumentException(
                    "QR token must not be empty"
            );
        }

        String badgeId = idGeneratorService.generateId(
                "VISITBADGE",
                "VB"
        );

        LocalDateTime issuedAt = LocalDateTime.now();

        LocalDateTime validUntil = visit.getExpectedArrivalAt()
                .toLocalDate()
                .atTime(23, 59, 59);

        VisitBadge badge = new VisitBadge();

        badge.setId(badgeId);
        badge.setVisit(visit);
        badge.setQrContainingToken(qrToken);
        badge.setIssuedAt(issuedAt);
        badge.setValidUntil(validUntil);
        badge.setStatus(BadgeStatus.ACTIVE);

        return visitBadgeRepository.save(badge);
    }
    @Transactional
    public void invalidateBadgeOnCheckout(
            String visitId,
            LocalDateTime checkoutTime
    ) {

        VisitBadge badge = visitBadgeRepository.findByVisit_Id(visitId)
                        .orElse(null);

        if (badge == null) {
            return;
        }

        badge.setValidUntil(checkoutTime);
        badge.setStatus(BadgeStatus.INVALID);

        visitBadgeRepository.save(badge);
    }





    @Override
    @Transactional(readOnly = true)
    public VisitBadge validateQrToken(String qrToken) {

        VisitBadge badge =
                visitBadgeRepository
                        .findByQrContainingToken(qrToken)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invalid QR code"
                                )
                        );

        LocalDateTime now = LocalDateTime.now();

        if (badge.getStatus() != BadgeStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "QR code is no longer valid"
            );
        }

        if (now.isAfter(badge.getValidUntil())) {
            throw new IllegalArgumentException(
                    "QR code has expired"
            );
        }

        return badge;
    }
}