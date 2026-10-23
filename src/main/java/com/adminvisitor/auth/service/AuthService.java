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

/**
 * Defines the contract for authentication and user account management operations,
 * including user creation, login, password management, token refresh, and logout.
 *
 * @author Logeshwaran
 */
public interface AuthService {

    /**
     * Creates a new user account using the provided request details.
     *
     * @param request the request containing the new user's information
     * @return the response containing the created user's details
     */
    UserResponse createUser(CreateUserRequest request);

    /**
     * Authenticates a user using the provided login credentials.
     *
     * @param request the request containing the user's login credentials
     * @return the authentication result containing the relevant authentication data
     */
    AuthResult login(LoginRequest request);

    /**
     * Changes the password of the specified user.
     *
     * @param userId the unique identifier of the user whose password is being changed
     * @param request the request containing the current and new password details
     */
    void changePassword(String userId, ChangePasswordRequest request);

    /**
     * Issues a new authentication result using the provided refresh token.
     *
     * @param rawRefreshToken the raw refresh token supplied by the client
     * @return the authentication result containing the refreshed authentication data
     */
    AuthResult refresh(String rawRefreshToken);

    /**
     * Initiates the password recovery process for the specified account.
     *
     * @param request the request containing the account information for password recovery
     * @return {@code true} if the password recovery request is accepted; {@code false} otherwise
     */
    boolean forgotPassword(ForgotPasswordRequest request);

    /**
     * Verifies the one-time password submitted during password recovery.
     *
     * @param request the request containing the OTP verification details
     * @return the response containing the OTP verification result
     */
    VerifyOtpResponse verifyResetOtp(VerifyOtpRequest request);

    /**
     * Resets a user's password using the provided reset request.
     *
     * @param request the request containing the password reset details
     */
    void resetPassword(ResetPasswordRequest request);

    /**
     * Logs out a user by processing the supplied refresh token.
     *
     * @param refreshToken the refresh token associated with the session to be terminated
     */
    void logout(String refreshToken);
}