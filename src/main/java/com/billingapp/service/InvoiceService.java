package com.billingapp.service;

import com.billingapp.dto.CreateInvoiceRequest;
import com.billingapp.dto.InvoiceDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface InvoiceService {
    InvoiceDTO create(CreateInvoiceRequest req);
    InvoiceDTO getById(String id);
    List<InvoiceDTO> getAll();
    InvoiceDTO update(String id, CreateInvoiceRequest req);
    void delete(String id);

    // new:
    Page<InvoiceDTO> search(
            String clientId,
            String status,
            String fromIso,    // ISO instant string or null
            String toIso,      // ISO instant string or null
            Double minTotal,
            Double maxTotal,
            int page,
            int size,
            String sort        // e.g. "createdAt,desc" or "total,asc"
    );
}
