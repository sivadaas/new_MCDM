package com.company.fucomhgra.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class MyDecisionRequest {

    @NotEmpty(message = "Priority order cannot be empty")
    private List<String> priorityOrder;

    @NotEmpty(message = "Comparative ratios cannot be empty")
    private Map<String, Double> comparativeRatios;

    @NotEmpty(message = "Decision matrix cannot be empty")
    private Map<String, Map<String, Double>> decisionMatrix;

    @NotEmpty(message = "Benefit criteria list cannot be empty")
    private List<String> benefitCriteria;
}