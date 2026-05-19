package com.company.fucomhgra.controller;

import org.springframework.http.converter.HttpMessageNotReadableException;
import com.company.fucomhgra.dto.MyDecisionRequest;
import com.company.fucomhgra.dto.RankResultDTO;
import com.company.fucomhgra.service.FucomService;
import com.company.fucomhgra.service.HgraService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins ="*")
public class MyDecisionController {
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleParseError(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "status", 400,
                "message", ex.getMostSpecificCause().getMessage()
        ));
    }


    @Autowired
    private FucomService fucomService;

    @Autowired
    private HgraService hgraService;

    // Health Check
    // GET /api/health
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", "FUCOM-HGRA backend is running"
        ));
    }

    // Core Calculation
    // POST /api/calculate
    @PostMapping("/calculate")
    public ResponseEntity<?> calculate(@Valid @RequestBody MyDecisionRequest request) {

        try {
            Map<String, Double> weights = fucomService.computeWeights(
                    request.getPriorityOrder(),
                    request.getComparativeRatios()
            );

            Map<String, Map<String, Double>> normalized = hgraService.normalise(
                    request.getDecisionMatrix(),
                    request.getBenefitCriteria()
            );

            Map<String, Map<String, Double>> grc = hgraService.computeGRC(normalized);

            Map<String, Double> grg = hgraService.computeGRG(grc, weights);

            List<RankResultDTO> results = new ArrayList<>();
            List<Map.Entry<String, Double>> sorted = new ArrayList<>(grg.entrySet());
            sorted.sort(Map.Entry.<String, Double>comparingByValue().reversed());

            for (int i = 0; i < sorted.size(); i++) {
                String tech = sorted.get(i).getKey();
                results.add(RankResultDTO.builder()
                        .technology(tech)
                        .grgScore(sorted.get(i).getValue())
                        .rank(i + 1)
                        .fucomWeights(weights)
                        .grcValues(grc.get(tech))
                        .normalizedValues(normalized.get(tech))
                        .build());
            }

            return ResponseEntity.ok(results);

        }

        catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", 500,
                    "message", "Something went wrong: " + e.getMessage()
            ));
        }
    }
    }
