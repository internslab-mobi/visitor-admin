package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.BlacklistRequest;
import com.adminvisitor.dto.responsedto.BlacklistResponse;
import com.adminvisitor.entity.Blacklist;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.enums.BlacklistStatus;
import com.adminvisitor.mapper.BlacklistMapper;
import com.adminvisitor.repository.BlacklistRepository;
import com.adminvisitor.repository.VisitorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class BlacklistService {

    private final BlacklistRepository blacklistRepository;
    private final VisitorRepository visitorRepository;
    private final BlacklistMapper blacklistMapper;

    @Transactional(readOnly = true)
    public boolean isBlacklisted(
            String idType,
            String idNumber) {

        return blacklistRepository
                .findByIdTypeAndIdNumberAndStatus(
                        idType,
                        idNumber,
                        BlacklistStatus.ACTIVE
                )
                .isPresent();
    }

    public BlacklistResponse addToBlacklist(
            BlacklistRequest request) {

        if (isBlacklisted(request.getIdType(), request.getIdNumber())) {
            throw new IllegalArgumentException(
                    "Person is already in blacklist"
            );
        }

        Visitor visitor = visitorRepository.findById(request.getVisitorId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Visitor not found"
                        )
                );

        Blacklist blacklist =
                blacklistMapper.toEntity(request, visitor);

        blacklist.setStatus(BlacklistStatus.ACTIVE);
        blacklist.setAddedAt(LocalDateTime.now());

        Blacklist savedBlacklist =
                blacklistRepository.save(blacklist);

        return blacklistMapper.toResponse(savedBlacklist);
    }

    public BlacklistResponse removeFromBlacklist(
            Long blacklistId,
            Long removedBy) {

        Blacklist blacklist =
                blacklistRepository.findById(blacklistId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Blacklist record not found"
                                )
                        );

        if (blacklist.getStatus() == BlacklistStatus.REMOVED) {
            throw new IllegalArgumentException(
                    "Person is already removed from blacklist"
            );
        }

        blacklist.setStatus(BlacklistStatus.REMOVED);
        blacklist.setRemovedAt(LocalDateTime.now());
        blacklist.setRemovedBy(removedBy);

        Blacklist updatedBlacklist =
                blacklistRepository.save(blacklist);

        return blacklistMapper.toResponse(updatedBlacklist);
    }
}