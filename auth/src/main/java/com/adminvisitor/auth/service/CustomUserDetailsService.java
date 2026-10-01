package com.adminvisitor.auth.service;

import com.adminvisitor.auth.entity.User;
import com.adminvisitor.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

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