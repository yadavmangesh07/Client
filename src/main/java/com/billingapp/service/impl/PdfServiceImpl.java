package com.billingapp.service.impl;

import com.billingapp.entity.Client;
import com.billingapp.entity.Company;
import com.billingapp.entity.Invoice;
import com.billingapp.repository.ClientRepository;
import com.billingapp.repository.CompanyRepository;
import com.billingapp.repository.InvoiceRepository;
import com.billingapp.service.PdfService;
import com.billingapp.util.NumberToWords; 
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import com.lowagie.text.Image;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class PdfServiceImpl implements PdfService {

    private final InvoiceRepository invoiceRepository;
    private final CompanyRepository companyRepository;
    private final ClientRepository clientRepository;

    // Fonts
    private static final Font FONT_BOLD_BIG = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);
    private static final Font FONT_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final Font FONT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
    private static final Font FONT_SMALL = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
    private static final Font FONT_RED_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.RED);

    public PdfServiceImpl(InvoiceRepository invoiceRepository, 
                          CompanyRepository companyRepository,
                          ClientRepository clientRepository) {
        this.invoiceRepository = invoiceRepository;
        this.companyRepository = companyRepository;
        this.clientRepository = clientRepository;
    }

    @Override
    public byte[] generateInvoicePdf(String invoiceId) throws Exception {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        Client client = clientRepository.findById(invoice.getClientId())
                .orElse(new Client()); 
        
        String clientName = client.getName() != null ? client.getName() : "Unknown Client";
        String clientGst = client.getGstin() != null ? client.getGstin() : "-";
        
        String clientState = client.getState() != null ? client.getState() : "-";
        String clientStateCode = client.getStateCode() != null ? client.getStateCode() : "27";

        // Fetch Company Data
        Company company = companyRepository.findById("MY_COMPANY").orElse(new Company());
        if(company.getCompanyName() == null) {
            company.setCompanyName("JMD DECOR");
            company.setAddress("210 ASHIRWAD INDUSTRIAL ESTATE BLDG. NO:-5 RAM MANDIR ROAD, GOREGAON WEST MUMBAI 400104");
            company.setEmail("jmdecor.2010@gmail.com");
            company.setGstin("27AAOPY8409R1ZD");
            company.setPhone("9819707090");
            company.setBankName("Kotak Mahindra Bank");
            company.setAccountNumber("9111365107");
            company.setIfscCode("KKBK0000643");
            company.setBranch("Jawahar Nagar Mumbai");
        }

        Document document = new Document(PageSize.A4, 20, 20, 15, 15);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();

        // --- 1. HEADER (Logo & Company) ---
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{2, 1}); 

        PdfPCell titleCell = new PdfPCell(new Phrase("", FONT_BOLD_BIG));
        titleCell.setBorder(Rectangle.NO_BORDER);
        headerTable.addCell(titleCell);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
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
                img.scaleToFit(100, 50); 
                img.setAlignment(Element.ALIGN_RIGHT);
                logoCell.addElement(img);
            } else { throw new RuntimeException("No img"); }
        } catch (Exception e) {
            Paragraph p = new Paragraph("JMD\nDECOR", FONT_BOLD_BIG);
            p.setAlignment(Element.ALIGN_RIGHT);
            logoCell.addElement(p);
        }
        headerTable.addCell(logoCell);
        document.add(headerTable);

        // --- TITLE ROW (Aligned) ---
        PdfPTable titleRowTable = new PdfPTable(3);
        titleRowTable.setWidthPercentage(100);
        titleRowTable.setWidths(new float[]{1, 1, 1}); 

        PdfPCell emptyLeft = new PdfPCell(new Phrase("", FONT_SMALL));
        emptyLeft.setBorder(Rectangle.NO_BORDER);
        titleRowTable.addCell(emptyLeft);

        PdfPCell centerTitle = new PdfPCell(new Phrase("TAX INVOICE", FONT_BOLD));
        centerTitle.setBorder(Rectangle.NO_BORDER);
        centerTitle.setHorizontalAlignment(Element.ALIGN_CENTER);
        centerTitle.setVerticalAlignment(Element.ALIGN_MIDDLE);
        titleRowTable.addCell(centerTitle);

        PdfPCell rightOriginal = new PdfPCell(new Phrase("Original For Recipient", FONT_SMALL));
        rightOriginal.setBorder(Rectangle.NO_BORDER);
        rightOriginal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightOriginal.setVerticalAlignment(Element.ALIGN_MIDDLE);
        titleRowTable.addCell(rightOriginal);

        document.add(titleRowTable);
        document.add(new Paragraph("\n")); 


        // --- 2. MAIN DETAILS GRID (Company & Invoice Info) ---
        PdfPTable mainGrid = new PdfPTable(2);
        mainGrid.setWidthPercentage(100);
        mainGrid.setWidths(new float[]{1.2f, 1});

        PdfPCell companyCell = new PdfPCell();
        companyCell.setBorder(Rectangle.BOX);
        companyCell.setPadding(5);
        companyCell.addElement(new Paragraph(company.getCompanyName(), FONT_BOLD));
        companyCell.addElement(new Paragraph(company.getAddress(), FONT_NORMAL));
        companyCell.addElement(new Paragraph("Phone: " + company.getPhone(), FONT_NORMAL)); 
        companyCell.addElement(new Paragraph("Email: " + company.getEmail(), FONT_NORMAL));
        companyCell.addElement(new Paragraph("GST: " + company.getGstin(), FONT_BOLD));
        mainGrid.addCell(companyCell);

        PdfPTable rightGrid = new PdfPTable(2);
        rightGrid.setWidthPercentage(100);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy").withZone(ZoneId.systemDefault());
        String dateStr = invoice.getIssuedAt() != null ? dtf.format(invoice.getIssuedAt()) : "-";
        
        rightGrid.addCell(createLabelValueCell("Bill No :", invoice.getInvoiceNo()));
        rightGrid.addCell(createLabelValueCell("Date :", dateStr));
        rightGrid.addCell(createLabelValueCell("Challan No.", invoice.getChallanNo()));
        rightGrid.addCell(createLabelValueCell("Date :", invoice.getChallanDate() != null ? dtf.format(invoice.getChallanDate()) : "-"));
        rightGrid.addCell(createLabelValueCell("State :", "Maharashtra"));
        rightGrid.addCell(createLabelValueCell("Code :", "27"));
        rightGrid.addCell(createLabelValueCell("E-way Bill :", invoice.getEwayBillNo()));
        rightGrid.addCell(createLabelValueCell("Mode of Transport:", invoice.getTransportMode()));
        rightGrid.addCell(createLabelValueCell("P.O.:", invoice.getPoNumber()));
        rightGrid.addCell(createLabelValueCell("Date :", invoice.getPoDate() != null ? dtf.format(invoice.getPoDate()) : "-"));
        
        PdfPCell rightCellContainer = new PdfPCell(rightGrid);
        rightCellContainer.setPadding(0);
        mainGrid.addCell(rightCellContainer);
        document.add(mainGrid);

        // --- 3. CLIENT DETAILS ---
        PdfPTable addressGrid = new PdfPTable(2);
        addressGrid.setWidthPercentage(100);
        addressGrid.setSpacingBefore(0);
        
        // Exact match of mainGrid {1.2f, 1} for perfect vertical alignment
        addressGrid.setWidths(new float[]{1.2f, 1}); 

        String shippingAddr = invoice.getShippingAddress() != null && !invoice.getShippingAddress().isEmpty() 
            ? invoice.getShippingAddress() 
            : invoice.getBillingAddress();

        addressGrid.addCell(createClientCell("BILLED TO", clientName, invoice.getBillingAddress(), clientState, clientStateCode, clientGst));
        addressGrid.addCell(createClientCell("SHIPPED TO", clientName, shippingAddr, clientState, clientStateCode, clientGst));

        document.add(addressGrid);

        // --- 4. ITEMS TABLE ---
        float[] itemColWidths = {0.8f, 4.7f, 1.5f, 1.3f, 1, 1, 2, 2.5f, 2, 2.5f};
        
        PdfPTable itemTable = new PdfPTable(itemColWidths);
        itemTable.setWidthPercentage(100);
        itemTable.setHeaderRows(1);
        itemTable.setSpacingBefore(0);
        
        String[] headers = {"Sr No", "Description", "HSN", "UOM", "Unit", "Qty", "Rate", "Amount", "GST%", "Total"};
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, FONT_BOLD));
            c.setBackgroundColor(Color.YELLOW); 
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setVerticalAlignment(Element.ALIGN_MIDDLE);
            c.setPadding(3);
            itemTable.addCell(c);
        }
        int sr = 1;
        double subTotal = 0;
        double totalTax = 0;
        if (invoice.getItems() != null) {
            for (Invoice.InvoiceItem item : invoice.getItems()) {
                double amount = item.getQty() * item.getRate();
                double taxVal = amount * (item.getTaxRate() / 100);
                double totalRow = amount + taxVal;
                itemTable.addCell(createCenterCell(String.valueOf(sr++)));
                itemTable.addCell(createLeftCell(item.getDescription()));
                itemTable.addCell(createCenterCell(item.getHsnCode()));
                itemTable.addCell(createCenterCell(item.getUom()));
                itemTable.addCell(createCenterCell("1"));
                itemTable.addCell(createCenterCell(String.valueOf(item.getQty())));
                itemTable.addCell(createRightCell(String.format("%.2f", item.getRate())));
                itemTable.addCell(createRightCell(String.format("%.2f", amount)));
                itemTable.addCell(createRightCell(String.format("%.0f%%", item.getTaxRate())));
                itemTable.addCell(createRightCell(String.format("%.2f", totalRow)));
                subTotal += amount;
                totalTax += taxVal;
            }
        }
        for(int i=0; i<3; i++) {
            for(int j=0; j<10; j++) itemTable.addCell(createEmptyCell());
        }
        PdfPCell blankTotal = new PdfPCell(new Phrase("", FONT_BOLD));
        blankTotal.setColspan(7);
        blankTotal.setBorder(Rectangle.NO_BORDER); 
        itemTable.addCell(blankTotal);
        itemTable.addCell(createRightCell(String.format("%.2f", subTotal)));
        itemTable.addCell(createRightCell(String.format("%.2f", totalTax)));
        itemTable.addCell(createRightCell(String.format("%.2f", subTotal + totalTax)));
        document.add(itemTable);

        // --- 5. FOOTER ---
        PdfPTable footerTable = new PdfPTable(2);
        footerTable.setWidthPercentage(100);
        footerTable.setWidths(new float[]{3, 1}); 

        PdfPCell cellRupees = new PdfPCell(new Phrase("Rupees: " + NumberToWords.convert(invoice.getTotal()), FONT_BOLD));
        cellRupees.setBorder(Rectangle.BOX);
        cellRupees.setPadding(5);
        footerTable.addCell(cellRupees);

        PdfPCell cellTotal = new PdfPCell(new Phrase("Total: " + String.format("%.0f", invoice.getTotal()), FONT_BOLD_BIG));
        cellTotal.setBorder(Rectangle.BOX);
        cellTotal.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellTotal.setVerticalAlignment(Element.ALIGN_MIDDLE);
        footerTable.addCell(cellTotal);

        PdfPCell cellBank = new PdfPCell();
        cellBank.setBorder(Rectangle.BOX);
        cellBank.setPadding(5);
        cellBank.addElement(new Paragraph("Bank Details:", FONT_BOLD));
        cellBank.addElement(new Paragraph("Bank: " + company.getBankName(), FONT_NORMAL));
        cellBank.addElement(new Paragraph("Account No: " + company.getAccountNumber(), FONT_NORMAL));
        cellBank.addElement(new Paragraph("IFSC Code: " + company.getIfscCode(), FONT_NORMAL));
        cellBank.addElement(new Paragraph("Branch: " + company.getBranch(), FONT_NORMAL));
        footerTable.addCell(cellBank);

        PdfPCell cellSignatory = new PdfPCell();
        cellSignatory.setBorder(Rectangle.BOX);
        cellSignatory.setRowspan(2); 
        cellSignatory.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellSignatory.setVerticalAlignment(Element.ALIGN_MIDDLE); // Vertically Centered
        cellSignatory.setPadding(5);
        
        Paragraph pSig = new Paragraph();
        pSig.add(new Paragraph("For JMD DÉCOR", FONT_BOLD));
        pSig.add(Chunk.NEWLINE); 
        pSig.add(Chunk.NEWLINE); 
        pSig.add(Chunk.NEWLINE);
        pSig.add(new Paragraph("PROPRIETOR", FONT_BOLD));
        pSig.setAlignment(Element.ALIGN_CENTER);
        cellSignatory.addElement(pSig);
        footerTable.addCell(cellSignatory);

        PdfPCell cellDecl = new PdfPCell();
        cellDecl.setBorder(Rectangle.BOX);
        cellDecl.setPadding(5);
        
        // Formal Declaration
        Paragraph declP = new Paragraph();
        declP.add(new Chunk("Declaration: ", FONT_BOLD)); 
        declP.add(new Chunk("Certified that the particulars given above are true and correct and the amount indicated represents the price actually charged and that there is no flow of additional consideration directly or indirectly from the buyer.", FONT_SMALL));
        cellDecl.addElement(declP);
        
        footerTable.addCell(cellDecl);

        document.add(footerTable);

        document.close();
        return out.toByteArray();
    }

    // --- HELPER: CREATE CLIENT CELL ---
    private PdfPCell createClientCell(String title, String name, String address, String state, String code, String gst) {
        PdfPCell parent = new PdfPCell();
        parent.setBorder(Rectangle.BOX);
        parent.setPadding(0);

        PdfPTable nested = new PdfPTable(2);
        nested.setWidthPercentage(100);
        
        try { nested.setWidths(new float[]{1.5f, 1}); } catch (Exception e) {}

        // 👇 UPDATED: Row 1 - TITLE ONLY (Separated Row)
        PdfPCell titleCell = new PdfPCell(new Phrase(title, FONT_RED_BOLD));
        titleCell.setColspan(2);
        titleCell.setBorder(Rectangle.BOTTOM); // 👈 Line Separating Title from Address
        titleCell.setPadding(5);
        nested.addCell(titleCell);

        // 👇 UPDATED: Row 2 - NAME & ADDRESS (No border between this and title cell due to Rectangle.BOTTOM above)
        PdfPCell addressCell = new PdfPCell();
        addressCell.setColspan(2);
        addressCell.setBorder(Rectangle.NO_BORDER);
        addressCell.setPadding(5);
        addressCell.addElement(new Paragraph(name, FONT_BOLD)); 
        addressCell.addElement(new Paragraph(address, FONT_NORMAL));
        addressCell.addElement(Chunk.NEWLINE); 
        nested.addCell(addressCell);

        // Row 3: State | Code
        PdfPCell stateCell = new PdfPCell(new Phrase("State: " + state, FONT_BOLD));
        stateCell.setBorder(Rectangle.TOP | Rectangle.RIGHT); 
        stateCell.setPadding(5);
        nested.addCell(stateCell);

        PdfPCell codeCell = new PdfPCell(new Phrase("State Code: " + code, FONT_BOLD));
        codeCell.setBorder(Rectangle.TOP); 
        codeCell.setPadding(5);
        codeCell.setHorizontalAlignment(Element.ALIGN_LEFT); // Left Aligned
        nested.addCell(codeCell);

        // Row 4: GST NO
        PdfPCell gstCell = new PdfPCell(new Phrase("GST NO: " + gst, FONT_BOLD));
        gstCell.setColspan(2);
        gstCell.setBorder(Rectangle.TOP); 
        gstCell.setPadding(5);
        nested.addCell(gstCell);

        parent.addElement(nested);
        return parent;
    }

    // Existing Helpers
    private PdfPCell createLabelValueCell(String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(3);
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + " ", FONT_BOLD));
        p.add(new Chunk(value != null ? value : "-", FONT_NORMAL));
        cell.addElement(p);
        return cell;
    }
    private PdfPCell createCenterCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, FONT_NORMAL));
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPadding(3);
        return c;
    }
    private PdfPCell createLeftCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, FONT_NORMAL));
        c.setHorizontalAlignment(Element.ALIGN_LEFT);
        c.setPadding(3);
        return c;
    }
    private PdfPCell createRightCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, FONT_NORMAL));
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setPadding(3);
        return c;
    }
    private PdfPCell createEmptyCell() {
        PdfPCell c = new PdfPCell(new Phrase(" ", FONT_NORMAL));
        c.setMinimumHeight(12); 
        return c;
    }
}