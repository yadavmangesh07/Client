package com.billingapp.service;

import com.billingapp.dto.EwayBillRequest;
import com.billingapp.entity.Client;
import com.billingapp.entity.Company;
import com.billingapp.entity.Invoice;
import com.billingapp.repository.ClientRepository;
import com.billingapp.repository.CompanyRepository;
import com.billingapp.repository.InvoiceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EwayBillService {

    private final InvoiceRepository invoiceRepository;
    private final CompanyRepository companyRepository;
    private final ClientRepository clientRepository;
    private final ObjectMapper objectMapper;

    public EwayBillService(InvoiceRepository invoiceRepository, CompanyRepository companyRepository, ClientRepository clientRepository) {
        this.invoiceRepository = invoiceRepository;
        this.companyRepository = companyRepository;
        this.clientRepository = clientRepository;
        this.objectMapper = new ObjectMapper();
    }
    private Integer parsePincode(String pin) {
        try {
            if (pin != null && pin.matches("\\d+")) {
                return Integer.parseInt(pin.trim());
            }
        } catch (NumberFormatException e) {
            // Ignore
        }
        return 400000; // Default fallback if data is missing
    }

    public byte[] generateJson(String invoiceId) throws Exception {
        // 1. Fetch Data
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();
        Client client = clientRepository.findById(invoice.getClientId()).orElseThrow();
        Company company = companyRepository.findById("MY_COMPANY").orElse(new Company());

        // 2. Map to E-Way Schema
        EwayBillRequest req = new EwayBillRequest();
        
        // Doc Details
        req.setDocNo(invoice.getInvoiceNo());
        req.setDocDate(DateTimeFormatter.ofPattern("dd/MM/yyyy")
                .withZone(ZoneId.systemDefault()).format(invoice.getIssuedAt()));

        // Sender (You)
        req.setFromGstin(company.getGstin() != null ? company.getGstin() : "URP"); // URP = Unregistered Person
        req.setFromTrdName(company.getCompanyName());
        req.setFromAddr1(company.getAddress());
        req.setFromPincode(extractPincode(company.getAddress())); 
        req.setFromStateCode(27); // Default Maharashtra (Fix later)
        req.setFromPlace("MUMBAI"); // Default (Fix later)
        req.setFromPincode(parsePincode(company.getPincode()));
        // Receiver (Client)
        req.setToGstin(client.getGstin() != null && !client.getGstin().isEmpty() ? client.getGstin() : "URP");
        req.setToTrdName(client.getName());
        req.setToAddr1(client.getAddress());
        req.setToPincode(extractPincode(client.getAddress()));
        req.setToStateCode(27); // Default Maharashtra (Should be logic based on GSTIN first 2 digits)
        req.setToPlace("MUMBAI");
        req.setToPincode(parsePincode(client.getPincode()));
        // Totals
        req.setTotInvValue(invoice.getTotal());
        req.setTotalValue(invoice.getSubtotal());
        
        // Calculate tax chunks (Simplified: Assuming IGST is 0 for intra-state default)
        // Ideally, check if State Codes match. If same -> SGST+CGST. If diff -> IGST.
        double totalTax = invoice.getTotal() - invoice.getSubtotal();
        req.setCgstValue(totalTax / 2);
        req.setSgstValue(totalTax / 2);
        req.setIgstValue(0.0);

        // Items
        List<EwayBillRequest.EwayItem> items = new ArrayList<>();
        if (invoice.getItems() != null) {
            for (Invoice.InvoiceItem item : invoice.getItems()) {
                EwayBillRequest.EwayItem eItem = new EwayBillRequest.EwayItem();
                eItem.setProductName(item.getDescription());
                try {
                    eItem.setHsnCode(Integer.parseInt(item.getHsnCode()));
                } catch (Exception e) { eItem.setHsnCode(0); }
                
                eItem.setQuantity((double) item.getQty());
                eItem.setQtyUnit(item.getUom() != null ? item.getUom() : "NOS");
                eItem.setTaxableAmount(item.getQty() * item.getRate());
                
                // Tax Rates
                eItem.setCgstRate(item.getTaxRate() / 2);
                eItem.setSgstRate(item.getTaxRate() / 2);
                eItem.setIgstRate(0.0);
                
                items.add(eItem);
            }
        }
        req.setItemList(items);

        // 3. Convert to JSON Bytes
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(req);
    }

    // Helper: Find 6 digit pincode in address
    private Integer extractPincode(String address) {
        if (address == null) return 400000;
        Matcher m = Pattern.compile("\\b\\d{6}\\b").matcher(address);
        if (m.find()) {
            return Integer.parseInt(m.group());
        }
        return 400000; // Default fallback
    }
}