package com.billingapp.repository;

import com.billingapp.entity.Challan;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface ChallanRepository extends MongoRepository<Challan, String> {
    Optional<Challan> findByChallanNo(String challanNo);
}