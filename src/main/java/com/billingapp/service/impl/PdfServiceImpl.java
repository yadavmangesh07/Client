package com.billingapp.service.impl;

import com.billingapp.dto.InvoiceDTO;
import com.billingapp.entity.Invoice;
import com.billingapp.mapper.InvoiceMapper;
import com.billingapp.repository.InvoiceRepository;
import com.billingapp.service.PdfService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class PdfServiceImpl implements PdfService {

    // using field injection to avoid any constructor/lookup complexities
    @org.springframework.beans.factory.annotation.Autowired
    private InvoiceRepository invoiceRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private InvoiceMapper mapper;

    @Override
    public ByteArrayOutputStream generateInvoicePdf(String invoiceId) throws Exception {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        InvoiceDTO dto = mapper.toDto(invoice);

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDPageContentStream cs = new PDPageContentStream(doc, page);

            float margin = 50;
            float y = page.getMediaBox().getHeight() - margin;

            // Header: Title and invoice meta
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
            cs.newLineAtOffset(margin, y);
            cs.showText("INVOICE");
            cs.endText();

            y -= 30;

            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 11);
            cs.newLineAtOffset(margin, y);
            cs.showText("Invoice No: " + safe(dto.getInvoiceNo()));
            cs.endText();

            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 11);
            cs.newLineAtOffset(margin + 300, y);
            String issued = dto.getIssuedAt() != null ? DateTimeFormatter.ISO_LOCAL_DATE
                    .withZone(ZoneId.systemDefault()).format(dto.getIssuedAt()) : "";
            cs.showText("Issued: " + issued);
            cs.endText();

            y -= 20;

            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 11);
            cs.newLineAtOffset(margin, y);
            cs.showText("Client ID: " + safe(dto.getClientId()));
            cs.endText();

            y -= 25;

            // Draw items table header
            float tableTopY = y;
            float tableX = margin;
            float[] colWidths = { 260, 60, 80, 80 }; // description, qty, rate, amount
            float rowHeight = 18;

            // header row
            cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
            cs.beginText();
            cs.newLineAtOffset(tableX + 2, tableTopY);
            cs.showText("Description");
            cs.endText();

            cs.beginText();
            cs.newLineAtOffset(tableX + colWidths[0] + 2, tableTopY);
            cs.showText("Qty");
            cs.endText();

            cs.beginText();
            cs.newLineAtOffset(tableX + colWidths[0] + colWidths[1] + 2, tableTopY);
            cs.showText("Rate");
            cs.endText();

            cs.beginText();
            cs.newLineAtOffset(tableX + colWidths[0] + colWidths[1] + colWidths[2] + 2, tableTopY);
            cs.showText("Amount");
            cs.endText();

            y = tableTopY - rowHeight;

            // draw items
            cs.setFont(PDType1Font.HELVETICA, 11);
            if (dto.getItems() != null) {
                for (var item : dto.getItems()) {
                    if (y < margin + 80) { // new page
                        cs.close();
                        PDPage newPage = new PDPage(PDRectangle.A4);
                        doc.addPage(newPage);
                        cs = new PDPageContentStream(doc, newPage);
                        y = newPage.getMediaBox().getHeight() - margin - 30;
                    }

                    cs.beginText();
                    cs.newLineAtOffset(tableX + 2, y);
                    cs.showText(truncate(safe(item.getDescription()), 40));
                    cs.endText();

                    cs.beginText();
                    cs.newLineAtOffset(tableX + colWidths[0] + 2, y);
                    cs.showText(String.valueOf(item.getQty()));
                    cs.endText();

                    cs.beginText();
                    cs.newLineAtOffset(tableX + colWidths[0] + colWidths[1] + 2, y);
                    cs.showText(String.format("%.2f", item.getRate()));
                    cs.endText();

                    cs.beginText();
                    cs.newLineAtOffset(tableX + colWidths[0] + colWidths[1] + colWidths[2] + 2, y);
                    cs.showText(String.format("%.2f", item.getAmount()));
                    cs.endText();

                    y -= rowHeight;
                }
            }

            // Totals block
            y -= 10;
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
            cs.newLineAtOffset(tableX + colWidths[0] + colWidths[1], y);
            cs.showText("Subtotal: " + String.format("%.2f", dto.getSubtotal()));
            cs.endText();

            y -= 16;
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
            cs.newLineAtOffset(tableX + colWidths[0] + colWidths[1], y);
            cs.showText("Tax: " + String.format("%.2f", dto.getTax()));
            cs.endText();

            y -= 16;
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
            cs.newLineAtOffset(tableX + colWidths[0] + colWidths[1], y);
            cs.showText("Total: " + String.format("%.2f", dto.getTotal()));
            cs.endText();

            cs.close();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos;
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String truncate(String s, int len) {
        if (s == null) return "";
        return s.length() <= len ? s : s.substring(0, len - 3) + "...";
    }
}
