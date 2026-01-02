package com.billingapp.repository;

import com.billingapp.entity.Invoice;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends MongoRepository<Invoice, String> {

    // Existing: Smart Invoice Number Logic
    Optional<Invoice> findTopByInvoiceNoStartingWithOrderByCreatedAtDesc(String prefix);

    // 👇 NEW: Dashboard Queries

    // 1. Get recent 5 invoices for "Recent Activity" list
    List<Invoice> findTop5ByOrderByCreatedAtDesc();

    // 2. Count invoices by status (Used for "Pending Invoices" count)
    long countByStatus(String status);

    // 3. Sum total amount of ALL invoices (Total Revenue)
    // Uses MongoDB Aggregation to sum the 'total' field of all documents
    @Aggregation(pipeline = {
            "{ '$group': { '_id': null, 'total': { '$sum': '$total' } } }"
    })
    Double sumTotalAmount();

    // 4. Sum total amount based on Status (e.g., Total "PENDING" Amount)
    // Filters by status first, then sums the total
    @Aggregation(pipeline = {
            "{ '$match': { 'status': ?0 } }",
            "{ '$group': { '_id': null, 'total': { '$sum': '$total' } } }"
    })
    Double sumTotalByStatus(String status);

    // Add this if missing
    List<Invoice> findByClientId(String clientId);
}