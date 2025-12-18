package com.billingapp.config;

import com.billingapp.entity.User;
import com.billingapp.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        String adminUsername = "admin";
        
        Optional<User> existingAdmin = userRepository.findByUsername(adminUsername);

        if (existingAdmin.isEmpty()) {
            // Create Admin if not exists
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode("password123"));
            admin.setRole("ADMIN");
            userRepository.save(admin);
            System.out.println("✅ Admin user created: admin / password123");
        } else {
            // Update existing Admin to ensure Role and Password are correct
            User admin = existingAdmin.get();
            admin.setRole("ADMIN");
            // Only reset password if you really need to (Uncomment next line to force reset password)
            // admin.setPassword(passwordEncoder.encode("password123")); 
            
            userRepository.save(admin);
            System.out.println("✅ Admin user verified/updated");
        }
    }
}