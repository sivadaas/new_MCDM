package com.company.fucomhgra.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class HgraService {

    private static final int DECIMAL_PLACES = 2;
    private static final double ZETA = 0.5;

    private double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(DECIMAL_PLACES, RoundingMode.HALF_UP)
                .doubleValue();//converts to double value
    }

    // STEP 1: HGRA Normalisation
    public Map<String, Map<String, Double>> normalise(
            Map<String, Map<String, Double>> decisionMatrix,
            List<String> benefitCriteria
    ) {
        List<String> technologies = new ArrayList<>(decisionMatrix.keySet());
        List<String> criteria = new ArrayList<>(decisionMatrix.get(technologies.get(0)).keySet());

        Map<String, Map<String, Double>> normalized = new LinkedHashMap<>();

        for (String criterion : criteria) {

            // FIX 1: Removed System.out.println that referenced min/max before they were declared
            double min = technologies.stream()
                    .mapToDouble(t -> decisionMatrix.get(t).get(criterion))
                    .min().getAsDouble();

            double max = technologies.stream()
                    .mapToDouble(t -> decisionMatrix.get(t).get(criterion))
                    .max().getAsDouble();

            System.out.println("Criterion: " + criterion + " | min=" + min + " | max=" + max);

            for (String tech : technologies) {
                double value = decisionMatrix.get(tech).get(criterion);
                double normalValue;

                if (max - min == 0) {
                    normalValue = 1.0;
                } else if (benefitCriteria.contains(criterion)) {
                    normalValue = (value - min) / (max - min);  // higher is better
                } else {
                    normalValue = (max - value) / (max - min);  // lower is better
                }

                normalized
                        .computeIfAbsent(tech, k -> new LinkedHashMap<>())
                        .put(criterion, round(normalValue));
            }
        }
        return normalized;
    }

    // STEP 2: Grey Relational Coefficient (GRC)
    public Map<String, Map<String, Double>> computeGRC(
            Map<String, Map<String, Double>> normalizedMatrix
    ) {
        Map<String, Map<String, Double>> grc = new LinkedHashMap<>();

        for (var techEntry : normalizedMatrix.entrySet()) {
            Map<String, Double> grcRow = new LinkedHashMap<>();

            for (var criterionEntry : techEntry.getValue().entrySet()) {
                double normalValue = criterionEntry.getValue();
                double delta = Math.abs(1.0 - normalValue);

                // FIX 2: Wrong formula — was (ZETA/delta+ZETA), correct is ZETA/(delta+ZETA)
                double grcValue = ZETA / (delta + ZETA);

                grcRow.put(criterionEntry.getKey(), round(grcValue));
            }
            grc.put(techEntry.getKey(), grcRow);
        }
        return grc;
    }

    // STEP 3: Grey Relational Grade (GRG)
    public Map<String, Double> computeGRG(
            Map<String, Map<String, Double>> grcMatrix,
            Map<String, Double> weights
    ) {
        Map<String, Double> grg = new LinkedHashMap<>();

        for (var techEntry : grcMatrix.entrySet()) {
            double grade = 0.0;

            for (var criterionEntry : techEntry.getValue().entrySet()) {
                String criterion = criterionEntry.getKey();
                double grcValue  = criterionEntry.getValue();
                double weight    = weights.getOrDefault(criterion, 0.0);

                grade += weight * grcValue;
            }
            grg.put(techEntry.getKey(), round(grade));
        }
        return grg;
    }
}