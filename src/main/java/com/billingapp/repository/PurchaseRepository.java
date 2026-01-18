package com.billingapp.repository;

import com.billingapp.entity.Purchase;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface PurchaseRepository extends MongoRepository<Purchase, String> {
    List<Purchase> findByStoreNameContainingIgnoreCase(String storeName);
}