package com.company.fucomhgra.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankResultDTO {
    private String technology;
    private double grgScore;
    private int rank;

    private Map<String,Double> fucomWeights;

    private Map<String, Double> grcValues;

    private Map<String, Double> normalizedValues;
}
