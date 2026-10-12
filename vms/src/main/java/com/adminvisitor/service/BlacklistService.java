package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.AddVisitorToBlacklistRequest;
import com.adminvisitor.dto.requestdto.BlacklistRequest;
import com.adminvisitor.dto.responsedto.BlacklistResponse;
import com.adminvisitor.enums.ProofType;

import java.util.List;

public interface BlacklistService {

    boolean isBlacklisted(String visitorId);

    boolean isBlacklistedByProof(ProofType proofType, String proofNumber);

    BlacklistResponse addExistingVisitorToBlacklist(
            String visitorId,
            AddVisitorToBlacklistRequest request
    );

    BlacklistResponse removeFromBlacklist(
            String blacklistId,
            String removedBy
    );

    List<BlacklistResponse> getAllBlacklistRecords();

    BlacklistResponse getBlacklistById(String id);
}