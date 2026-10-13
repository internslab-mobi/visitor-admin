package com.adminvisitor.auth.service;

import com.adminvisitor.auth.dto.requestdto.ChangePasswordRequest;
import com.adminvisitor.auth.dto.requestdto.CreateUserRequest;
import com.adminvisitor.auth.dto.requestdto.ForgotPasswordRequest;
import com.adminvisitor.auth.dto.requestdto.LoginRequest;
import com.adminvisitor.auth.dto.requestdto.ResetPasswordRequest;
import com.adminvisitor.auth.dto.requestdto.VerifyOtpRequest;
import com.adminvisitor.auth.dto.responsedto.AuthResult;
import com.adminvisitor.auth.dto.responsedto.UserResponse;
import com.adminvisitor.auth.dto.responsedto.VerifyOtpResponse;

public interface AuthService {

    UserResponse createUser(CreateUserRequest request);

    AuthResult login(LoginRequest request);

    void changePassword(String userId, ChangePasswordRequest request);

    AuthResult refresh(String rawRefreshToken);

    boolean forgotPassword(ForgotPasswordRequest request);

    VerifyOtpResponse verifyResetOtp(VerifyOtpRequest request);

    void resetPassword(ResetPasswordRequest request);

    void logout(String refreshToken);
}
