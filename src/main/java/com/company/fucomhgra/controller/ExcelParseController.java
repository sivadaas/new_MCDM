package com.company.fucomhgra.controller;

import com.company.fucomhgra.service.ExcelParseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/excel")
@CrossOrigin(origins = "*")
public class ExcelParseController {

    @Autowired
    private ExcelParseService excelParseService;

    // ─────────────────────────────────────────────
    // Parse Simple Excel File
    // POST /api/excel/parse
    // ─────────────────────────────────────────────
    @PostMapping("/parse")
    public ResponseEntity<?> parseExcel(
            @RequestParam("file") MultipartFile file) {
        try {
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

            // Parse Excel
            Map<String, Object> result = excelParseService
                    .parseSimpleExcel(file);

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", 500,
                    "message", "Failed to parse Excel: " + e.getMessage()
            ));
        }
    }
}