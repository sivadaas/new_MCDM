package com.company.fucomhgra.controller;

import com.company.fucomhgra.entity.Analysis;
import com.company.fucomhgra.entity.User;
import com.company.fucomhgra.service.AnalysisService;
import com.company.fucomhgra.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/share")
@CrossOrigin(origins = "*")
public class ShareController {

    @Autowired private AnalysisService analysisService;
    @Autowired private AuthService authService;

    // ─────────────────────────────────────────────
    // Generate shareable link
    // POST /api/share/{analysisId}
    // ─────────────────────────────────────────────
    @PostMapping("/{analysisId}")
    public ResponseEntity<?> generateShareLink(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long analysisId) {
        try {
            String token = authHeader.replace("Bearer ", "");
            User user = authService.getUserFromToken(token);

            String shareToken = analysisService.generateShareToken(
                    analysisId, user
            );

            return ResponseEntity.ok(Map.of(
                    "shareToken", shareToken,
                    "shareUrl", "/api/share/view/" + shareToken,
                    "message", "Share this link with anyone to view results"
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", 500,
                    "message", e.getMessage()
            ));
        }
    }

    // ─────────────────────────────────────────────
    // View shared analysis (no auth needed)
    // GET /api/share/view/{shareToken}
    // ─────────────────────────────────────────────
    @GetMapping("/view/{shareToken}")
    public ResponseEntity<?> viewSharedAnalysis(
            @PathVariable String shareToken) {
        try {
            Analysis analysis = analysisService
                    .getAnalysisByShareToken(shareToken);

            return ResponseEntity.ok(Map.of(
                    "projectName", analysis.getProject().getName(),
                    "createdAt",   analysis.getCreatedAt().toString(),
                    "resultData",  analysis.getResultData()
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of(
                    "status", 404,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", 500,
                    "message", e.getMessage()
            ));
        }
    }
}

