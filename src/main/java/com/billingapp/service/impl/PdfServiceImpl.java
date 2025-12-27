package com.billingapp.service.impl;

import com.billingapp.entity.Company;
import com.billingapp.entity.Invoice;
import com.billingapp.repository.CompanyRepository;
import com.billingapp.repository.InvoiceRepository;
import com.billingapp.service.PdfService;
import com.billingapp.util.NumberToWords; // 👈 Import the Utility
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class PdfServiceImpl implements PdfService {

    private final InvoiceRepository invoiceRepository;
    private final CompanyRepository companyRepository;

    public PdfServiceImpl(InvoiceRepository invoiceRepository, CompanyRepository companyRepository) {
        this.invoiceRepository = invoiceRepository;
        this.companyRepository = companyRepository;
    }

    @Override
    public byte[] generateInvoicePdf(String invoiceId) throws Exception {
        // 1. Fetch Data
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));
        
        // Fetch Company Profile (or use defaults)
        Company company = companyRepository.findById("MY_COMPANY").orElse(new Company());
        if(company.getCompanyName() == null) company.setCompanyName("JMD Decor");

        // 2. Setup Document
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();
        addImage(document, company.getLogoUrl());

        // --- HEADER SECTION ---
        // Company Name (Big Bold)
        Font companyFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
        Paragraph companyName = new Paragraph(company.getCompanyName(), companyFont);
        companyName.setAlignment(Element.ALIGN_LEFT);
        document.add(companyName);

        // Company Address & Details
        Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
        document.add(new Paragraph(company.getAddress(), smallFont));
        document.add(new Paragraph("Phone: " + (company.getPhone() != null ? company.getPhone() : "-") + " | Email: " + (company.getEmail() != null ? company.getEmail() : "-"), smallFont));
        if(company.getGstin() != null) {
            document.add(new Paragraph("GSTIN: " + company.getGstin(), smallFont));
        }
        
        document.add(Chunk.NEWLINE);
        
        // Title: TAX INVOICE
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);
        Paragraph title = new Paragraph("TAX INVOICE", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(Chunk.NEWLINE);

        // --- INVOICE & CLIENT DETAILS (2 Columns) ---
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{1, 1});

        // Left: Client Details
        PdfPCell clientCell = new PdfPCell();
        clientCell.setBorder(Rectangle.BOX);
        clientCell.setPadding(10);
        clientCell.addElement(new Paragraph("BILLED TO:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        clientCell.addElement(new Paragraph(invoice.getBillingAddress() != null ? invoice.getBillingAddress() : "Client Address", smallFont));
        infoTable.addCell(clientCell);

        // Right: Invoice Specifics
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy").withZone(ZoneId.systemDefault());
        PdfPCell invCell = new PdfPCell();
        invCell.setBorder(Rectangle.BOX);
        invCell.setPadding(10);
        invCell.addElement(new Paragraph("Invoice No: " + invoice.getInvoiceNo(), smallFont));
        invCell.addElement(new Paragraph("Date: " + (invoice.getIssuedAt() != null ? formatter.format(invoice.getIssuedAt()) : "-"), smallFont));
        if(invoice.getPoNumber() != null) invCell.addElement(new Paragraph("PO No: " + invoice.getPoNumber(), smallFont));
        if(invoice.getTransportMode() != null) invCell.addElement(new Paragraph("Transport: " + invoice.getTransportMode(), smallFont));
        if(invoice.getEwayBillNo() != null) invCell.addElement(new Paragraph("E-Way Bill: " + invoice.getEwayBillNo(), smallFont));
        infoTable.addCell(invCell);

        document.add(infoTable);
        document.add(Chunk.NEWLINE);

        // --- ITEMS TABLE ---
        PdfPTable table = new PdfPTable(7); // Desc, HSN, Qty, UOM, Rate, Tax, Amount
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4, 1.5f, 1, 1, 1.5f, 1, 2});

        // Headers
        String[] headers = {"Description", "HSN", "Qty", "Unit", "Rate", "Tax", "Amount"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            cell.setPadding(5);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        // Rows
        double totalTaxVal = 0;
        if (invoice.getItems() != null) {
            for (Invoice.InvoiceItem item : invoice.getItems()) {
                table.addCell(createCell(item.getDescription(), Element.ALIGN_LEFT));
                table.addCell(createCell(item.getHsnCode(), Element.ALIGN_CENTER));
                table.addCell(createCell(String.valueOf(item.getQty()), Element.ALIGN_CENTER));
                table.addCell(createCell(item.getUom(), Element.ALIGN_CENTER));
                
                // 👇 Added "Rs."
                table.addCell(createCell("Rs. " + String.format("%.2f", item.getRate()), Element.ALIGN_RIGHT));
                
                table.addCell(createCell(String.format("%.0f%%", item.getTaxRate()), Element.ALIGN_CENTER));
                
                double lineAmount = item.getQty() * item.getRate();
                // 👇 Added "Rs."
                table.addCell(createCell("Rs. " + String.format("%.2f", lineAmount), Element.ALIGN_RIGHT));
                
                totalTaxVal += (lineAmount * item.getTaxRate() / 100);
            }
        }
        document.add(table);

        // --- TOTALS SECTION ---
        PdfPTable totalTable = new PdfPTable(2);
        totalTable.setWidthPercentage(40);
        totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        
        addTotalRow(totalTable, "Subtotal:", invoice.getSubtotal());
        addTotalRow(totalTable, "Tax Amount:", totalTaxVal);
        addTotalRow(totalTable, "Grand Total:", invoice.getTotal());
        
        document.add(totalTable);
        document.add(Chunk.NEWLINE);

        // --- AMOUNT IN WORDS & BANK DETAILS ---
        PdfPTable footerTable = new PdfPTable(2);
        footerTable.setWidthPercentage(100);
        
        // Bank Details Cell
        PdfPCell bankCell = new PdfPCell();
        bankCell.setBorder(Rectangle.BOX);
        bankCell.setPadding(10);
        bankCell.addElement(new Paragraph("Bank Details:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        bankCell.addElement(new Paragraph("Bank Name: " + (company.getBankName() != null ? company.getBankName() : "-"), smallFont));
        bankCell.addElement(new Paragraph("A/c No: " + (company.getAccountNumber() != null ? company.getAccountNumber() : "-"), smallFont));
        bankCell.addElement(new Paragraph("IFSC: " + (company.getIfscCode() != null ? company.getIfscCode() : "-"), smallFont));
        bankCell.addElement(new Paragraph("Branch: " + (company.getBranch() != null ? company.getBranch() : "-"), smallFont));
        footerTable.addCell(bankCell);
        
        // Amount in Words Cell
        PdfPCell wordsCell = new PdfPCell();
        wordsCell.setBorder(Rectangle.BOX);
        wordsCell.setPadding(10);
        wordsCell.addElement(new Paragraph("Amount in Words:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        
        // 👇 USE NUMBER TO WORDS UTILITY
        String words = NumberToWords.convert(invoice.getTotal());
        wordsCell.addElement(new Paragraph(words, smallFont));

        wordsCell.addElement(new Paragraph("\n\nFor " + company.getCompanyName(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        wordsCell.addElement(new Paragraph("\n\nAuthorized Signatory", smallFont));
        footerTable.addCell(wordsCell);

        document.add(footerTable);

        document.close();
        
        // ✅ CORRECTED RETURN
        return out.toByteArray();
    }

    // Helper for Cells
    private PdfPCell createCell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", FontFactory.getFont(FontFactory.HELVETICA, 10)));
        cell.setPadding(5);
        cell.setHorizontalAlignment(alignment);
        return cell;
    }

    // Helper for Totals
    private void addTotalRow(PdfPTable table, String label, double value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(labelCell);

        // 👇 Added "Rs."
        PdfPCell valueCell = new PdfPCell(new Phrase("Rs. " + String.format("%.2f", value), FontFactory.getFont(FontFactory.HELVETICA, 10)));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }
    
    private void addImage(Document document, String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                Image img = Image.getInstance(imageUrl);
                img.scaleToFit(100, 50); 
                img.setAlignment(Element.ALIGN_LEFT);
                document.add(img);
            } catch (Exception e) {
                System.err.println("Could not load image: " + imageUrl);
            }
        }
    }
}