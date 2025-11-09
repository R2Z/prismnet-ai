package com.prismnetai.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // TODO: Implement proper user loading from database
        // For now, return a default user for any username
        // In production, load user from database and check credentials
        return User.builder()
                .username(username)
                .password("") // Password not used for JWT validation
                .authorities(Collections.emptyList())
                .build();
    }
}