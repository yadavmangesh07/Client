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

        // Map new fields
        dto.setBillingAddress(invoice.getBillingAddress());
        dto.setShippingAddress(invoice.getShippingAddress());
        dto.setEwayBillNo(invoice.getEwayBillNo());
        dto.setTransportMode(invoice.getTransportMode());
        dto.setChallanNo(invoice.getChallanNo());
        dto.setChallanDate(invoice.getChallanDate());
        dto.setPoNumber(invoice.getPoNumber());
        dto.setPoDate(invoice.getPoDate());

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
                
                // Map new fields
                .billingAddress(req.getBillingAddress())
                .shippingAddress(req.getShippingAddress())
                .ewayBillNo(req.getEwayBillNo())
                .transportMode(req.getTransportMode())
                .challanNo(req.getChallanNo())
                .challanDate(req.getChallanDate())
                .poNumber(req.getPoNumber())
                .poDate(req.getPoDate())

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
                // Map new item fields
                .hsnCode(i.getHsnCode())
                .uom(i.getUom())
                .taxRate(i.getTaxRate())
                .qty(i.getQty())
                .rate(i.getRate())
                .build()).collect(Collectors.toList());
    }

    private List<InvoiceDTO.InvoiceItemDTO> mapItemsToDto(List<Invoice.InvoiceItem> items) {
        if (items == null) return null;
        return items.stream().map(i -> {
            InvoiceDTO.InvoiceItemDTO dto = new InvoiceDTO.InvoiceItemDTO();
            dto.setDescription(i.getDescription());
            // Map new item fields
            dto.setHsnCode(i.getHsnCode());
            dto.setUom(i.getUom());
            dto.setTaxRate(i.getTaxRate());
            dto.setQty(i.getQty());
            dto.setRate(i.getRate());
            dto.setAmount(i.getQty() * i.getRate());
            return dto;
        }).collect(Collectors.toList());
    }
}