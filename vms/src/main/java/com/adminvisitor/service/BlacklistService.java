package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.BlacklistRequest;
import com.adminvisitor.dto.responsedto.BlacklistResponse;
import com.adminvisitor.entity.Blacklist;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.enums.BlacklistStatus;
import com.adminvisitor.exception.BlacklistAlreadyExistsException;
import com.adminvisitor.exception.BlacklistAlreadyRemovedException;
import com.adminvisitor.exception.BlacklistNotFoundException;
import com.adminvisitor.exception.VisitorNotFoundException;
import com.adminvisitor.mapper.BlacklistMapper;
import com.adminvisitor.repository.BlacklistRepository;
import com.adminvisitor.repository.VisitorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BlacklistService {

    private final BlacklistRepository blacklistRepository;
    private final VisitorRepository visitorRepository;
    private final BlacklistMapper blacklistMapper;
    private final IdGeneratorService idGeneratorService;

    @Transactional(readOnly = true)
    public boolean isBlacklisted(String visitorId) {

        return blacklistRepository
                .findByVisitorIdAndStatus(
                        visitorId,
                        BlacklistStatus.ACTIVE
                )
                .isPresent();
    }

    public BlacklistResponse addToBlacklist(
            BlacklistRequest request) {

        if (isBlacklisted(request.getVisitorId())) {
            throw new BlacklistAlreadyExistsException(
                    "Visitor is already in blacklist"
            );
        }

        Visitor visitor = visitorRepository.findById(request.getVisitorId())
                .orElseThrow(() ->
                        new VisitorNotFoundException(
                                "Visitor not found"
                        )
                );

        Blacklist blacklist =
                blacklistMapper.toEntity(request, visitor);

        blacklist.setId(
                idGeneratorService.generateId("BLACKLIST", "BL")
        );

        blacklist.setStatus(BlacklistStatus.ACTIVE);

        Blacklist savedBlacklist =
                blacklistRepository.save(blacklist);

        return blacklistMapper.toResponse(savedBlacklist);
    }

    public BlacklistResponse removeFromBlacklist(
            String blacklistId,
            String removedBy) {

        Blacklist blacklist =
                blacklistRepository.findById(blacklistId)
                        .orElseThrow(() ->
                                new BlacklistNotFoundException(
                                        "Blacklist record not found"
                                )
                        );

        if (blacklist.getStatus() == BlacklistStatus.REMOVED) {
            throw new BlacklistAlreadyRemovedException(
                    "Person is already removed from blacklist"
            );
        }

        blacklist.setStatus(BlacklistStatus.REMOVED);
        blacklist.setUpdatedBy(removedBy);

        Blacklist updatedBlacklist =
                blacklistRepository.save(blacklist);

        return blacklistMapper.toResponse(updatedBlacklist);
    }

    @Transactional(readOnly = true)
    public List<BlacklistResponse> getAllBlacklistRecords() {

        return blacklistRepository.findAll()
                .stream()
                .map(blacklistMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BlacklistResponse getBlacklistById(String id) {

        Blacklist blacklist =
                blacklistRepository.findById(id)
                        .orElseThrow(() ->
                                new BlacklistNotFoundException(
                                        "Blacklist record not found"
                                )
                        );

        return blacklistMapper.toResponse(blacklist);
    }
}