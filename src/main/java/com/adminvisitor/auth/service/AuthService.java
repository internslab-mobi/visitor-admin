package com.adminvisitor.auth.service;

import com.adminvisitor.auth.dto.requestdto.ChangePasswordRequest;
import com.adminvisitor.auth.dto.requestdto.CreateUserRequest;
import com.adminvisitor.auth.dto.requestdto.LoginRequest;
import com.adminvisitor.auth.dto.responsedto.AuthResponse;
import com.adminvisitor.auth.dto.responsedto.AuthResult;
import com.adminvisitor.auth.dto.responsedto.UserResponse;

public interface AuthService {

    UserResponse createUser(CreateUserRequest request);

    AuthResult login(LoginRequest request);

    void changePassword(String userId, ChangePasswordRequest request);

    AuthResult refresh(String rawRefreshToken);
}
