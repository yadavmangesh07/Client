package com.billingapp.service.impl;

import com.billingapp.entity.Challan;
import com.billingapp.entity.Company;
import com.billingapp.repository.ChallanRepository;
import com.billingapp.repository.CompanyRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;

@Service
public class ChallanPdfServiceImpl {

    private final ChallanRepository challanRepository;
    private final CompanyRepository companyRepository;

    // Fonts
    private static final Font FONT_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
    private static final Font FONT_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final Font FONT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);

    public ChallanPdfServiceImpl(ChallanRepository challanRepository, CompanyRepository companyRepository) {
        this.challanRepository = challanRepository;
        this.companyRepository = companyRepository;
    }

    public byte[] generateChallanPdf(String challanId) throws Exception {
        Challan challan = challanRepository.findById(challanId)
                .orElseThrow(() -> new IllegalArgumentException("Challan not found"));

        Company company = companyRepository.findById("MY_COMPANY").orElse(new Company());

        Document document = new Document(PageSize.A4, 20, 20, 20, 20);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

        // --- 1. TOP HEADER (Logo & Title) ---
        PdfPTable topTable = new PdfPTable(1);
        topTable.setWidthPercentage(100);
        
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        try {
            Image img = null;
            if (company.getLogoUrl() != null && !company.getLogoUrl().isBlank()) {
                try { img = Image.getInstance(company.getLogoUrl()); } catch (Exception e) {}
            }
            if (img == null) {
                URL resource = getClass().getResource("/logo.png");
                if(resource != null) img = Image.getInstance(resource);
            }
            if (img != null) {
                img.scaleToFit(100, 50);
                img.setAlignment(Element.ALIGN_RIGHT);
                logoCell.addElement(img);
            }
        } catch (Exception e) {}
        topTable.addCell(logoCell);
        document.add(topTable);

        Paragraph title = new Paragraph("DELIVERY CHALLAN", FONT_HEADER);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);

        // --- 2. MAIN INFO TABLE (Split into 2 Rows) ---
        // Width Ratio 1.5 : 1 (60% : 40%)
        PdfPTable mainTable = new PdfPTable(2); 
        mainTable.setWidthPercentage(100);
        mainTable.setWidths(new float[]{1.5f, 1}); 

        // ==========================================
        // ROW 1: COMPANY INFO (Left) + CHALLAN META (Right)
        // ==========================================
        
        // 1. Left Cell: Company Details
        PdfPCell companyCell = new PdfPCell();
        companyCell.setBorder(Rectangle.BOX);
        companyCell.setPadding(5);
        companyCell.addElement(new Paragraph(company.getCompanyName(), FONT_BOLD));
        companyCell.addElement(new Paragraph(company.getAddress(), FONT_NORMAL));
        companyCell.addElement(new Paragraph(company.getEmail(), new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLUE)));
        
        // GST Line
        Paragraph pGst = new Paragraph();
        pGst.add(new Chunk("GST: " + company.getGstin(), FONT_BOLD));
        companyCell.addElement(pGst);

        // Udyam in Next Line
        String udyam = company.getUdyamRegNo() != null ? company.getUdyamRegNo() : "-";
        Paragraph pUdyam = new Paragraph();
        pUdyam.add(new Chunk("UDYAM- " + udyam, FONT_BOLD));
        companyCell.addElement(pUdyam);
        
        mainTable.addCell(companyCell);

        // 2. Right Cell: Challan Meta Data
        PdfPCell metaCell1 = new PdfPCell();
        metaCell1.setBorder(Rectangle.BOX);
        metaCell1.setPadding(0);
        
        PdfPTable metaTable1 = new PdfPTable(2);
        metaTable1.setWidthPercentage(100);
        
        metaTable1.addCell(createCell("Challan No:", challan.getChallanNo()));
        metaTable1.addCell(createCell("Date:", challan.getChallanDate() != null ? sdf.format(challan.getChallanDate()) : "-"));
        metaTable1.addCell(createCell("State:", "MAHARASHTRA"));
        metaTable1.addCell(createCell("Code:", "27"));
        
        metaCell1.addElement(metaTable1);
        mainTable.addCell(metaCell1);

        // ==========================================
        // ROW 2: CLIENT INFO (Left) + ORDER META (Right)
        // ==========================================

        // 3. Left Cell: Client Details
        PdfPCell clientCell = new PdfPCell();
        clientCell.setBorder(Rectangle.BOX);
        clientCell.setPadding(5);
        clientCell.addElement(new Paragraph(challan.getClientName(), FONT_BOLD));
        clientCell.addElement(new Paragraph(challan.getClientAddress(), FONT_NORMAL));
        clientCell.addElement(new Paragraph("GST NO: " + challan.getClientGst(), FONT_NORMAL));
        mainTable.addCell(clientCell);

        // 4. Right Cell: Order & Contact Meta
        PdfPCell metaCell2 = new PdfPCell();
        metaCell2.setBorder(Rectangle.BOX);
        metaCell2.setPadding(0);

        PdfPTable metaTable2 = new PdfPTable(2);
        metaTable2.setWidthPercentage(100);

        metaTable2.addCell(createCell("Order:", challan.getOrderNo()));
        metaTable2.addCell(createCell("Date:", challan.getOrderDate() != null ? sdf.format(challan.getOrderDate()) : "-"));
        metaTable2.addCell(createCell("State.:", challan.getClientState()));
        metaTable2.addCell(createCell("Code:", challan.getClientStateCode()));

        // Contact Row (Label)
        PdfPCell contactLabel = new PdfPCell(new Phrase("Contact:", FONT_BOLD));
        contactLabel.setBorder(Rectangle.BOX); 
        contactLabel.setPadding(3);
        metaTable2.addCell(contactLabel);

        // Contact Row (Value with Mixed Font)
        PdfPCell contactVal = new PdfPCell();
        contactVal.setBorder(Rectangle.BOX);
        contactVal.setPadding(3);
        // 👇 FIX: Added Vertical Alignment
        contactVal.setVerticalAlignment(Element.ALIGN_MIDDLE);
        
        Paragraph pLoc = new Paragraph();
        pLoc.add(new Chunk("Location: ", FONT_BOLD));
        pLoc.add(new Chunk((challan.getContactPerson() != null ? challan.getContactPerson() : ""), FONT_NORMAL));
        
        contactVal.addElement(pLoc);
        metaTable2.addCell(contactVal);

        metaCell2.addElement(metaTable2);
        mainTable.addCell(metaCell2);

        document.add(mainTable);

        // --- 3. ITEMS TABLE ---
        // Width Ratio 1.5 : 1 (60% : 40%)
        // Left (0.8 + 5.2 = 6.0) : Right (1.5 + 1.5 + 1 = 4.0) -> Ratio 6:4 = 1.5:1
        float[] itemWidths = {0.8f, 5.2f, 1.5f, 1.5f, 1f}; 
        
        PdfPTable itemTable = new PdfPTable(itemWidths);
        itemTable.setWidthPercentage(100);
        itemTable.setSpacingBefore(0); 

        String[] headers = {"S. N.", "D E S C R I P T I O N", "SIZE", "HSN", "QTY"};
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, FONT_BOLD));
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setBackgroundColor(Color.WHITE); 
            c.setPadding(5);
            itemTable.addCell(c);
        }

        int sn = 1;
        if (challan.getItems() != null) {
            for (Challan.ChallanItem item : challan.getItems()) {
                itemTable.addCell(createCenterCell(String.valueOf(sn++)));
                itemTable.addCell(createLeftCell(item.getDescription()));
                itemTable.addCell(createCenterCell(item.getSize()));
                itemTable.addCell(createCenterCell(item.getHsn()));
                itemTable.addCell(createCenterCell(String.valueOf(item.getQty())));
            }
        }
        
        for (int i = 0; i < 10; i++) {
             itemTable.addCell(createEmptyCell());
             itemTable.addCell(createEmptyCell());
             itemTable.addCell(createEmptyCell());
             itemTable.addCell(createEmptyCell());
             itemTable.addCell(createEmptyCell());
        }

        document.add(itemTable);

        // --- 4. FOOTER ---
        PdfPTable footer = new PdfPTable(2);
        footer.setWidthPercentage(100);
        footer.setSpacingBefore(20);

        // Left Side
        PdfPCell receiver = new PdfPCell(new Phrase("Receiver's Signature with Rubber Stamp", new Font(Font.HELVETICA, 10, Font.UNDERLINE)));
        receiver.setBorder(Rectangle.NO_BORDER);
        receiver.setVerticalAlignment(Element.ALIGN_BOTTOM);
        receiver.setPaddingTop(30);
        footer.addCell(receiver);

        // Right Side
        PdfPCell prop = new PdfPCell();
        prop.setBorder(Rectangle.NO_BORDER);
        
        Paragraph pFor = new Paragraph("For JMD DÉCOR", new Font(Font.HELVETICA, 10, Font.BOLD, Color.RED));
        pFor.setAlignment(Element.ALIGN_RIGHT); 
        prop.addElement(pFor);
        
        prop.addElement(Chunk.NEWLINE);
        prop.addElement(Chunk.NEWLINE);
        prop.addElement(Chunk.NEWLINE);
        
        Paragraph pProp = new Paragraph("PROPRIETOR", FONT_BOLD);
        pProp.setAlignment(Element.ALIGN_RIGHT); 
        prop.addElement(pProp);
        
        footer.addCell(prop);

        document.add(footer);

        document.close();
        return out.toByteArray();
    }

    // Helpers
    private PdfPCell createCell(String label, String val) {
        PdfPCell c = new PdfPCell();
        c.setPadding(3);
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + " ", FONT_BOLD));
        p.add(new Chunk(val, FONT_NORMAL));
        c.addElement(p);
        return c;
    }
    private PdfPCell createCenterCell(String txt) {
        PdfPCell c = new PdfPCell(new Phrase(txt, FONT_NORMAL));
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPadding(4);
        return c;
    }
    private PdfPCell createLeftCell(String txt) {
        PdfPCell c = new PdfPCell(new Phrase(txt, FONT_NORMAL));
        c.setPadding(4);
        return c;
    }
    private PdfPCell createEmptyCell() {
        PdfPCell c = new PdfPCell(new Phrase(" ", FONT_NORMAL));
        c.setMinimumHeight(18);
        return c;
    }
}