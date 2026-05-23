package com.company.fucomhgra.controller;

import com.company.fucomhgra.dto.MyDecisionRequest;
import com.company.fucomhgra.dto.RankResultDTO;
import com.company.fucomhgra.entity.Analysis;
import com.company.fucomhgra.entity.User;
import com.company.fucomhgra.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AnalysisController {

    @Autowired private FucomService fucomService;
    @Autowired private HgraService hgraService;
    @Autowired private AnalysisService analysisService;
    @Autowired private AuthService authService;

    // ─────────────────────────────────────────────
    // Run Analysis + Save to DB
    // POST /api/projects/{projectId}/analyse
    // ─────────────────────────────────────────────
    @PostMapping("/projects/{projectId}/analyse")
    public ResponseEntity<?> analyse(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long projectId,
            @Valid @RequestBody MyDecisionRequest request) {
        try {
            User user = getUserFromHeader(authHeader);

            // Step 1: FUCOM weights
            Map<String, Double> weights = fucomService.computeWeights(
                    request.getPriorityOrder(),
                    request.getComparativeRatios()
            );

            // Step 2: Normalize
            Map<String, Map<String, Double>> normalized = hgraService.normalise(
                    request.getDecisionMatrix(),
                    request.getBenefitCriteria()
            );

            // Step 3: GRC
            Map<String, Map<String, Double>> grc = hgraService.computeGRC(normalized);

            // Step 4: GRG
            Map<String, Double> grg = hgraService.computeGRG(grc, weights);

            // Step 5: Sort and rank
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

            // Step 6: Save to database
            Map<String, Object> inputData = new LinkedHashMap<>();
            inputData.put("priorityOrder", request.getPriorityOrder());
            inputData.put("comparativeRatios", request.getComparativeRatios());
            inputData.put("decisionMatrix", request.getDecisionMatrix());
            inputData.put("benefitCriteria", request.getBenefitCriteria());

            Map<String, Object> resultData = new LinkedHashMap<>();
            resultData.put("weights", weights);
            resultData.put("rankings", results);

            Analysis saved = analysisService.saveAnalysis(
                    user, projectId, inputData, resultData
            );

            // Step 7: Return results with analysis ID
            return ResponseEntity.ok(Map.of(
                    "analysisId", saved.getId(),
                    "createdAt",  saved.getCreatedAt().toString(),
                    "rankings",   results,
                    "weights",    weights
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", 500,
                    "message", e.getMessage()
            ));
        }
    }

    // ─────────────────────────────────────────────
    // Get All Analyses for a Project
    // GET /api/projects/{projectId}/analyses
    // ─────────────────────────────────────────────
    @GetMapping("/projects/{projectId}/analyses")
    public ResponseEntity<?> getProjectAnalyses(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long projectId) {
        try {
            User user = getUserFromHeader(authHeader);
            List<Analysis> analyses = analysisService
                    .getProjectAnalyses(projectId, user);

            List<Map<String, Object>> response = analyses.stream()
                    .map(a -> Map.of(
                            "id",         (Object) a.getId(),
                            "createdAt",  a.getCreatedAt().toString(),
                            "resultData", a.getResultData()
                    ))
                    .toList();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", 500,
                    "message", e.getMessage()
            ));
        }
    }

    // ─────────────────────────────────────────────
    // Get One Analysis
    // GET /api/analyses/{id}
    // ─────────────────────────────────────────────
    @GetMapping("/analyses/{id}")
    public ResponseEntity<?> getAnalysis(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        try {
            User user = getUserFromHeader(authHeader);
            Analysis analysis = analysisService.getAnalysisById(id, user);

            return ResponseEntity.ok(Map.of(
                    "id",         analysis.getId(),
                    "createdAt",  analysis.getCreatedAt().toString(),
                    "inputData",  analysis.getInputData(),
                    "resultData", analysis.getResultData()
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of(
                    "status", 403,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", 500,
                    "message", e.getMessage()
            ));
        }
    }

    // ─────────────────────────────────────────────
    // Delete Analysis
    // DELETE /api/analyses/{id}
    // ─────────────────────────────────────────────
    @DeleteMapping("/analyses/{id}")
    public ResponseEntity<?> deleteAnalysis(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        try {
            User user = getUserFromHeader(authHeader);
            analysisService.deleteAnalysis(id, user);

            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Analysis deleted successfully"
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", 500,
                    "message", e.getMessage()
            ));
        }
    }

    // ─────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────
    private User getUserFromHeader(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return authService.getUserFromToken(token);
    }
}