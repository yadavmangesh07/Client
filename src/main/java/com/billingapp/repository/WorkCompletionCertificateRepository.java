package com.billingapp.repository;

import com.billingapp.entity.WorkCompletionCertificate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkCompletionCertificateRepository extends MongoRepository<WorkCompletionCertificate, String> {
    
    // 👇 NEW: Find by Client ID (Robust Link)
    List<WorkCompletionCertificate> findByClientId(String clientId);

    // Existing fuzzy search
    List<WorkCompletionCertificate> findByStoreNameContainingIgnoreCase(String storeName);
    
    // Existing top finder
    Optional<WorkCompletionCertificate> findTopByOrderByCreatedAtDesc();

    // Existing exact name finder (Fallback)
    List<WorkCompletionCertificate> findByStoreNameIgnoreCase(String storeName);
}