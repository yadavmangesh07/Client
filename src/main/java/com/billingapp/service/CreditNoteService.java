package com.billingapp.service;

import com.billingapp.entity.CreditNote;
import java.util.List;

public interface CreditNoteService {
    CreditNote create(CreditNote creditNote);
    CreditNote update(String id, CreditNote creditNote);
    CreditNote getById(String id);
    List<CreditNote> getAll();
    void delete(String id);
    byte[] generatePdf(String id) throws Exception;
}