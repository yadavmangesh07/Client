package com.billingapp.repository;

import com.billingapp.entity.WorkCompletionCertificate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkCompletionCertificateRepository extends MongoRepository<WorkCompletionCertificate, String> {
    
    // Existing fuzzy search
    List<WorkCompletionCertificate> findByStoreNameContainingIgnoreCase(String storeName);
    
    // Existing top finder
    Optional<WorkCompletionCertificate> findTopByOrderByCreatedAtDesc();

    // 👇 NEW: Find by exact name (ignoring case) for the Profile Page correlation
    List<WorkCompletionCertificate> findByStoreNameIgnoreCase(String storeName);
}