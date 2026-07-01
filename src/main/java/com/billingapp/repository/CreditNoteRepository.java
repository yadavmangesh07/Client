package com.billingapp.repository;

import com.billingapp.entity.CreditNote;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditNoteRepository extends MongoRepository<CreditNote, String> {
    boolean existsByCreditNoteNo(String creditNoteNo);
}