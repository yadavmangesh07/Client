package com.billingapp.repository;

import com.billingapp.entity.Challan;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChallanRepository extends MongoRepository<Challan, String> {
    
    // Check if duplicate exists
    boolean existsByChallanNo(String challanNo);

    // Find all challans that start with a specific prefix (e.g., "JMD/2025-26/")
    List<Challan> findByChallanNoStartingWith(String prefix);

    // Keep existing
    Optional<Challan> findTopByOrderByCreatedAtDesc();
}