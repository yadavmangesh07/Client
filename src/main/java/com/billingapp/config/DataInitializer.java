package com.billingapp.config;

import com.billingapp.entity.User;
import com.billingapp.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    // @Bean
    // public PasswordEncoder passwordEncoder() {
    //     return new BCryptPasswordEncoder();
    // }

    @Bean
    public CommandLineRunner initData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Check if admin exists in Mongo, if not, create one
            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                // Encrypt "password123"
                admin.setPassword(passwordEncoder.encode("password123"));
                admin.setRole("ADMIN");
                userRepository.save(admin);
                System.out.println("✅ MONGODB: DEFAULT USER CREATED: admin / password123");
            } else {
                System.out.println("ℹ️ MONGODB: Admin user already exists.");
            }
        };
    }
}