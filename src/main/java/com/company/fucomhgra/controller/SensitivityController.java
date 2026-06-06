package com.company.fucomhgra.controller;

import com.company.fucomhgra.service.AuthService;
import com.company.fucomhgra.service.SensitivityServices;
import com.company.fucomhgra.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sensitivity")
@CrossOrigin(origins = "*")
public class SensitivityController {

    @Autowired private SensitivityServices sensitivityService;
    @Autowired private AuthService authService;

    // ─────────────────────────────────────────────
    // Run Sensitivity Analysis
    // POST /api/sensitivity/analyse
    // ─────────────────────────────────────────────
    @PostMapping("/analyse")
    public ResponseEntity<?> analyse(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("file") MultipartFile file,
            @RequestParam("priorityOrder") String priorityOrderJson,
            @RequestParam("comparativeRatios") String comparativeRatiosJson
    ) {
        try {
            // Validate user
            String token = authHeader.replace("Bearer ", "");
            User user = authService.getUserFromToken(token);

            // Parse JSON strings from form data
            ObjectMapper mapper = new ObjectMapper();
            List<String> priorityOrder = mapper.readValue(
                    priorityOrderJson,
                    mapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );
            Map<String, Double> comparativeRatios = mapper.readValue(
                    comparativeRatiosJson,
                    mapper.getTypeFactory().constructMapType(Map.class, String.class, Double.class)
            );

            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", 400,
                        "message", "Please upload an Excel file"
                ));
            }

            String filename = file.getOriginalFilename();
            if (filename == null || !filename.endsWith(".xlsx")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", 400,
                        "message", "Only .xlsx files are supported"
                ));
            }

            // Run sensitivity analysis
            Map<String, Object> result = sensitivityService.runSensitivityAnalysis(
                    file, priorityOrder, comparativeRatios
            );

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", 500,
                    "message", "Analysis failed: " + e.getMessage()
            ));
        }
    }
}