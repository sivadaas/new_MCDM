package com.company.fucomhgra.controller;

import com.company.fucomhgra.dto.MyDecisionRequest;
import com.company.fucomhgra.dto.RankResultDTO;
import com.company.fucomhgra.entity.ApiKey;
import com.company.fucomhgra.entity.User;
import com.company.fucomhgra.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/keys")
@CrossOrigin(origins = "*")
public class ApiKeyController {

    @Autowired private ApiKeyServices apiKeyService;
    @Autowired private AuthService authService;
    @Autowired private FucomService fucomService;
    @Autowired private HgraService hgraService;

    // ─────────────────────────────────────────────
    // Generate API Key
    // POST /api/keys/generate
    // ─────────────────────────────────────────────
    @PostMapping("/generate")
    public ResponseEntity<?> generateKey(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body) {
        try {
            User user = getUserFromHeader(authHeader);
            String keyName = body.getOrDefault("name", "Default Key");

            ApiKey apiKey = apiKeyService.generateApiKey(user, keyName);

            return ResponseEntity.ok(Map.of(
                    "id",         apiKey.getId(),
                    "name",       apiKey.getName(),
                    "keyValue",   apiKey.getKeyValue(),
                    "isActive",   apiKey.getIsActive(),
                    "createdAt",  apiKey.getCreatedAt().toString(),
                    "message",    "Save this key safely — it won't be shown again!"
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", 500,
                    "message", e.getMessage()
            ));
        }
    }

    // ─────────────────────────────────────────────
    // List All API Keys
    // GET /api/keys
    // ─────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<?> listKeys(
            @RequestHeader("Authorization") String authHeader) {
        try {
            User user = getUserFromHeader(authHeader);
            List<ApiKey> keys = apiKeyService.getUserApiKeys(user);

            List<Map<String, Object>> response = keys.stream()
                    .map(k -> Map.of(
                            "id",          (Object) k.getId(),
                            "name",        k.getName(),
                            "keyPreview",  maskKey(k.getKeyValue()),
                            "isActive",    k.getIsActive(),
                            "createdAt",   k.getCreatedAt().toString(),
                            "lastUsedAt",  k.getLastUsedAt() != null ?
                                    k.getLastUsedAt().toString() : "Never"
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
    // Revoke API Key
    // PUT /api/keys/{id}/revoke
    // ─────────────────────────────────────────────
    @PutMapping("/{id}/revoke")
    public ResponseEntity<?> revokeKey(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        try {
            User user = getUserFromHeader(authHeader);
            apiKeyService.revokeApiKey(id, user);

            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "API key revoked successfully"
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
    // Delete API Key
    // DELETE /api/keys/{id}
    // ─────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteKey(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        try {
            User user = getUserFromHeader(authHeader);
            apiKeyService.deleteApiKey(id, user);

            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "API key deleted successfully"
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
    // Use API Key to run calculation (Enterprise endpoint)
    // POST /api/keys/calculate
    // ─────────────────────────────────────────────
    @PostMapping("/calculate")
    public ResponseEntity<?> calculateWithApiKey(
            @RequestHeader("X-Api-Key") String apiKey,
            @Valid @RequestBody MyDecisionRequest request) {
        try {
            // Validate API key and get user
            User user = apiKeyService.validateApiKey(apiKey);

            // Run FUCOM-HGRA calculation
            Map<String, Double> weights = fucomService.computeWeights(
                    request.getPriorityOrder(),
                    request.getComparativeRatios()
            );

            Map<String, Map<String, Double>> normalized =
                    hgraService.normalise(
                            request.getDecisionMatrix(),
                            request.getBenefitCriteria()
                    );

            Map<String, Map<String, Double>> grc =
                    hgraService.computeGRC(normalized);

            Map<String, Double> grg =
                    hgraService.computeGRG(grc, weights);

            // Sort and rank
            List<RankResultDTO> results = new ArrayList<>();
            List<Map.Entry<String, Double>> sorted =
                    new ArrayList<>(grg.entrySet());
            sorted.sort(
                    Map.Entry.<String, Double>comparingByValue().reversed()
            );

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

            return ResponseEntity.ok(Map.of(
                    "user",     user.getName(),
                    "rankings", results,
                    "weights",  weights
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", 401,
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
    // Helpers
    // ─────────────────────────────────────────────
    private User getUserFromHeader(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return authService.getUserFromToken(token);
    }

    // Show only first 12 chars: sk-wwr-a3f9x2k8****
    private String maskKey(String keyValue) {
        if (keyValue.length() <= 12) return "****";
        return keyValue.substring(0, 12) + "****";
    }
}