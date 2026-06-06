package com.company.fucomhgra.controller;

import com.company.fucomhgra.service.ReferenceDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reference")
@CrossOrigin(origins = "*")
public class ReferenceDataController {

    @Autowired
    private ReferenceDataService referenceDataService;

    // GET /api/reference/matrix
    // Returns complete 9x5 decision matrix
    @GetMapping("/matrix")
    public ResponseEntity<?> getDefaultMatrix() {
        try {
            Map<String, Map<String, Double>> matrix =
                    referenceDataService.getDefaultDecisionMatrix();
            return ResponseEntity.ok(matrix);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", 500,
                    "message", e.getMessage()
            ));
        }
    }

    // GET /api/reference/all
    // Returns all data with min, max, typical, unit, source
    @GetMapping("/all")
    public ResponseEntity<?> getAllReferenceData() {
        try {
            return ResponseEntity.ok(
                    referenceDataService.getAllReferenceData()
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", 500,
                    "message", e.getMessage()
            ));
        }
    }

    // GET /api/reference/{technology}
    // Returns all data for one technology
    @GetMapping("/{technology}")
    public ResponseEntity<?> getTechnologyData(
            @PathVariable String technology) {
        try {
            return ResponseEntity.ok(
                    referenceDataService.getTechnologyData(technology)
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", 500,
                    "message", e.getMessage()
            ));
        }
    }
}
