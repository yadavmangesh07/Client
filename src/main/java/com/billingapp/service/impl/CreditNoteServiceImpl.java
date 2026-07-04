package com.billingapp.service.impl;

import com.billingapp.entity.Client;
import com.billingapp.entity.Company;
import com.billingapp.entity.CreditNote;
import com.billingapp.repository.ClientRepository;
import com.billingapp.repository.CompanyRepository;
import com.billingapp.repository.CreditNoteRepository;
import com.billingapp.service.CreditNoteService;
import com.billingapp.service.DashboardService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@Transactional
public class CreditNoteServiceImpl implements CreditNoteService {

    private final CreditNoteRepository creditNoteRepository;
    private final ClientRepository clientRepository;
    private final CompanyRepository companyRepository;
    private final DashboardService dashboardService;

    public CreditNoteServiceImpl(CreditNoteRepository creditNoteRepository,
                                 ClientRepository clientRepository,
                                 CompanyRepository companyRepository,
                                 DashboardService dashboardService) {
        this.creditNoteRepository = creditNoteRepository;
        this.clientRepository = clientRepository;
        this.companyRepository = companyRepository;
        this.dashboardService = dashboardService;
    }

    @Override
    @CacheEvict(value = "credit_notes", allEntries = true)
    public CreditNote create(CreditNote cn) {
        log.info("Attempting to create Credit Note profile record: {}", cn.getCreditNoteNo());
        if (cn.getCreditNoteNo() == null || cn.getCreditNoteNo().isBlank()) {
            log.warn("Credit Note creation aborted: missing transaction document reference code");
            throw new IllegalArgumentException("Credit Note number is required");
        }
        if (creditNoteRepository.existsByCreditNoteNo(cn.getCreditNoteNo())) {
            log.warn("Credit Note creation aborted: tracking identifier target code {} already exists", cn.getCreditNoteNo());
            throw new IllegalArgumentException("Credit Note number already exists");
        }
        cn.setCreatedAt(Instant.now());
        cn.setUpdatedAt(Instant.now());
        CreditNote saved = creditNoteRepository.save(cn);
        log.info("Credit Note tracking token successfully written to storage layer with inner record ID: {}", saved.getId());
        dashboardService.clearDashboardCache();
        return saved;
    }

