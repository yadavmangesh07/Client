package com.billingapp.controller;

import com.billingapp.entity.User;
import com.billingapp.repository.UserRepository;
import com.billingapp.security.JwtUtils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtil;

    public AuthController(AuthenticationManager authenticationManager,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtils jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    // --- LOGIN ENDPOINT ---
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        try {
            // 1. Authenticate credentials
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Collections.singletonMap("message", "Invalid username or password"));
        }

        // 2. Generate Token
        String token = jwtUtil.generateToken(username);

        // 3. Retrieve User Details (to get the Role)
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 4. Construct Response with Token + Role + Username
        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("username", user.getUsername());
        response.put("role", user.getRole());

        return ResponseEntity.ok(response);
    }

    // --- REGISTER ENDPOINT ---
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        // Default to USER if no role is provided
        String role = body.getOrDefault("role", "USER");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Username and password are required"));
        }

        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Username is already taken!"));
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setRole(role);

        userRepository.save(newUser);

        return ResponseEntity.ok(Collections.singletonMap("message", "User registered successfully"));
    }

    // --- GET ALL USERS ---
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userRepository.findAll();
        // Hide passwords in response for security
        users.forEach(u -> u.setPassword("HIDDEN"));
        return ResponseEntity.ok(users);
    }

    // --- DELETE USER ---
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok(Collections.singletonMap("message", "User deleted successfully"));
    }

    // --- VERIFY PASSWORD (For Settings Security) ---
    @PostMapping("/verify-password")
    public ResponseEntity<?> verifyPassword(@RequestBody Map<String, String> payload, Principal principal) {
        String password = payload.get("password");

        // 1. Get current logged-in user
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Check password match
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(401).body(Collections.singletonMap("message", "Incorrect password"));
        }

        return ResponseEntity.ok(Map.of("success", true));
    }

    // --- UPDATE CURRENT USER (ME) ---
    @PutMapping("/me")
    public ResponseEntity<?> updateCurrentUser(@RequestBody UpdateProfileRequest request, java.security.Principal principal) {
        try {
            User user = userRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 1. Verify Current Password
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Collections.singletonMap("message", "Incorrect current password"));
            }

            // 2. Update Username (if provided and different)
            if (request.getUsername() != null && !request.getUsername().isBlank()) {
                if (!user.getUsername().equals(request.getUsername())) {
                    if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                         return ResponseEntity.badRequest()
                                 .body(Collections.singletonMap("message", "Username already taken"));
                    }
                    user.setUsername(request.getUsername());
                }
            }

            // 3. Update Password (if provided)
            if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            }

            userRepository.save(user);
            return ResponseEntity.ok(Collections.singletonMap("message", "Profile updated successfully"));

        } catch (Exception e) {
            e.printStackTrace(); // Helpful for debugging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "Error updating profile: " + e.getMessage()));
        }
    }

    // 👇 FIXED: DTO Class MUST be static and public for JSON parsing
    public static class UpdateProfileRequest {
        private String username;
        private String currentPassword;
        private String newPassword;

        // Default Constructor
        public UpdateProfileRequest() {}

        // Getters and Setters
        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getCurrentPassword() {
            return currentPassword;
        }

        public void setCurrentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}