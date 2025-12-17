package com.billingapp.repository;

import com.billingapp.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    // MongoDB automatically implements this based on the method name
    Optional<User> findByUsername(String username);
}