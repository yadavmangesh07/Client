package com.billingapp.service;

import com.billingapp.entity.User;
import com.billingapp.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // 👇 SAFETY CHECK: If role is null (old users), default to USER (or ADMIN for specific users)
        String role = user.getRole();
        if (role == null || role.isEmpty()) {
            role = "USER"; 
        }

        // Remove "ROLE_" prefix if it exists in DB to avoid double prefixing
        if (role.startsWith("ROLE_")) {
            role = role.substring(5);
        }

        return org.springframework.security.core.userdetails.User
                .builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(role) // Spring automatically adds "ROLE_"
                .build();
    }
}