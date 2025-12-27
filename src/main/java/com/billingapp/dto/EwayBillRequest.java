package com.billingapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class EwayBillRequest {

    // --- TRANSACTION DETAILS ---
    @JsonProperty("supplyType")
    private String supplyType = "O"; // O = Outward

    @JsonProperty("subSupplyType")
    private String subSupplyType = "1"; // 1 = Supply

    @JsonProperty("docType")
    private String docType = "INV"; // Invoice

    @JsonProperty("docNo")
    private String docNo;

    @JsonProperty("docDate")
    private String docDate; // dd/MM/yyyy

    @JsonProperty("transType")
    private String transType = "1"; // 1 = Road

    // --- SELLER (FROM) ---
    @JsonProperty("fromGstin")
    private String fromGstin;

    @JsonProperty("fromTrdName")
    private String fromTrdName;

    @JsonProperty("fromAddr1")
    private String fromAddr1;

    @JsonProperty("fromPlace")
    private String fromPlace; // City

    @JsonProperty("fromPincode")
    private Integer fromPincode;

    @JsonProperty("fromStateCode")
    private Integer fromStateCode; // e.g., 27 for Maharashtra

    // --- BUYER (TO) ---
    @JsonProperty("toGstin")
    private String toGstin;

    @JsonProperty("toTrdName")
    private String toTrdName;

    @JsonProperty("toAddr1")
    private String toAddr1;

    @JsonProperty("toPlace")
    private String toPlace;

    @JsonProperty("toPincode")
    private Integer toPincode;

    @JsonProperty("toStateCode")
    private Integer toStateCode;

    // --- VALUES ---
    @JsonProperty("totalValue")
    private Double totalValue; // Taxable Value

    @JsonProperty("cgstValue")
    private Double cgstValue;

    @JsonProperty("sgstValue")
    private Double sgstValue;

    @JsonProperty("igstValue")
    private Double igstValue;

    @JsonProperty("totInvValue")
    private Double totInvValue; // Grand Total

    @JsonProperty("itemList")
    private List<EwayItem> itemList;

    @Data
    public static class EwayItem {
        @JsonProperty("productName")
        private String productName;

        @JsonProperty("hsnCode")
        private Integer hsnCode; // JSON expects Integer usually, but string if leading zeros

        @JsonProperty("quantity")
        private Double quantity;

        @JsonProperty("qtyUnit")
        private String qtyUnit; // e.g., PCS, KGS

        @JsonProperty("taxableAmount")
        private Double taxableAmount;

        @JsonProperty("sgstRate")
        private Double sgstRate;

        @JsonProperty("cgstRate")
        private Double cgstRate;

        @JsonProperty("igstRate")
        private Double igstRate;
    }
}