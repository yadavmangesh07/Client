package com.billingapp.service;

import com.billingapp.entity.Estimate;
import java.util.List;

public interface EstimateService {
    
    List<Estimate> getAllEstimates();
    Estimate getEstimateById(String id);

    Estimate createEstimate(Estimate estimate);

    Estimate updateEstimate(String id, Estimate estimate);

    void deleteEstimate(String id);

    byte[] generateEstimatePdf(String id) throws Exception;
}