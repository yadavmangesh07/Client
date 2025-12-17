package com.billingapp.mapper;

import com.billingapp.dto.CreateInvoiceRequest;
import com.billingapp.dto.InvoiceDTO;
import com.billingapp.dto.InvoiceItemRequest;
import com.billingapp.entity.Invoice;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class InvoiceMapper {

    public InvoiceDTO toDto(Invoice invoice) {
        if (invoice == null) return null;
        InvoiceDTO dto = new InvoiceDTO();
        dto.setId(invoice.getId());
        dto.setInvoiceNo(invoice.getInvoiceNo());
        dto.setClientId(invoice.getClientId());
        dto.setItems(mapItemsToDto(invoice.getItems()));
        dto.setSubtotal(invoice.getSubtotal());
        dto.setTax(invoice.getTax());
        dto.setTotal(invoice.getTotal());
        dto.setStatus(invoice.getStatus());
        dto.setIssuedAt(invoice.getIssuedAt());
        dto.setDueDate(invoice.getDueDate());
        dto.setCreatedBy(invoice.getCreatedBy());
        dto.setCreatedAt(invoice.getCreatedAt());
        dto.setUpdatedAt(invoice.getUpdatedAt());
        return dto;
    }

    public Invoice toEntity(CreateInvoiceRequest req) {
        if (req == null) return null;
        Invoice invoice = Invoice.builder()
                .clientId(req.getClientId())
                .items(mapItemsFromRequest(req.getItems()))
                .tax(req.getTax())
                .status(req.getStatus())
                .issuedAt(req.getIssuedAt())
                .dueDate(req.getDueDate())
                .createdBy(req.getCreatedBy())
                .build();
        return invoice;
    }

    private List<Invoice.InvoiceItem> mapItemsFromRequest(List<InvoiceItemRequest> items) {
        if (items == null) return null;
        return items.stream().map(i -> Invoice.InvoiceItem.builder()
                .description(i.getDescription())
                .qty(i.getQty())
                .rate(i.getRate())
                .build()).collect(Collectors.toList());
    }

    private List<InvoiceDTO.InvoiceItemDTO> mapItemsToDto(List<Invoice.InvoiceItem> items) {
        if (items == null) return null;
        return items.stream().map(i -> {
            InvoiceDTO.InvoiceItemDTO dto = new InvoiceDTO.InvoiceItemDTO();
            dto.setDescription(i.getDescription());
            dto.setQty(i.getQty());
            dto.setRate(i.getRate());
            dto.setAmount(i.getQty() * i.getRate());
            return dto;
        }).collect(Collectors.toList());
    }
}