    @Override
    @CacheEvict(value = "credit_notes", allEntries = true)
    public CreditNote update(String id, CreditNote data) {
        log.info("Attempting to commit transaction modification delta configurations on Credit Note reference ID: {}", id);
        CreditNote existing = creditNoteRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Update payload rejected: target mapping entity matching code ID {} doesn't exist", id);
                    return new IllegalArgumentException("Credit Note not found: " + id);
                });

        if (data.getCreditNoteNo() != null && !data.getCreditNoteNo().equals(existing.getCreditNoteNo())) {
            if (creditNoteRepository.existsByCreditNoteNo(data.getCreditNoteNo())) {
                log.warn("Update mutation failed: collision vector triggered for modification code {}", data.getCreditNoteNo());
                throw new IllegalArgumentException("New Credit Note number already exists");
            }
            existing.setCreditNoteNo(data.getCreditNoteNo());
        }

        existing.setStatus(data.getStatus());
        existing.setCreditNoteDate(data.getCreditNoteDate());
        existing.setBillReferenceNo(data.getBillReferenceNo());
        existing.setBillReferenceDate(data.getBillReferenceDate());
        existing.setPoNumber(data.getPoNumber());
        existing.setPoDate(data.getPoDate());
        existing.setTransportMode(data.getTransportMode());
        existing.setEwayBillNo(data.getEwayBillNo());
        existing.setScnNo(data.getScnNo());
        existing.setClientId(data.getClientId());
        existing.setClientName(data.getClientName());
        existing.setBillingAddress(data.getBillingAddress());
        existing.setShippingAddress(data.getShippingAddress());
        existing.setClientGst(data.getClientGst());
        existing.setClientState(data.getClientState());
        existing.setClientStateCode(data.getClientStateCode());
        existing.setItems(data.getItems());
        existing.setSubtotal(data.getSubtotal());
        existing.setCgstRate(data.getCgstRate());
        existing.setCgstAmount(data.getCgstAmount());
        existing.setSgstRate(data.getSgstRate());
        existing.setSgstAmount(data.getSgstAmount());
        existing.setTotalAmount(data.getTotalAmount());
        existing.setRupeesInWords(data.getRupeesInWords());
        existing.setUpdatedAt(Instant.now());

        CreditNote saved = creditNoteRepository.save(existing);
        log.info("Credit Note state modifications for target document identity matching ID {} successfully persisted", id);
        dashboardService.clearDashboardCache();
        return saved;
    }

    @Override
    @Cacheable(value = "credit_notes", key = "#id")
    public CreditNote getById(String id) {
        log.debug("Executing structured data mapping find lookup query for parameter ID: {}", id);
        return creditNoteRepository.findById(id).orElseThrow(() -> {
            log.error("Data lookup request failed: Credit Note record signature ID {} is non-existent", id);
            return new IllegalArgumentException("Not found: " + id);
        });
    }

    @Override
    @Cacheable(value = "credit_notes")
    public List<CreditNote> getAll() { 
        log.debug("Executing collective lookup query for all credit note records");
        return creditNoteRepository.findAll(); 
    }

    @Override
    @CacheEvict(value = "credit_notes", allEntries = true)
    public void delete(String id) {
        log.info("Initiating structural row cache eviction sequence for Credit Note token code ID: {}", id);
        creditNoteRepository.deleteById(id);
        log.info("Credit Note index successfully dropped for row ID: {}", id);
        dashboardService.clearDashboardCache();
    }

    @Override
    @Cacheable(value = "credit_notes", key = "'pdf-' + #id")
    public byte[] generatePdf(String id) throws Exception {
        log.info("Initiating structural PDF generation engine context pipeline for Credit Note token ID: {}", id);
        CreditNote cn = creditNoteRepository.findById(id).orElseThrow(() -> {
            log.error("PDF engine pipeline aborted: document reference entity mapping code ID {} non-existent", id);
            return new IllegalArgumentException("Credit Note not found");
        });
        Client client = clientRepository.findById(cn.getClientId()).orElse(new Client());
        Company company = companyRepository.findById("MY_COMPANY").orElse(new Company());

        if (company.getCompanyName() == null) {
            company.setCompanyName("JMD DÉCOR");
            company.setAddress("210 ASHIRWAD INDUSTRIAL ESTATE BLDG. NO.-5 RAM MANDIR ROAD, GOREGAON WEST MUMBAI 400104");
            company.setEmail("jmdsignage.2010@gmail.com");
            company.setGstin("27AAOPY8409R1ZD");
            company.setUdyamRegNo("MH-19-0044729");
            company.setPhone("9819707090/9322821737");
            company.setBankName("Kotak Mahindra Bank");
            company.setAccountNumber("9111365107");
            company.setIfscCode("KKBK0000643");
            company.setBranch("Jawahar Nagar Mumbai 400062");
        }

        // Setup Document parameters
        Document document = new Document(PageSize.A4, 15, 15, 15, 15);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        // Canvas Palette
        Color headerYellow = new Color(255, 255, 0);
        Color redText = new Color(180, 50, 50);

        Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
        Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
        Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
        Font fontRed = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, redText);
        Font fontBoldBig = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy").withZone(ZoneId.systemDefault());

        // Header Titles
        Paragraph title = new Paragraph("CREDIT NOTE", fontHeader);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        
        Paragraph subTitle = new Paragraph("Original For Recipient", fontNormal);
        subTitle.setAlignment(Element.ALIGN_CENTER);
        subTitle.setSpacingAfter(8);
        document.add(subTitle);

        // UNIFIED MASTER ARCHITECTURE: Strict 10-column framework tracking
        float[] masterColumnWidths = {0.5f, 4.5f, 1f, 0.8f, 0.6f, 1f, 1.2f, 1.1f, 1.1f, 1.5f};
        
        // --- SECTION 1: COMPANY MATRICES & BRAND LOGO GRAPHIC ---
        PdfPTable mainTable = new PdfPTable(masterColumnWidths);
        mainTable.setWidthPercentage(100);

        PdfPCell compCell = new PdfPCell();
        compCell.setColspan(7); 
        compCell.setPadding(6);
        compCell.addElement(new Paragraph(company.getCompanyName(), fontHeader));
        compCell.addElement(new Paragraph(company.getAddress(), fontNormal));
        compCell.addElement(new Paragraph(company.getPhone() + "  |  " + company.getEmail(), fontNormal));
        compCell.addElement(new Paragraph("GST: " + company.getGstin() + "   UADYAM- " + company.getUdyamRegNo(), fontBold));
        mainTable.addCell(compCell);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setColspan(3); 
        logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        try {
            Image img = null;
            if (company.getLogoUrl() != null && !company.getLogoUrl().isBlank()) {
                try { img = Image.getInstance(company.getLogoUrl()); } catch (Exception ignored) {}
            }
            if (img == null) {
                URL logoResource = getClass().getResource("/logo.png");
                if (logoResource != null) img = Image.getInstance(logoResource);
            }
            if (img != null) {
                img.scaleToFit(90, 55);
                img.setAlignment(Element.ALIGN_CENTER);
                logoCell.addElement(img);
            } else {
                throw new RuntimeException();
            }
        } catch (Exception e) {
            log.debug("Bypassing brand layout graphic insertion: exception encountered during asset extraction", e);
            Paragraph p = new Paragraph("JMD\nDÉCOR", fontBoldBig);
            p.setAlignment(Element.ALIGN_CENTER);
            logoCell.addElement(p);
        }
        mainTable.addCell(logoCell);
        document.add(mainTable);

        // --- SECTION 2: METADATA SEGMENTATION FIELDS ---
        PdfPTable metaTable = new PdfPTable(masterColumnWidths);
        metaTable.setWidthPercentage(100);

        addUnifiedMetaRow(metaTable, "CREDIT NOTE NO:", cn.getCreditNoteNo(), "Date:", cn.getCreditNoteDate() != null ? dtf.format(cn.getCreditNoteDate()) : "-", fontBold, fontNormal);
        addUnifiedMetaRow(metaTable, "Bill Reference No:", cn.getBillReferenceNo(), "Date:", cn.getBillReferenceDate() != null ? dtf.format(cn.getBillReferenceDate()) : "-", fontBold, fontNormal);
        addUnifiedMetaRow(metaTable, "State:", cn.getClientState(), "Code:", cn.getClientStateCode(), fontBold, fontNormal);
        addUnifiedMetaRow(metaTable, "E-way Bill:", cn.getEwayBillNo(), "Mode Of Transport:", cn.getTransportMode(), fontBold, fontNormal);
        addUnifiedMetaRow(metaTable, "SCN NO.:", cn.getScnNo(), "P.O:", cn.getPoNumber(), fontBold, fontNormal);
        addUnifiedMetaRow(metaTable, "Date:", cn.getPoDate() != null ? dtf.format(cn.getPoDate()) : "-", "", "", fontBold, fontNormal);
        document.add(metaTable);

        // --- SECTION 3: ADDRESSES & ISOLATED SNAPSHOT BLOCKS ---
        PdfPTable addressLabelTable = new PdfPTable(masterColumnWidths);
        addressLabelTable.setWidthPercentage(100);
        
        PdfPCell bLabel = new PdfPCell(new Phrase("BILLED TO", fontRed));
        bLabel.setColspan(5); bLabel.setPadding(4); addressLabelTable.addCell(bLabel);
        PdfPCell sLabel = new PdfPCell(new Phrase("SHIPPED TO", fontRed));
        sLabel.setColspan(3); sLabel.setPadding(4); addressLabelTable.addCell(sLabel);
        PdfPCell stLabel = new PdfPCell(new Phrase("State Code: " + (cn.getClientStateCode() != null ? cn.getClientStateCode() : ""), fontBold));
        stLabel.setColspan(2); stLabel.setHorizontalAlignment(Element.ALIGN_CENTER); stLabel.setPadding(4); addressLabelTable.addCell(stLabel);
        document.add(addressLabelTable);

        // Addresses Text Area Block
        PdfPTable addressBlockTable = new PdfPTable(masterColumnWidths);
        addressBlockTable.setWidthPercentage(100);
        
        PdfPCell bAddr = new PdfPCell(new Phrase(cn.getClientName() + "\n" + cn.getBillingAddress(), fontNormal));
        bAddr.setColspan(5); bAddr.setPadding(5); bAddr.setMinimumHeight(45); addressBlockTable.addCell(bAddr);
        PdfPCell sAddr = new PdfPCell(new Phrase(cn.getClientName() + "\n" + cn.getShippingAddress(), fontNormal));
        sAddr.setColspan(5); sAddr.setPadding(5); sAddr.setMinimumHeight(45); addressBlockTable.addCell(sAddr);
        document.add(addressBlockTable);

        // Dedicated State Field Row
        PdfPTable stateRowTable = new PdfPTable(masterColumnWidths);
        stateRowTable.setWidthPercentage(100);
        
        PdfPCell bState = new PdfPCell(new Phrase("State: " + cn.getClientState(), fontBold));
        bState.setColspan(5); bState.setPadding(4); stateRowTable.addCell(bState);
        PdfPCell sState = new PdfPCell(new Phrase("State: " + cn.getClientState(), fontBold));
        sState.setColspan(5); sState.setPadding(4); stateRowTable.addCell(sState);
        document.add(stateRowTable);

        // Dedicated GSTIN Code Row
        PdfPTable gstRowTable = new PdfPTable(masterColumnWidths);
        gstRowTable.setWidthPercentage(100);
        String activeGst = (cn.getClientGst() != null && !cn.getClientGst().isBlank()) ? cn.getClientGst() : client.getGstin();
        
        PdfPCell bGst = new PdfPCell(new Phrase("GST NO : " + activeGst, fontBold));
        bGst.setColspan(5); bGst.setPadding(4); gstRowTable.addCell(bGst);
        PdfPCell sGst = new PdfPCell(new Phrase("GST NO : " + activeGst, fontBold));
        sGst.setColspan(5); sGst.setPadding(4); gstRowTable.addCell(sGst);
        document.add(gstRowTable);

        // --- SECTION 4: PRODUCT SPECIFICATIONS GRID ---
        PdfPTable itemTable = new PdfPTable(masterColumnWidths);
        itemTable.setWidthPercentage(100);

        String[] headers = {"Sr\nNo:", "Description", "HSN", "UOM", "Qty", "Rate", "Amount", "CGST\n@9%", "SGST\n@9%", "Total\nAmount"};
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, fontBold));
            c.setBackgroundColor(headerYellow);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setVerticalAlignment(Element.ALIGN_MIDDLE);
            c.setPadding(5);
            itemTable.addCell(c);
        }

        int index = 1;
        if (cn.getItems() != null) {
            for (CreditNote.CreditNoteItem item : cn.getItems()) {
                double itemAmount = item.getQty() * item.getRate();
                // Dynamically uses the tax percentage value configured on the individual item line context
                double itemTaxPct = item.getTaxPercent() > 0 ? item.getTaxPercent() : 18.0;
                double lineCgst = itemAmount * ((itemTaxPct / 2) / 100.0);
                double lineSgst = itemAmount * ((itemTaxPct / 2) / 100.0);
                double lineTotal = itemAmount + lineCgst + lineSgst;

                itemTable.addCell(createCenterCell(String.valueOf(index++), fontNormal));
                itemTable.addCell(createLeftCell(item.getDescription(), fontNormal));
                itemTable.addCell(createCenterCell(item.getHsn(), fontNormal));
                itemTable.addCell(createCenterCell(item.getUom(), fontNormal));
                itemTable.addCell(createCenterCell(String.valueOf(item.getQty()), fontNormal));
                itemTable.addCell(createRightCell(String.format("%.0f", item.getRate()), fontNormal));
                itemTable.addCell(createRightCell(String.format("%.0f", itemAmount), fontNormal));
                itemTable.addCell(createRightCell(String.format("%.1f", lineCgst), fontNormal));
                itemTable.addCell(createRightCell(String.format("%.1f", lineSgst), fontNormal));
                itemTable.addCell(createRightCell(String.format("%.0f", lineTotal), fontNormal));
            }
        }

        // Generate structural empty padding rows
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                PdfPCell empty = new PdfPCell(new Phrase(" "));
                empty.setMinimumHeight(16);
                itemTable.addCell(empty);
            }
        }

        // --- SECTION 4 SUMMARY: 2-COLUMN SPLIT FOR VERTICAL BORDER DIVIDER ---
        String rupeesText = com.billingapp.util.NumberToWords.convert(cn.getTotalAmount());

        PdfPCell finalSummaryCell = new PdfPCell();
        finalSummaryCell.setColspan(6);
        finalSummaryCell.setPadding(0);
        finalSummaryCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        PdfPTable innerSummaryTable = new PdfPTable(new float[]{5.2f, 1.0f});
        innerSummaryTable.setWidthPercentage(100);

        // Column 1 (Left Side): Amount Chargeable text details
        PdfPCell innerCellWords = new PdfPCell();
        innerCellWords.setBorder(Rectangle.RIGHT); 
        innerCellWords.setBorderWidthRight(0.5f);
        innerCellWords.setBorderColorRight(Color.BLACK);
        innerCellWords.setPadding(6);
        innerCellWords.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph pWords = new Paragraph("Amount Chargeable (in words):\n" + rupeesText, fontBold);
        pWords.setAlignment(Element.ALIGN_CENTER);
        innerCellWords.addElement(pWords);
        innerSummaryTable.addCell(innerCellWords);

        // Column 2 (Right Side): "Total" text tracking block anchored right next to calculations
        PdfPCell innerCellTotal = new PdfPCell();
        innerCellTotal.setBorder(Rectangle.NO_BORDER);
        innerCellTotal.setPadding(6);
        innerCellTotal.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph pTotalLabel = new Paragraph("Total", fontBold);
        pTotalLabel.setAlignment(Element.ALIGN_RIGHT); 
        innerCellTotal.addElement(pTotalLabel);
        innerSummaryTable.addCell(innerCellTotal);

        finalSummaryCell.addElement(innerSummaryTable);
        itemTable.addCell(finalSummaryCell);
        
        // Render calculation totals columns centered inside their grid parameters
        itemTable.addCell(createCenterCell(String.format("%.0f", cn.getSubtotal()), fontBold));
        itemTable.addCell(createCenterCell(String.format("%.0f", cn.getCgstAmount()), fontBold));
        itemTable.addCell(createCenterCell(String.format("%.0f", cn.getSgstAmount()), fontBold));
        
        PdfPCell totalGrandSumCell = new PdfPCell(new Phrase(String.format("%.2f", cn.getTotalAmount()), fontBold));
        totalGrandSumCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        totalGrandSumCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        totalGrandSumCell.setPadding(4);
        itemTable.addCell(totalGrandSumCell);

        document.add(itemTable);

        // --- SECTION 5: ACCOUNT DECLARATIONS & SYSTEM CONTROLS FOOTER ---
        PdfPTable finalFooter = new PdfPTable(masterColumnWidths);
        finalFooter.setWidthPercentage(100);

        // Left block layout wrapper cell
        PdfPCell bankAndDecCell = new PdfPCell();
        bankAndDecCell.setColspan(7); 
        bankAndDecCell.setPadding(0); // Clear layout bounds outer padding to align subtable borders cleanly

        // Create an inner 1-column subtable inside the left footer column to inject an explicit border line 
        PdfPTable innerFooterTable = new PdfPTable(1);
        innerFooterTable.setWidthPercentage(100);

        // Part 1: Top Cell containing Bank details
        PdfPCell innerBankCell = new PdfPCell();
        innerBankCell.setBorder(Rectangle.NO_BORDER);
        innerBankCell.setPadding(6);
        innerBankCell.addElement(new Paragraph("Bank: " + company.getBankName() + " Account No: " + company.getAccountNumber() + " IFSC Code: " + company.getIfscCode(), fontBold));
        innerBankCell.addElement(new Paragraph("Branch: " + company.getBranch(), fontBold));
        innerFooterTable.addCell(innerBankCell);

        // Part 2: Bottom Cell containing Declaration note (with top line border separator active)
        PdfPCell innerDecCell = new PdfPCell();
        innerDecCell.setBorder(Rectangle.TOP); 
        innerDecCell.setBorderWidthTop(0.5f);
        innerDecCell.setBorderColorTop(Color.BLACK);
        innerDecCell.setPadding(6);
        innerDecCell.addElement(new Paragraph("Declaration: We Declare that this Credit Note shows the actual price of the Goods Described and that all particulars are true and correct.", fontNormal));
        innerFooterTable.addCell(innerDecCell);

        bankAndDecCell.addElement(innerFooterTable);
        finalFooter.addCell(bankAndDecCell);

        // Right block layout wrapper cell (Signature)
        PdfPCell signCell = new PdfPCell();
        signCell.setColspan(3); 
        signCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        signCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
        signCell.setPaddingBottom(10);
        Paragraph pProp = new Paragraph("For JMD DÉCOR\n\n\n\nPROPRIETOR", fontBold);
        pProp.setAlignment(Element.ALIGN_CENTER);
        signCell.addElement(pProp);
        finalFooter.addCell(signCell);
        
        document.add(finalFooter);
        document.close();
        
        log.info("PDF generation successfully completed for Credit Note ID: {}", id);
        return out.toByteArray();
    }

    private void addUnifiedMetaRow(PdfPTable table, String l1, String v1, String l2, String v2, Font b, Font n) {
        PdfPCell cellL1 = new PdfPCell(new Phrase(l1, b)); cellL1.setColspan(2); cellL1.setPadding(3); table.addCell(cellL1);
        PdfPCell cellV1 = new PdfPCell(new Phrase(v1 != null ? v1 : "", n)); cellV1.setColspan(3); cellV1.setPadding(3); table.addCell(cellV1);
        PdfPCell cellL2 = new PdfPCell(new Phrase(l2, b)); cellL2.setColspan(2); cellL2.setPadding(3); table.addCell(cellL2);
        PdfPCell cellV2 = new PdfPCell(new Phrase(v2 != null ? v2 : "", n)); cellV2.setColspan(3); cellV2.setPadding(3); table.addCell(cellV2);
    }

    private PdfPCell createCenterCell(String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f)); c.setHorizontalAlignment(Element.ALIGN_CENTER); c.setVerticalAlignment(Element.ALIGN_MIDDLE); return c;
    }
    private PdfPCell createLeftCell(String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f)); c.setVerticalAlignment(Element.ALIGN_MIDDLE); return c;
    }
    private PdfPCell createRightCell(String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f)); c.setHorizontalAlignment(Element.ALIGN_RIGHT); c.setVerticalAlignment(Element.ALIGN_MIDDLE); return c;
    }
}