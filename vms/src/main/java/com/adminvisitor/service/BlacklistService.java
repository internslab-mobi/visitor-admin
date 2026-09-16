package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.BlacklistRequestDTO;
import com.adminvisitor.dto.responsedto.BlacklistResponseDTO;
import com.adminvisitor.entity.Blacklist;
import com.adminvisitor.enums.BlacklistStatus;
import com.adminvisitor.repository.BlacklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BlacklistService {

    private final BlacklistRepository blacklistRepository;

    public boolean isBlacklisted(String idType, String idNumber) {

        return blacklistRepository
                .findByIdTypeAndIdNumberAndStatus(
                        idType,
                        idNumber,
                        BlacklistStatus.ACTIVE
                )
                .isPresent();
    }

    public BlacklistResponseDTO addToBlacklist(
            BlacklistRequestDTO request) {

        if (isBlacklisted(request.getIdType(), request.getIdNumber())) {
            throw new IllegalArgumentException(
                    "Person is already in blacklist"
            );
        }

        Blacklist blacklist = new Blacklist();

        blacklist.setVisitorId(request.getVisitorId());
        blacklist.setIdType(request.getIdType());
        blacklist.setIdNumber(request.getIdNumber());
        blacklist.setReason(request.getReason());
        blacklist.setStatus(BlacklistStatus.ACTIVE);
        blacklist.setAddedBy(request.getAddedBy());
        blacklist.setAddedAt(LocalDateTime.now());

        Blacklist savedBlacklist =
                blacklistRepository.save(blacklist);

        return mapToResponse(savedBlacklist);
    }

    public BlacklistResponseDTO removeFromBlacklist(
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

        return mapToResponse(updatedBlacklist);
    }

    private BlacklistResponseDTO mapToResponse(
            Blacklist blacklist) {

        return new BlacklistResponseDTO(
                blacklist.getId(),
                blacklist.getVisitorId(),
                blacklist.getIdType(),
                blacklist.getIdNumber(),
                blacklist.getReason(),
                blacklist.getStatus(),
                blacklist.getAddedBy(),
                blacklist.getAddedAt(),
                blacklist.getRemovedAt(),
                blacklist.getRemovedBy()
        );
    }
}