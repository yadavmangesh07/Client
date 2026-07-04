package com.billingapp.service.impl;

import com.billingapp.dto.CreateInvoiceRequest;
import com.billingapp.dto.InvoiceDTO;
import com.billingapp.dto.InvoiceItemRequest;
import com.billingapp.entity.Invoice;
import com.billingapp.mapper.InvoiceMapper;
import com.billingapp.repository.InvoiceRepository;
import com.billingapp.service.InvoiceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper mapper;
    private final MongoTemplate mongoTemplate;

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository, InvoiceMapper mapper, MongoTemplate mongoTemplate) {
        this.invoiceRepository = invoiceRepository;
        this.mapper = mapper;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    @CacheEvict(value = "invoices", allEntries = true) // Flushes invoice lists on new creation
    public InvoiceDTO create(CreateInvoiceRequest req) {
        log.info("Attempting to create Invoice document record: {}", req.getInvoiceNo());
        
        if (req.getItems() == null || req.getItems().isEmpty()) {
            log.warn("Invoice creation aborted: payload items array collection is empty");
            throw new IllegalArgumentException("Invoice must have at least one item");
        }

        if (req.getInvoiceNo() == null || req.getInvoiceNo().isBlank()) {
            log.warn("Invoice creation aborted: missing alphanumeric identifier entry reference code");
            throw new IllegalArgumentException("Invoice number is required for manual entry");
        }

        if (invoiceRepository.existsByInvoiceNo(req.getInvoiceNo())) {
            log.warn("Invoice creation aborted: tracking identifier target code {} already exists in system context", req.getInvoiceNo());
            throw new IllegalArgumentException("Invoice number " + req.getInvoiceNo() + " already exists");
        }

        Invoice invoice = mapper.toEntity(req);
        invoice.setInvoiceNo(req.getInvoiceNo());

        double subtotal = computeSubtotalFromItems(req.getItems());
        invoice.setSubtotal(subtotal);
        invoice.setTax(req.getTax());
        invoice.setTotal(subtotal + req.getTax());

        invoice.setCreatedAt(Instant.now());
        if (invoice.getIssuedAt() == null) invoice.setIssuedAt(Instant.now());

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice tracking token successfully written to collection persistence storage layer with inner record ID: {}", saved.getId());
        return mapper.toDto(saved);
    }

    @Override
    @Cacheable(value = "invoices", key = "#id") // Caches individual invoices by database ID
    public InvoiceDTO getById(String id) {
        log.debug("Executing structured data mapping find lookup query for instance token key parameter ID: {}", id);
        Invoice inv = invoiceRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Data lookup request failed: Invoice record signature ID {} is missing or non-existent", id);
                    return new IllegalArgumentException("Invoice not found: " + id);
                });
        return mapper.toDto(inv);
    }

    @Override
    @Cacheable(value = "invoices") // Caches the comprehensive historical lookup lists
    public List<InvoiceDTO> getAll() {
        log.debug("Executing collective historical dump vector query against transaction logging datasets");
        return invoiceRepository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = "invoices", allEntries = true) // Evicts old state on document recalculations/updates
    public InvoiceDTO update(String id, CreateInvoiceRequest req) {
        log.info("Attempting to commit transaction modification delta configurations on entry entity database reference ID: {}", id);
        
        Invoice existing = invoiceRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Update payload configuration rejected: target mapping entity matching code ID {} doesn't exist", id);
                    return new IllegalArgumentException("Invoice not found: " + id);
                });

        // 1. Update Items & Totals
        if (req.getItems() != null && !req.getItems().isEmpty()) {
            existing.setItems(mapper.toEntity(req).getItems());
            double subtotal = computeSubtotalFromItems(req.getItems());
            existing.setSubtotal(subtotal);
            existing.setTotal(subtotal + req.getTax());
            existing.setTax(req.getTax());
        } else if (req.getTax() != 0d) {
            existing.setTax(req.getTax());
            existing.setTotal(existing.getSubtotal() + req.getTax());
        }
        if (req.getClientGst() != null) existing.setClientGst(req.getClientGst());

        // 2. Update Standard Fields
        if (req.getInvoiceNo() != null && !req.getInvoiceNo().equals(existing.getInvoiceNo())) {
            if (invoiceRepository.existsByInvoiceNo(req.getInvoiceNo())) {
                log.warn("Update mutation failed: collision vector triggered for modification code {}", req.getInvoiceNo());
                throw new IllegalArgumentException("New invoice number already exists");
            }
            existing.setInvoiceNo(req.getInvoiceNo());
        }

        if (req.getStatus() != null) existing.setStatus(req.getStatus());
        if (req.getIssuedAt() != null) existing.setIssuedAt(req.getIssuedAt());
        if (req.getDueDate() != null) existing.setDueDate(req.getDueDate());
        if (req.getCreatedBy() != null) existing.setCreatedBy(req.getCreatedBy());
        if (req.getClientId() != null) existing.setClientId(req.getClientId());

        // 3. Update Logistics/Address Fields
        if (req.getBillingAddress() != null) existing.setBillingAddress(req.getBillingAddress());
        if (req.getShippingAddress() != null) existing.setShippingAddress(req.getShippingAddress());
        if (req.getTransportMode() != null) existing.setTransportMode(req.getTransportMode());
        if (req.getEwayBillNo() != null) existing.setEwayBillNo(req.getEwayBillNo());
        if (req.getPoNumber() != null) existing.setPoNumber(req.getPoNumber());
        if (req.getPoDate() != null) existing.setPoDate(req.getPoDate());
        if (req.getChallanNo() != null) existing.setChallanNo(req.getChallanNo());
        if (req.getChallanDate() != null) existing.setChallanDate(req.getChallanDate());

        existing.setUpdatedAt(Instant.now());
        Invoice saved = invoiceRepository.save(existing);
        log.info("Invoice state modifications for target document identity matching ID {} successfully persisted", id);
        return mapper.toDto(saved);
    }

    @Override
    @CacheEvict(value = "invoices", allEntries = true) // Drops stale entry tags on removals
    public void delete(String id) {
        log.info("Initiating structural row cache eviction sequence for entry object collection token context code ID: {}", id);
        if (!invoiceRepository.existsById(id)) {
            log.error("Eviction request aborted: deletion reference target key ID {} cannot be verified as existing", id);
            throw new IllegalArgumentException("Invoice not found: " + id);
        }
        invoiceRepository.deleteById(id);
        log.info("Entity index successfully dropped for row ID: {}", id);
    }

    @Override
    public Page<InvoiceDTO> search(String clientId, String status, String fromIso, String toIso,
                                   Double minTotal, Double maxTotal, int page, int size, String sort) {
        log.debug("Executing dynamic multi-criteria parameter aggregate pipeline search filters");
        
        if (page < 0) page = 0;
        if (size <= 0) size = 10;
        Sort s = parseSort(sort, Sort.by(Sort.Direction.DESC, "createdAt"));
        Pageable pageable = PageRequest.of(page, size, s);

        Query countQuery = buildQuery(clientId, status, fromIso, toIso, minTotal, maxTotal);
        long total = mongoTemplate.count(countQuery, Invoice.class);

        Query dataQuery = buildQuery(clientId, status, fromIso, toIso, minTotal, maxTotal);
        dataQuery.with(pageable);
        List<Invoice> list = mongoTemplate.find(dataQuery, Invoice.class);
        List<InvoiceDTO> dtos = list.stream().map(mapper::toDto).collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, total);
    }

    private Query buildQuery(String clientId, String status, String fromIso, String toIso, Double minTotal, Double maxTotal) {
        List<Criteria> criterias = new ArrayList<>();
        if (clientId != null && !clientId.isBlank()) criterias.add(Criteria.where("clientId").is(clientId));
        if (status != null && !status.isBlank()) criterias.add(Criteria.where("status").is(status));
        if (fromIso != null && !fromIso.isBlank()) {
            try {
                Instant from = Instant.parse(fromIso);
                criterias.add(Criteria.where("createdAt").gte(from));
            } catch (DateTimeParseException ignored) {
                log.debug("Bypassing query date parameter parsing rule constraints: invalid fallback ISO string payload layout metadata: {}", fromIso);
            }
        }
        if (toIso != null && !toIso.isBlank()) {
            try {
                Instant to = Instant.parse(toIso);
                criterias.add(Criteria.where("createdAt").lte(to));
            } catch (DateTimeParseException ignored) {
                log.debug("Bypassing query date parameter parsing rule constraints: invalid fallback ISO string payload layout metadata: {}", toIso);
            }
        }
        if (minTotal != null) criterias.add(Criteria.where("total").gte(minTotal));
        if (maxTotal != null) criterias.add(Criteria.where("total").lte(maxTotal));

        Query q = new Query();
        if (!criterias.isEmpty()) {
            Criteria combined = new Criteria().andOperator(criterias.toArray(new Criteria[0]));
            q.addCriteria(combined);
        }
        return q;
    }

    private double computeSubtotalFromItems(List<InvoiceItemRequest> items) {
        return items.stream()
                .mapToDouble(i -> (double) i.getQty() * i.getRate())
                .sum();
    }

    private Sort parseSort(String sort, Sort defaultSort) {
        if (sort == null || sort.isBlank()) return defaultSort;
        try {
            String[] parts = sort.split(",");
            String prop = parts[0];
            Sort.Direction dir = (parts.length > 1 && parts[1].equalsIgnoreCase("asc")) ? Sort.Direction.ASC : Sort.Direction.DESC;
            return Sort.by(dir, prop);
        } catch (Exception e) {
            log.debug("Gracefully handling sort evaluation format failure; reverting data alignment query indexing array context to default rules");
            return defaultSort;
        }
    }
}