package com.billingapp.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;

@Document(collection = "work_certificates")
public class WorkCompletionCertificate {

    @Id
    private String id;
    private String storeName;
    private String refNo;
    private String projectLocation;
    private String certificateDate;
    private String poNo;
    private String poDate;
    private String gstin;
    
    // Using a simple inner static class for items since they are nested
    private List<WCCItem> items;
    
    private String companyName;
    private String clientName;
    private Instant createdAt;

    // Constructors
    public WorkCompletionCertificate() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public String getRefNo() { return refNo; }
    public void setRefNo(String refNo) { this.refNo = refNo; }

    public String getProjectLocation() { return projectLocation; }
    public void setProjectLocation(String projectLocation) { this.projectLocation = projectLocation; }

    public String getCertificateDate() { return certificateDate; }
    public void setCertificateDate(String certificateDate) { this.certificateDate = certificateDate; }

    public String getPoNo() { return poNo; }
    public void setPoNo(String poNo) { this.poNo = poNo; }

    public String getPoDate() { return poDate; }
    public void setPoDate(String poDate) { this.poDate = poDate; }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }

    public List<WCCItem> getItems() { return items; }
    public void setItems(List<WCCItem> items) { this.items = items; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    // Inner Class for Items
    public static class WCCItem {
        private String srNo;
        private String activity;
        private String qty;

        public WCCItem() {}

        public String getSrNo() { return srNo; }
        public void setSrNo(String srNo) { this.srNo = srNo; }

        public String getActivity() { return activity; }
        public void setActivity(String activity) { this.activity = activity; }

        public String getQty() { return qty; }
        public void setQty(String qty) { this.qty = qty; }
    }
}