package com.adminvisitor.service;

import com.adminvisitor.enums.Nationality;
import com.adminvisitor.enums.ProofType;

public interface ProofValidationService {

    void validate(
            Nationality nationality,
            String aadharNumber,
            String panNumber,
            String passportNumber
    );

    void validate(
            Nationality nationality,
            ProofType proofType
    );
}