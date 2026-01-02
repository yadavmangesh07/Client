package com.billingapp.repository;

import com.billingapp.entity.WorkCompletionCertificate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkCompletionCertificateRepository extends MongoRepository<WorkCompletionCertificate, String> {
    // We can add custom finders here if needed, e.g., by client name
    List<WorkCompletionCertificate> findByStoreNameContainingIgnoreCase(String storeName);
    Optional<WorkCompletionCertificate> findTopByOrderByCreatedAtDesc();
}