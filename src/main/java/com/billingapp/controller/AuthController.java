package com.billingapp.controller;

import com.billingapp.entity.User;
import com.billingapp.repository.UserRepository;
import com.billingapp.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // 1. Declare the dependencies
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository; // <--- This was likely missing
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtil;

    // 2. Initialize them in the Constructor
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
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid username or password");
        }

        String token = jwtUtil.generateToken(username);
        return ResponseEntity.ok(Map.of("token", token));
    }

    // --- REGISTER ENDPOINT (New) ---
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        // Default to USER if no role is provided
        String role = body.getOrDefault("role", "USER"); 

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body("Username and password are required");
        }

        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body("Username is already taken!");
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setRole(role);

        userRepository.save(newUser);

        return ResponseEntity.ok("User registered successfully");
    }
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        // Security Tip: In a real app, verify the requester is ADMIN here
        List<User> users = userRepository.findAll();
        // Hide passwords in response for security
        users.forEach(u -> u.setPassword("HIDDEN")); 
        return ResponseEntity.ok(users);
    }

    // 2. Delete User
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok("User deleted successfully");
    }
}