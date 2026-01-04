package com.billingapp.service.impl;

import com.billingapp.entity.Client;
import com.billingapp.entity.Company;
import com.billingapp.entity.Estimate;
import com.billingapp.repository.ClientRepository;
import com.billingapp.repository.CompanyRepository;
import com.billingapp.repository.EstimateRepository;
import com.billingapp.service.EstimateService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class EstimateServiceImpl implements EstimateService {

    private final EstimateRepository estimateRepository;
    private final ClientRepository clientRepository;
    private final CompanyRepository companyRepository;

    // Fonts
    private static final Font FONT_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final Font FONT_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
    private static final Font FONT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
    private static final Font FONT_SMALL = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
    private static final Font FONT_RED = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.RED);

    public EstimateServiceImpl(EstimateRepository estimateRepository, ClientRepository clientRepository, CompanyRepository companyRepository) {
        this.estimateRepository = estimateRepository;
        this.clientRepository = clientRepository;
        this.companyRepository = companyRepository;
    }

    @Override
    public List<Estimate> getAllEstimates() {
        return estimateRepository.findAll();
    }
    @Override
    public Estimate getEstimateById(String id) {
        return estimateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estimate not found with id: " + id));
    }

    @Override
    public Estimate createEstimate(Estimate estimate) {
        // Auto-generate Estimate No logic if empty
        if (estimate.getEstimateNo() == null || estimate.getEstimateNo().isEmpty()) {
            
            // 1. Calculate Financial Year dynamically
            java.time.LocalDate now = java.time.LocalDate.now();
            int currentYear = now.getYear();
            int currentMonth = now.getMonthValue(); // 1=Jan, ... 4=April
            
            String finYear;
            if (currentMonth >= 4) {
                // April onwards (e.g., April 2025 -> "2025-26")
                finYear = currentYear + "-" + (currentYear + 1 - 2000);
            } else {
                // Jan to March (e.g., March 2026 -> "2025-26")
                finYear = (currentYear - 1) + "-" + (currentYear - 2000);
            }

            // 2. generate the sequence number
            // (Ideally, you should count estimates ONLY for this financial year, 
            // but simply counting all is okay for now to keep it unique)
            long count = estimateRepository.count() + 1;

            // 3. Set the formatted ID
            estimate.setEstimateNo("JMD/" + finYear + "/" + (140 + count));
        }
        
        return estimateRepository.save(estimate);
    }
    
    @Override
    public Estimate updateEstimate(String id, Estimate data) {
        Estimate existing = estimateRepository.findById(id).orElseThrow();
        
        existing.setEstimateNo(data.getEstimateNo()); // Allow updating ref no if needed
        existing.setEstimateDate(data.getEstimateDate());
        existing.setClientId(data.getClientId());
        existing.setClientName(data.getClientName());
        existing.setBillingAddress(data.getBillingAddress());
        existing.setSubject(data.getSubject());
        
        existing.setItems(data.getItems());
        existing.setSubTotal(data.getSubTotal());
        existing.setTaxAmount(data.getTaxAmount());
        existing.setTotal(data.getTotal());
        
        existing.setStatus(data.getStatus());
        existing.setNotes(data.getNotes());
        
        return estimateRepository.save(existing);
    }

    @Override
    public void deleteEstimate(String id) {
        estimateRepository.deleteById(id);
    }

    @Override
    public byte[] generateEstimatePdf(String id) throws Exception {
        Estimate estimate = estimateRepository.findById(id).orElseThrow();
        
        // Assuming ClientRepository/CompanyRepository are also MongoRepositories now
        // If Client not found, use empty object to avoid null pointers
        Client client = clientRepository.findById(estimate.getClientId()).orElse(new Client());
        Company company = companyRepository.findById("MY_COMPANY").orElse(new Company()); 

        Document document = new Document(PageSize.A4, 20, 20, 20, 20);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();

        // --- 1. COMPANY HEADER BOX ---
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{3, 1}); 

        PdfPCell companyInfo = new PdfPCell();
        companyInfo.setBorder(Rectangle.BOX);
        companyInfo.setPadding(5);
        companyInfo.addElement(new Paragraph("JMD DÉCOR", FONT_HEADER));
        companyInfo.addElement(new Paragraph(company.getAddress() != null ? company.getAddress() : "", FONT_SMALL));
        companyInfo.addElement(new Paragraph(company.getEmail() != null ? company.getEmail() : "", FONT_SMALL));
        companyInfo.addElement(new Paragraph("GST No.- " + (company.getGstin() != null ? company.getGstin() : "-"), FONT_BOLD));
        headerTable.addCell(companyInfo);

        PdfPCell emptyRef = new PdfPCell();
        emptyRef.setBorder(Rectangle.BOX);
        // You could put dynamic REF/DATE here if not using the rows below
        headerTable.addCell(emptyRef); 
        document.add(headerTable);

        // --- 2. CLIENT ROW ---
        PdfPTable clientRow = new PdfPTable(4);
        clientRow.setWidthPercentage(100);
        clientRow.setWidths(new float[]{0.5f, 4, 1, 1.5f});

        addCell(clientRow, "TO", FONT_RED, true, Color.WHITE);
        
        String clientDetails = estimate.getClientName() + "\n" + (estimate.getBillingAddress() != null ? estimate.getBillingAddress() : "");
        addCell(clientRow, clientDetails, FONT_BOLD, true, Color.WHITE);

        addCell(clientRow, "REF", FONT_BOLD, true, Color.WHITE);
        addCell(clientRow, estimate.getEstimateNo(), FONT_NORMAL, true, Color.WHITE);
        
        document.add(clientRow);
        
        // --- 3. GST & DATE ROW ---
        PdfPTable gstRow = new PdfPTable(4);
        gstRow.setWidthPercentage(100);
        gstRow.setWidths(new float[]{2, 2.5f, 1, 1.5f});
        
        // Use Estimate's captured GST or Client's current GST
        String gstVal = estimate.getGstin() != null ? estimate.getGstin() : (client.getGstin() != null ? client.getGstin() : "-");
        
        addCell(gstRow, "CLIENT GSTIN : " + gstVal, FONT_BOLD, true, Color.WHITE);
        addCell(gstRow, "", FONT_NORMAL, true, Color.WHITE); 
        addCell(gstRow, "DATE", FONT_BOLD, true, Color.WHITE);
        
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yy").withZone(ZoneId.systemDefault());
        String dateStr = estimate.getEstimateDate() != null ? dtf.format(estimate.getEstimateDate()) : "-";
        addCell(gstRow, dateStr, FONT_BOLD, true, Color.WHITE);
        document.add(gstRow);

        // --- 4. ATTENTION ROW ---
        PdfPTable attRow = new PdfPTable(1);
        attRow.setWidthPercentage(100);
        addCell(attRow, "KIND ATT.: " + (estimate.getAttention() != null ? estimate.getAttention() : " "), FONT_BOLD, true, Color.WHITE);
        document.add(attRow);

        // --- 5. SUBJECT / LOCATION HEADER ---
        PdfPTable subjectRow = new PdfPTable(1);
        subjectRow.setWidthPercentage(100);
        PdfPCell subjCell = new PdfPCell(new Phrase(estimate.getSubject() != null ? estimate.getSubject() : "PROJECT LOCATION : -", FONT_BOLD));
        subjCell.setBackgroundColor(Color.WHITE); 
        subjCell.setPadding(5);
        subjectRow.addCell(subjCell);
        document.add(subjectRow);

        // --- 6. ITEMS TABLE ---
        float[] colWidths = {0.8f, 5, 1.5f, 1.2f, 1, 1.5f, 2, 2, 2};
        PdfPTable itemTable = new PdfPTable(colWidths);
        itemTable.setWidthPercentage(100);
        
        String[] headers = {"Sr No.", "Description", "HSN CODE", "Total Unit", "Qty", "Rate", "Amount", "IGST@18%", "Total Amount"};
        for(String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, FONT_BOLD));
            c.setBackgroundColor(Color.YELLOW);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setVerticalAlignment(Element.ALIGN_MIDDLE);
            c.setPadding(4);
            itemTable.addCell(c);
        }

        int sr = 1;
        if (estimate.getItems() != null) {
            for (Estimate.EstimateItem item : estimate.getItems()) {
                double amount = item.getQty() * item.getRate();
                double taxVal = amount * (item.getTaxRate() / 100);
                double lineTotal = amount + taxVal;

                addCenterCell(itemTable, String.valueOf(sr++));
                addLeftCell(itemTable, item.getDescription());
                addCenterCell(itemTable, item.getHsnCode());
                addCenterCell(itemTable, item.getUnit() != null ? item.getUnit() : "-");
                addCenterCell(itemTable, String.valueOf(item.getQty()));
                addRightCell(itemTable, String.format("%.0f", item.getRate()));
                addRightCell(itemTable, String.format("%.0f", amount));
                addRightCell(itemTable, String.format("%.0f", taxVal));
                addRightCell(itemTable, String.format("%.0f", lineTotal));
            }
        }

        // Fill empty rows
        for(int i=0; i<3; i++) {
             for(int j=0; j<9; j++) {
                 PdfPCell c = new PdfPCell(new Phrase(" "));
                 c.setMinimumHeight(15);
                 itemTable.addCell(c);
             }
        }
        
        // --- TOTAL ROW ---
        PdfPCell blank = new PdfPCell(new Phrase(" ", FONT_BOLD));
        blank.setColspan(6); 
        itemTable.addCell(blank);

        addRightCell(itemTable, String.format("%.0f", estimate.getSubTotal())); 
        addRightCell(itemTable, String.format("%.0f", estimate.getTaxAmount())); 
        addRightCell(itemTable, String.format("%.2f", estimate.getTotal())); 

        document.add(itemTable);

        // --- 7. FOOTER (TERMS + BANK) ---
        PdfPTable footerTable = new PdfPTable(2);
        footerTable.setWidthPercentage(100);
        footerTable.setWidths(new float[]{2, 1});

        // LEFT: Terms
        PdfPCell termsCell = new PdfPCell();
        termsCell.setBorder(Rectangle.BOX);
        termsCell.setPadding(2);
        
        PdfPTable tTerms = new PdfPTable(2);
        tTerms.setWidthPercentage(100);
        tTerms.setWidths(new float[]{0.3f, 5});
        
        // You can fetch these from estimate.getNotes() if customized, or use hardcoded defaults
        if (estimate.getNotes() != null && !estimate.getNotes().isEmpty()) {
             addTermRow(tTerms, "1", estimate.getNotes());
        } else {
             addTermRow(tTerms, "1", "No any complaints will be accepted after Delivery Of Materials.");
             addTermRow(tTerms, "2", "Payment 50% in advance with purchase order and balance on completion of job.");
             addTermRow(tTerms, "3", "Specification,if any must be intimated in writing before issuing of purchase order.");
             addTermRow(tTerms, "4", "All co-Operation permission will be granted by you.");
             addTermRow(tTerms, "5", "Payment should be made in name of \" JMD DECOR \"");
        }
        
        termsCell.addElement(new Paragraph("TERMS & CONDITIONS", FONT_BOLD));
        termsCell.addElement(tTerms);
        footerTable.addCell(termsCell);

        // RIGHT: Bank/Sign
        PdfPCell rightFooter = new PdfPCell();
        rightFooter.setBorder(Rectangle.BOX);
        rightFooter.setPadding(0);

        PdfPCell grandTotal = new PdfPCell(new Phrase("Total    " + String.format("%.2f", estimate.getTotal()), FONT_BOLD));
        grandTotal.setPadding(5);
        grandTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        
        PdfPCell signCell = new PdfPCell();
        signCell.setBorder(Rectangle.NO_BORDER);
        signCell.setPadding(10);
        signCell.addElement(new Paragraph("YOUR FAITHFULLY", FONT_SMALL));
        signCell.addElement(new Paragraph("JMD DÉCOR", FONT_RED));
        signCell.addElement(Chunk.NEWLINE);
        signCell.addElement(new Paragraph("RAMESH YADAV", FONT_SMALL));
        signCell.addElement(new Paragraph("9819707090", FONT_SMALL));
        
        PdfPCell bankCell = new PdfPCell();
        bankCell.setBackgroundColor(Color.WHITE);
        Font blueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLUE);
        bankCell.addElement(new Paragraph(company.getBankName(), blueFont));
        bankCell.addElement(new Paragraph("ACCOUNT NO. " + company.getAccountNumber(), blueFont));
        bankCell.addElement(new Paragraph("IFSC CODE: " + company.getIfscCode(), blueFont));

        PdfPTable rightInner = new PdfPTable(1);
        rightInner.addCell(grandTotal);
        rightInner.addCell(signCell);
        rightInner.addCell(bankCell);
        
        rightFooter.addElement(rightInner);
        footerTable.addCell(rightFooter);

        document.add(footerTable);

        document.close();
        return out.toByteArray();
    }

    private void addCell(PdfPTable table, String text, Font font, boolean border, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        if(!border) cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(bg);
        cell.setPadding(3);
        table.addCell(cell);
    }
    
    private void addCenterCell(PdfPTable table, String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, FONT_NORMAL));
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(c);
    }
    private void addLeftCell(PdfPTable table, String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, FONT_NORMAL));
        c.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(c);
    }
    private void addRightCell(PdfPTable table, String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, FONT_NORMAL));
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c);
    }
    private void addTermRow(PdfPTable table, String sr, String text) {
        PdfPCell c1 = new PdfPCell(new Phrase(sr, FONT_SMALL));
        c1.setBorder(Rectangle.NO_BORDER);
        table.addCell(c1);
        PdfPCell c2 = new PdfPCell(new Phrase(text, FONT_SMALL));
        c2.setBorder(Rectangle.NO_BORDER);
        table.addCell(c2);
    }
}