package com.billingapp.repository;

import com.billingapp.entity.Estimate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstimateRepository extends MongoRepository<Estimate, String> {
    // Basic CRUD is auto-provided
}