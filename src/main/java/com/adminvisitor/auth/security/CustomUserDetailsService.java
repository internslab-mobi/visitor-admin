package com.adminvisitor.auth.security;

import com.adminvisitor.auth.entity.User;
import com.adminvisitor.auth.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Custom implementation of Spring Security's {@link UserDetailsService}.
 * <p>
 * Resolves user credentials and roles from the database using employee email addresses during authentication.
 * </p>
 *
 * @author Logeshwaran
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Constructs the user details service with the requisite user repository.
     *
     * @param userRepository the repository used for retrieving user persistence records
     */
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Locates a user account by the corporate email of the associated employee.
     *
     * @param email the employee email identifying the user
     * @return the populated Spring Security {@link UserDetails} principal
     * @throws UsernameNotFoundException if no user account exists with the supplied email
     */
    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user =
                userRepository.findByEmployeeEmail(email)
                        .orElseThrow(() ->
                                new UsernameNotFoundException(
                                        "User not found: " + email
                                )
                        );

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getId())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .build();
    }
}