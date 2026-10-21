package com.adminvisitor.service;

import com.adminvisitor.enums.Nationality;
import com.adminvisitor.enums.ProofType;

public interface ProofSecurityService {

    String generateBlindIndex(ProofType proofType, String proofNumber);

    void validate(Nationality nationality, ProofType proofType);

    void validate(
            Nationality nationality,
            String aadharNumber,
            String panNumber,
            String passportNumber
    );
}