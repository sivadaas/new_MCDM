package com.company.fucomhgra.controller;

import com.company.fucomhgra.entity.Analysis;
import com.company.fucomhgra.entity.User;
import com.company.fucomhgra.service.AnalysisService;
import com.company.fucomhgra.service.AuthService;
import com.company.fucomhgra.service.AuditLogService;
import com.company.fucomhgra.service.PdfExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pdf")
@CrossOrigin(origins = "*")
public class PdfController {

    @Autowired private PdfExportService pdfExportService;
    @Autowired private AnalysisService analysisService;
    @Autowired private AuthService authService;
    @Autowired private AuditLogService auditLogService;

    // ─────────────────────────────────────────────
    // Download PDF for an analysis
    // GET /api/pdf/{analysisId}
    // ─────────────────────────────────────────────
    @GetMapping("/{analysisId}")
    public ResponseEntity<?> downloadPdf(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long analysisId) {
        try {
            // Get user from token
            String token = authHeader.replace("Bearer ", "");
            User user = authService.getUserFromToken(token);

            // Get analysis from DB
            Analysis analysis = analysisService.getAnalysisById(analysisId, user);

            // Extract data
            Map<String, Object> resultData = analysis.getResultData();
            List<Map<String, Object>> rankings =
                    (List<Map<String, Object>>) resultData.get("rankings");
            Map<String, Double> weights =
                    (Map<String, Double>) resultData.get("weights");

            // Generate PDF
            byte[] pdf = pdfExportService.generateAnalysisReport(
                    analysis.getProject().getName(),
                    user.getName(),
                    rankings,
                    weights
            );

            // Log action
            auditLogService.log(
                    user,
                    "DOWNLOADED_PDF",
                    "Analysis ID: " + analysisId
            );

            // Return PDF as download
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=wastewaterrank-analysis-" + analysisId + ".pdf"
                    )
                    .body(pdf);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", 500,
                    "message", e.getMessage()
            ));
        }
    }
}