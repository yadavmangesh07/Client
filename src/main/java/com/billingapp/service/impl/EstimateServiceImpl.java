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
import java.net.URL;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class EstimateServiceImpl implements EstimateService {

    private final EstimateRepository estimateRepository;
    private final ClientRepository clientRepository;
    private final CompanyRepository companyRepository;

    // --- FONTS ---
    private static final Font FONT_COMPANY_NAME = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
    private static final Font FONT_HEADER_RED = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(180, 50, 50));
    private static final Font FONT_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
    private static final Font FONT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
    private static final Font FONT_SMALL_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.BLACK);
    private static final Font FONT_SIGNATURE_RED = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(200, 0, 0));
    private static final Font FONT_BANK_BLUE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(0, 50, 150));
    private static final Font FONT_BOLD_BIG = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);

    public EstimateServiceImpl(EstimateRepository estimateRepository, ClientRepository clientRepository, CompanyRepository companyRepository) {
        this.estimateRepository = estimateRepository;
        this.clientRepository = clientRepository;
        this.companyRepository = companyRepository;
    }

    @Override
    public List<Estimate> getAllEstimates() { return estimateRepository.findAll(); }
    @Override
    public Estimate getEstimateById(String id) { return estimateRepository.findById(id).orElseThrow(); }
    @Override
    public void deleteEstimate(String id) { estimateRepository.deleteById(id); }

    @Override
    public Estimate createEstimate(Estimate estimate) {
        if (estimate.getEstimateNo() == null || estimate.getEstimateNo().isEmpty()) {
            java.time.LocalDate now = java.time.LocalDate.now();
            int currentYear = now.getYear();
            String finYear = (now.getMonthValue() >= 4) ? currentYear + "-" + (currentYear + 1 - 2000) : (currentYear - 1) + "-" + (currentYear - 2000);
            long count = estimateRepository.count() + 1;
            estimate.setEstimateNo("JMD/" + finYear + "/" + (140 + count));
        }
        return estimateRepository.save(estimate);
    }

    @Override
    public Estimate updateEstimate(String id, Estimate data) {
        Estimate existing = estimateRepository.findById(id).orElseThrow();
        existing.setEstimateNo(data.getEstimateNo());
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

    // --- PDF GENERATION ---
    @Override
    public byte[] generateEstimatePdf(String id) throws Exception {
        Estimate estimate = estimateRepository.findById(id).orElseThrow();
        Client client = clientRepository.findById(estimate.getClientId()).orElse(new Client());
        Company company = companyRepository.findById("MY_COMPANY").orElse(new Company());

        if(company.getCompanyName() == null) {
            company.setCompanyName("JMD DÉCOR");
            company.setAddress("210 ASHIRWAD INDUSTRIAL ESTATE BLDG. NO. -5 RAM MANDIR ROAD GOREGAON WEST MUMBAI 400104");
            company.setEmail("JMDSIGNAGE.2010@GMAIL.COM");
            company.setGstin("27AAOPY8409R1ZD");
            company.setPhone("9819707090 / 9322821737");
            company.setBankName("KOTAK MAHINDRA BANK");
            company.setAccountNumber("9111365107");
            company.setIfscCode("KKBK0000643");
        }

        Document document = new Document(PageSize.A4, 15, 15, 15, 15);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();

        // 1. HEADER
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{3.5f, 1});

        PdfPCell companyCell = new PdfPCell();
        companyCell.setBorder(Rectangle.BOX);
        companyCell.setPadding(5);
        companyCell.addElement(new Paragraph(company.getCompanyName(), FONT_COMPANY_NAME));
        companyCell.addElement(new Paragraph(company.getAddress(), FONT_NORMAL));
        companyCell.addElement(new Paragraph(company.getPhone() + " / " + company.getEmail(), FONT_NORMAL));
        companyCell.addElement(new Paragraph("GST No.- " + company.getGstin(), FONT_NORMAL));
        headerTable.addCell(companyCell);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.BOX);
        logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        
        try {
            Image img = null;
            if (company.getLogoUrl() != null && !company.getLogoUrl().isBlank()) {
                try { img = Image.getInstance(company.getLogoUrl()); } catch (Exception e) {}
            }
            if (img == null) {
                URL logoResource = getClass().getResource("/logo.png");
                if (logoResource != null) img = Image.getInstance(logoResource);
            }
            if (img != null) {
                img.scaleToFit(80, 50);
                img.setAlignment(Element.ALIGN_CENTER);
                logoCell.addElement(img);
            } else { 
                throw new RuntimeException("No img"); 
            }
        } catch (Exception e) {
            Paragraph p = new Paragraph("JMD\nSIGNAGE", FONT_BOLD_BIG);
            p.setAlignment(Element.ALIGN_CENTER);
            logoCell.addElement(p);
        }
        headerTable.addCell(logoCell);
        document.add(headerTable);

        // 2. CLIENT & REF DETAILS
        PdfPTable infoTable = new PdfPTable(4);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{3, 3, 0.5f, 1.5f});

        addCell(infoTable, "BILLED TO", FONT_HEADER_RED, Color.WHITE, 1);
        addCell(infoTable, "SHIPPED TO", FONT_HEADER_RED, Color.WHITE, 3);

        String billingInfo = estimate.getClientName() + "\n" + (estimate.getBillingAddress() != null ? estimate.getBillingAddress() : "");
        String shippingInfo = estimate.getClientName() + "\n" + (estimate.getBillingAddress() != null ? estimate.getBillingAddress() : ""); 
        
        addCell(infoTable, billingInfo, FONT_BOLD, Color.WHITE, 1);
        addCell(infoTable, shippingInfo, FONT_BOLD, Color.WHITE, 1);
        
        addCell(infoTable, "REF", FONT_BOLD, Color.WHITE, 1);
        addCell(infoTable, estimate.getEstimateNo(), FONT_NORMAL, Color.WHITE, 1);

        String clientGst = estimate.getGstin() != null ? estimate.getGstin() : (client.getGstin() != null ? client.getGstin() : "");
        
        addCell(infoTable, "CLIENT GSTIN: " + clientGst, FONT_BOLD, Color.WHITE, 1);
        addCell(infoTable, "CLIENT GSTIN: " + clientGst, FONT_BOLD, Color.WHITE, 1);
        
        addCell(infoTable, "Date", FONT_BOLD, Color.WHITE, 1);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("M/d/yy").withZone(ZoneId.systemDefault());
        addCell(infoTable, estimate.getEstimateDate() != null ? dtf.format(estimate.getEstimateDate()) : "-", FONT_NORMAL, Color.WHITE, 1);

        String attn = "KIND ATTENTION: " + (estimate.getSubject() != null ? "" : ""); 
        addCell(infoTable, attn, FONT_BOLD, Color.WHITE, 4);

        String loc = "PROJECT LOCATION : " + (estimate.getSubject() != null ? estimate.getSubject() : "");
        addCell(infoTable, loc, FONT_BOLD, Color.WHITE, 4);

        document.add(infoTable);

        // 3. ITEMS TABLE
        float[] itemWidths = {0.6f, 5f, 1f, 1f, 1f, 1.5f, 1.5f, 1.5f, 2f};
        PdfPTable itemTable = new PdfPTable(itemWidths);
        itemTable.setWidthPercentage(100);

        String[] headers = {"Sr\nNo.", "Description", "HSN\nCode", "Total\nUnit", "Qty", "Rate", "Amount", "IGST\n18%", "Total Amount"};
        for(String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, FONT_SMALL_BOLD));
            c.setBackgroundColor(new Color(255, 255, 100)); // Yellow
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setVerticalAlignment(Element.ALIGN_MIDDLE);
            c.setPadding(3);
            itemTable.addCell(c);
        }

        int sr = 1;
        double subTotal = 0;
        double totalTax = 0;
        
        if (estimate.getItems() != null) {
            for (Estimate.EstimateItem item : estimate.getItems()) {
                double amount = item.getQty() * item.getRate();
                double tax = amount * (item.getTaxRate() / 100.0);
                double lineTotal = amount + tax;
                subTotal += amount;
                totalTax += tax;

                addCenterCell(itemTable, String.valueOf(sr++));
                addLeftCell(itemTable, item.getDescription());
                addCenterCell(itemTable, item.getHsnCode() != null ? item.getHsnCode() : "");
                addCenterCell(itemTable, "1");
                addCenterCell(itemTable, String.valueOf(item.getQty()));
                addRightCell(itemTable, String.format("%.0f", item.getRate()));
                addRightCell(itemTable, String.format("%.0f", amount));
                addRightCell(itemTable, String.format("%.0f", tax));
                addRightCell(itemTable, String.format("%.0f", lineTotal));
            }
        }

        for(int i=0; i<3; i++) {
             for(int j=0; j<9; j++) {
                 PdfPCell c = new PdfPCell(new Phrase(" "));
                 c.setMinimumHeight(15);
                 itemTable.addCell(c);
             }
        }

        // --- TOTAL ROW ---
        PdfPCell totalLabel = new PdfPCell(new Phrase("Total", FONT_BOLD));
        totalLabel.setColspan(6);
        totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        itemTable.addCell(totalLabel);
        
        addRightCell(itemTable, String.format("%.0f", subTotal));
        addRightCell(itemTable, String.format("%.1f", totalTax));
        // Added Total
        PdfPCell grandTotalCell = new PdfPCell(new Phrase(String.format("%.2f", subTotal + totalTax), FONT_BOLD));
        grandTotalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        itemTable.addCell(grandTotalCell);

        document.add(itemTable);

        // ============================================
        // 4. FOOTER (Left: Terms & Bank, Right: Signature)
        // ============================================
        PdfPTable footerTable = new PdfPTable(2);
        footerTable.setWidthPercentage(100);
        footerTable.setWidths(new float[]{2, 1}); // 2:1 Ratio

        // --- LEFT COLUMN: TERMS + BANK ---
        PdfPCell leftContainer = new PdfPCell();
        leftContainer.setBorder(Rectangle.BOX);
        leftContainer.setPadding(0);

        // A. Terms Table
        PdfPTable termsTable = new PdfPTable(2);
        termsTable.setWidthPercentage(100);
        termsTable.setWidths(new float[]{0.3f, 5});

        PdfPCell termHeader = new PdfPCell(new Phrase("TERMS & CONDITIONS", FONT_BOLD));
        termHeader.setColspan(2);
        termHeader.setBorder(Rectangle.BOTTOM);
        termHeader.setBackgroundColor(new Color(240, 240, 240));
        termHeader.setPadding(3);
        termsTable.addCell(termHeader);

        String[] defaultTerms = {
            "Payment 50% in advance with purchase order and balance on completion of job.",
            "Specification,if any must be intimated in writing before issuing of purchase order.",
            "All co-operation permission will be granted by you.",
            "Payment should be made in name of \" " + company.getCompanyName() + "\"",
            "Electrical point up to the sign to be provided by the Client.",
            "JMD is not responsible for any Damage occurred to sign, due to Natural calamities",
            "Like Wind storming, cyclone, heavy rains, vandalism etc, as these are beyond our control.",
            "Completion of job 20 to 30 days after confirming order"
        };
        
        int tSr = 1;
        String[] actualTerms = (estimate.getNotes() != null && !estimate.getNotes().isEmpty()) 
                                ? estimate.getNotes().split("\n") 
                                : defaultTerms;
                                
        for(String t : actualTerms) {
            PdfPCell c1 = new PdfPCell(new Phrase(String.valueOf(tSr++), FONT_NORMAL));
            c1.setBorder(Rectangle.BOTTOM | Rectangle.RIGHT);
            c1.setHorizontalAlignment(Element.ALIGN_CENTER);
            termsTable.addCell(c1);
            
            PdfPCell c2 = new PdfPCell(new Phrase(t, FONT_NORMAL));
            c2.setBorder(Rectangle.BOTTOM);
            termsTable.addCell(c2);
        }
        
        leftContainer.addElement(termsTable);

        // B. Bank Details (Below Terms)
        PdfPTable bankTable = new PdfPTable(1);
        bankTable.setWidthPercentage(100);
        
        PdfPCell bankCell = new PdfPCell();
        bankCell.setBorder(Rectangle.TOP); // Separator line from terms
        bankCell.setPadding(5);
        
        Paragraph bankP = new Paragraph();
        bankP.setAlignment(Element.ALIGN_LEFT);
        bankP.add(new Paragraph("BANK DETAILS:", FONT_BOLD));
        bankP.add(new Paragraph(company.getBankName(), FONT_BANK_BLUE));
        bankP.add(new Paragraph("ACC NO. " + company.getAccountNumber(), FONT_BANK_BLUE));
        bankP.add(new Paragraph("IFSC: " + company.getIfscCode(), FONT_BANK_BLUE));
        bankCell.addElement(bankP);
        
        bankTable.addCell(bankCell);
        leftContainer.addElement(bankTable);
        
        footerTable.addCell(leftContainer);

        // --- RIGHT COLUMN: SIGNATURE (Full Height) ---
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.BOX);
        rightCell.setPadding(0);
        rightCell.setVerticalAlignment(Element.ALIGN_MIDDLE); // Key for vertical center
        rightCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        
        // Use a table to force centering if cell alignment isn't enough
        PdfPTable signTable = new PdfPTable(1);
        signTable.setWidthPercentage(100);
        
        PdfPCell signInner = new PdfPCell();
        signInner.setBorder(Rectangle.NO_BORDER);
        signInner.setMinimumHeight(150); // Ensure it matches height of left column roughly or stretches
        signInner.setVerticalAlignment(Element.ALIGN_MIDDLE);
        signInner.setHorizontalAlignment(Element.ALIGN_CENTER);
        
        Paragraph signP = new Paragraph();
        signP.setAlignment(Element.ALIGN_CENTER);
        signP.add(new Paragraph("YOUR'S FAITHFULLY", FONT_BOLD));
        signP.add(new Paragraph(company.getCompanyName(), FONT_SIGNATURE_RED));
        signP.add(Chunk.NEWLINE);
        signP.add(Chunk.NEWLINE);
        signP.add(Chunk.NEWLINE);
        signP.add(new Paragraph("RAMESH YADAV", FONT_NORMAL));
        signP.add(new Paragraph("9819707090", FONT_NORMAL));
        
        signInner.addElement(signP);
        signTable.addCell(signInner);
        
        rightCell.addElement(signTable);
        footerTable.addCell(rightCell);

        document.add(footerTable);

        document.close();
        return out.toByteArray();
    }

    // --- HELPER METHODS ---
    private void addCell(PdfPTable table, String text, Font font, Color bg, int colspan) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setColspan(colspan);
        cell.setPadding(3);
        table.addCell(cell);
    }

    private void addCenterCell(PdfPTable table, String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, FONT_NORMAL));
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(3);
        table.addCell(c);
    }
    private void addLeftCell(PdfPTable table, String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, FONT_NORMAL));
        c.setHorizontalAlignment(Element.ALIGN_LEFT);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(3);
        table.addCell(c);
    }
    private void addRightCell(PdfPTable table, String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, FONT_NORMAL));
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(3);
        table.addCell(c);
    }
}