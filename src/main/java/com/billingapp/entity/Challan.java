package com.billingapp.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.List;

@Document(collection = "challans")
public class Challan {
    @Id
    private String id;
    
    private String challanNo;     // e.g., JMD/2025-26/01
    private Date challanDate;

    private String orderNo;       // e.g., -NYKAA-17145
    private Date orderDate;

    // Client/Consignee Info
    private String clientName;
    private String clientAddress;
    private String clientGst;
    private String clientState;
    private String clientStateCode;
    private String contactPerson; // e.g., Location: NYKAA LUXE...

    private List<ChallanItem> items;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getChallanNo() { return challanNo; }
    public void setChallanNo(String challanNo) { this.challanNo = challanNo; }
    public Date getChallanDate() { return challanDate; }
    public void setChallanDate(Date challanDate) { this.challanDate = challanDate; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Date getOrderDate() { return orderDate; }
    public void setOrderDate(Date orderDate) { this.orderDate = orderDate; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public String getClientAddress() { return clientAddress; }
    public void setClientAddress(String clientAddress) { this.clientAddress = clientAddress; }
    public String getClientGst() { return clientGst; }
    public void setClientGst(String clientGst) { this.clientGst = clientGst; }
    public String getClientState() { return clientState; }
    public void setClientState(String clientState) { this.clientState = clientState; }
    public String getClientStateCode() { return clientStateCode; }
    public void setClientStateCode(String clientStateCode) { this.clientStateCode = clientStateCode; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    public List<ChallanItem> getItems() { return items; }
    public void setItems(List<ChallanItem> items) { this.items = items; }

    // Nested Item Class
    public static class ChallanItem {
        private String description;
        private String size;
        private String hsn;
        private int qty;

        // Getters and Setters
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
        public String getHsn() { return hsn; }
        public void setHsn(String hsn) { this.hsn = hsn; }
        public int getQty() { return qty; }
        public void setQty(int qty) { this.qty = qty; }
    }
}