package com.billingapp.service.impl;

import com.billingapp.dto.CreateInvoiceRequest;
import com.billingapp.dto.InvoiceDTO;
import com.billingapp.dto.InvoiceItemRequest;
import com.billingapp.entity.Invoice;
import com.billingapp.mapper.InvoiceMapper;
import com.billingapp.repository.InvoiceRepository;
import com.billingapp.service.InvoiceService;
// import com.billingapp.util.InvoiceNumberUtil; // 👈 Removed, logic is now internal
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate; // 👈 Added
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional; // 👈 Added
import java.util.stream.Collectors;

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
    public InvoiceDTO create(CreateInvoiceRequest req) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("Invoice must have at least one item");
        }

        Invoice invoice = mapper.toEntity(req);
        
        // 👇 UPDATED: Generate Smart Invoice Number (JMD/2025-26/97)
        invoice.setInvoiceNo(generateInvoiceNumber());

        double subtotal = computeSubtotalFromItems(req.getItems());
        invoice.setSubtotal(subtotal);
        invoice.setTax(req.getTax());
        invoice.setTotal(subtotal + req.getTax());

        invoice.setCreatedAt(Instant.now());
        if (invoice.getIssuedAt() == null) invoice.setIssuedAt(Instant.now());

        Invoice saved = invoiceRepository.save(invoice);
        return mapper.toDto(saved);
    }

    @Override
    public InvoiceDTO getById(String id) {
        Invoice inv = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));
        return mapper.toDto(inv);
    }

    @Override
    public List<InvoiceDTO> getAll() {
        return invoiceRepository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    public InvoiceDTO update(String id, CreateInvoiceRequest req) {
        Invoice existing = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));

        // 1. Update Items & Totals
        if (req.getItems() != null && !req.getItems().isEmpty()) {
            existing.setItems(mapper.toEntity(req).getItems());
            double subtotal = computeSubtotalFromItems(req.getItems());
            existing.setSubtotal(subtotal);
            existing.setTotal(subtotal + req.getTax());
            existing.setTax(req.getTax());
        } else if (req.getTax() != 0d) {
            // Recalculate if only tax changed
            existing.setTax(req.getTax());
            existing.setTotal(existing.getSubtotal() + req.getTax());
        }

        // 2. Update Standard Fields
        if (req.getStatus() != null) existing.setStatus(req.getStatus());
        if (req.getIssuedAt() != null) existing.setIssuedAt(req.getIssuedAt());
        if (req.getDueDate() != null) existing.setDueDate(req.getDueDate());
        if (req.getCreatedBy() != null) existing.setCreatedBy(req.getCreatedBy());
        if (req.getClientId() != null) existing.setClientId(req.getClientId());

        // 3. 👇 Update New Logistics/Address Fields
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
        return mapper.toDto(saved);
    }

    @Override
    public void delete(String id) {
        if (!invoiceRepository.existsById(id)) {
            throw new IllegalArgumentException("Invoice not found: " + id);
        }
        invoiceRepository.deleteById(id);
    }

    // ----------------------------
    // Search implementation
    // ----------------------------
    @Override
    public Page<InvoiceDTO> search(String clientId, String status, String fromIso, String toIso,
                                   Double minTotal, Double maxTotal, int page, int size, String sort) {

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
            } catch (DateTimeParseException ignored) {}
        }
        if (toIso != null && !toIso.isBlank()) {
            try {
                Instant to = Instant.parse(toIso);
                criterias.add(Criteria.where("createdAt").lte(to));
            } catch (DateTimeParseException ignored) {}
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
            return defaultSort;
        }
    }

    // 👇 NEW LOGIC: Dynamic Financial Year + Auto Increment
    private String generateInvoiceNumber() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue(); // 1 to 12

        String fy;
        // Financial Year Logic: If Month is Jan(1), Feb(2), Mar(3) -> Previous Year is start of FY
        // Example: Jan 2026 => FY 2025-26
        if (month <= 3) {
            fy = (year - 1) + "-" + String.valueOf(year).substring(2); 
        } else {
            // Example: April 2025 => FY 2025-26
            fy = year + "-" + String.valueOf(year + 1).substring(2); 
        }

        String prefix = "JMD/" + fy + "/";

        // Find the last invoice with this specific prefix
        // NOTE: Ensure InvoiceRepository has findTopByInvoiceNoStartingWithOrderByCreatedAtDesc
        Optional<Invoice> lastInvoice = invoiceRepository.findTopByInvoiceNoStartingWithOrderByCreatedAtDesc(prefix);

        int nextNum = 1;
        if (lastInvoice.isPresent()) {
            String lastNo = lastInvoice.get().getInvoiceNo();
            // Format is JMD/2025-26/97
            String[] parts = lastNo.split("/");
            if (parts.length == 3) {
                try {
                    int lastSeq = Integer.parseInt(parts[2]);
                    nextNum = lastSeq + 1;
                } catch (NumberFormatException e) {
                    nextNum = 1; // Safety fallback
                }
            }
        }

        return prefix + nextNum;
    }
}