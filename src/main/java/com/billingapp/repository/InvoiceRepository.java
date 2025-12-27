package com.billingapp.repository;

import com.billingapp.entity.Invoice;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends MongoRepository<Invoice, String> {
    // add custom queries later (findByInvoiceNo, findByClientId, etc.)
    Optional<Invoice> findTopByInvoiceNoStartingWithOrderByCreatedAtDesc(String prefix);

}
