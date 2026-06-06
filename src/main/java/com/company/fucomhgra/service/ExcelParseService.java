package com.company.fucomhgra.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExcelParseService {

    private static final Map<String, String> CRITERIA_KEYWORDS = new LinkedHashMap<>();
    private static final Set<String> KNOWN_TECHNOLOGIES = new HashSet<>(
            Arrays.asList("MBBR", "MBR", "UASB", "SBR", "ASP")
    );

    static {
        CRITERIA_KEYWORDS.put("power",    "Power");
        CRITERIA_KEYWORDS.put("land",     "Land");
        CRITERIA_KEYWORDS.put("capex",    "CapEx");
        CRITERIA_KEYWORDS.put("capital",  "CapEx");
        CRITERIA_KEYWORDS.put("o&m",      "O&M");
        CRITERIA_KEYWORDS.put("om",       "O&M");
        CRITERIA_KEYWORDS.put("operation","O&M");
        CRITERIA_KEYWORDS.put("bod",      "BOD");
        CRITERIA_KEYWORDS.put("cod",      "COD");
        CRITERIA_KEYWORDS.put("tss",      "TSS");
        CRITERIA_KEYWORDS.put("gwp",      "GWP");
        CRITERIA_KEYWORDS.put("global",   "GWP");
        CRITERIA_KEYWORDS.put("ep",       "EP");
        CRITERIA_KEYWORDS.put("eutro",    "EP");
    }

    // ─────────────────────────────────────────────
    // Parse Simple Excel File
    // ─────────────────────────────────────────────
    public Map<String, Object> parseSimpleExcel(MultipartFile file)
            throws Exception {

        Workbook workbook = new XSSFWorkbook(file.getInputStream());

        // Step 1: Find best sheet
        Sheet sheet = findBestSheet(workbook);
        if (sheet == null) {
            workbook.close();
            throw new IllegalArgumentException(
                    "No valid data sheet found in Excel file"
            );
        }

        // Step 2: Find header row + technology columns
        HeaderInfo headerInfo = findHeaderRow(sheet);
        if (headerInfo == null) {
            workbook.close();
            throw new IllegalArgumentException(
                    "Could not find technology names. " +
                            "Make sure header row contains technology names " +
                            "like MBBR, MBR, UASB, SBR, ASP"
            );
        }

        // Step 3: Read criteria rows
        Map<String, Map<String, Double>> decisionMatrix =
                readCriteriaRows(sheet, headerInfo);

        workbook.close();

        // Step 4: Validate
        List<String> requiredCriteria = List.of(
                "Power", "Land", "CapEx", "O&M",
                "BOD", "COD", "TSS", "GWP", "EP"
        );

        List<String> foundCriteria = new ArrayList<>(
                decisionMatrix.values().stream()
                        .findFirst()
                        .map(Map::keySet)
                        .orElse(new LinkedHashSet<>())
        );

        List<String> missingCriteria = requiredCriteria.stream()
                .filter(c -> !foundCriteria.contains(c))
                .toList();

        // Step 5: Build response
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sheetUsed", sheet.getSheetName());
        response.put("technologies", headerInfo.technologies);
        response.put("criteriaFound", foundCriteria);
        response.put("decisionMatrix", decisionMatrix);

        if (!missingCriteria.isEmpty()) {
            response.put("warnings",
                    "Missing criteria: " + missingCriteria +
                            ". Please check your Excel file."
            );
        }

        return response;
    }

    // ─────────────────────────────────────────────
    // Find best sheet — prefers sheet with most data
    // ─────────────────────────────────────────────
    private Sheet findBestSheet(Workbook workbook) {
        Sheet bestSheet = null;
        int bestScore = 0;

        for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
            Sheet sheet = workbook.getSheetAt(s);

            // Skip empty sheets
            if (sheet.getLastRowNum() < 2) continue;

            // Score sheet by how many criteria keywords it contains
            int score = 0;
            for (int r = 0; r <= Math.min(20, sheet.getLastRowNum()); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                for (Cell cell : row) {
                    if (cell.getCellType() != CellType.STRING) continue;
                    String val = cell.getStringCellValue()
                            .trim().toLowerCase();
                    for (String keyword : CRITERIA_KEYWORDS.keySet()) {
                        if (val.contains(keyword)) {
                            score++;
                            break;
                        }
                    }
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestSheet = sheet;
            }
        }

        // Fallback to first sheet
        return bestSheet != null ? bestSheet : workbook.getSheetAt(0);
    }

    // ─────────────────────────────────────────────
    // Find header row and technology column indices
    // ─────────────────────────────────────────────
    private HeaderInfo findHeaderRow(Sheet sheet) {

        for (int r = 0; r <= Math.min(15, sheet.getLastRowNum()); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            List<String> technologies = new ArrayList<>();
            List<Integer> colIndices = new ArrayList<>();

            for (int c = 0; c < row.getLastCellNum(); c++) {
                Cell cell = row.getCell(c);
                if (cell == null) continue;

                String val = getCellStringValue(cell).trim();
                if (val.isEmpty()) continue;

                // Normalize: uppercase, remove spaces/hyphens
                String normalized = val.toUpperCase()
                        .replaceAll("[\\s\\-_]+", "");

                // Check against known technologies
                // or check if it looks like a tech name
                // (short, alphanumeric, not a unit or number)
                boolean isKnownTech = KNOWN_TECHNOLOGIES.contains(normalized);
                boolean looksLikeTech = normalized.length() <= 8
                        && normalized.matches("[A-Z0-9]+")
                        && !isUnit(val)
                        && !isNumeric(val);

                if (isKnownTech || looksLikeTech) {
                    // Verify next rows have numeric data in this column
                    if (hasNumericDataBelow(sheet, r, c, 3)) {
                        technologies.add(normalized);
                        colIndices.add(c);
                    }
                }
            }

            // Valid header needs at least 2 technologies
            if (technologies.size() >= 2) {
                // Verify next row has criteria keyword
                Row nextRow = sheet.getRow(r + 1);
                if (nextRow != null && hasCriteriaKeyword(nextRow)) {
                    return new HeaderInfo(technologies, colIndices, r + 1);
                }
            }
        }
        return null;
    }

    // ─────────────────────────────────────────────
    // Read criteria rows and extract values
    // ─────────────────────────────────────────────
    private Map<String, Map<String, Double>> readCriteriaRows(
            Sheet sheet, HeaderInfo headerInfo) {

        Map<String, Map<String, Double>> matrix = new LinkedHashMap<>();
        for (String tech : headerInfo.technologies) {
            matrix.put(tech, new LinkedHashMap<>());
        }

        Set<String> foundCriteria = new HashSet<>();

        for (int r = headerInfo.dataStartRow;
             r <= sheet.getLastRowNum(); r++) {

            // Stop if all 9 criteria found
            if (foundCriteria.size() >= 9) break;

            Row row = sheet.getRow(r);
            if (row == null) continue;

            // Get criterion from first cell
            Cell firstCell = row.getCell(0);
            if (firstCell == null) continue;

            String criterionRaw = getCellStringValue(firstCell)
                    .trim().toLowerCase();
            if (criterionRaw.isEmpty()) continue;

            // Map to standard criterion name
            String criterionName = mapToCriterion(criterionRaw);
            if (criterionName == null) continue;
            if (foundCriteria.contains(criterionName)) continue;

            // Read values for each technology
            boolean hasAnyValue = false;
            for (int t = 0; t < headerInfo.technologies.size(); t++) {
                int colIdx = headerInfo.colIndices.get(t);
                Cell cell = row.getCell(colIdx);
                if (cell == null) continue;

                double value = getNumericValue(cell);
                if (value != 0.0) hasAnyValue = true;
                matrix.get(headerInfo.technologies.get(t))
                        .put(criterionName, value);
            }

            if (hasAnyValue) foundCriteria.add(criterionName);
        }

        return matrix;
    }

    // ─────────────────────────────────────────────
    // Helper — map raw string to criterion name
    // ─────────────────────────────────────────────
    private String mapToCriterion(String raw) {
        for (Map.Entry<String, String> entry : CRITERIA_KEYWORDS.entrySet()) {
            if (raw.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    // ─────────────────────────────────────────────
    // Helper — check if column has numeric data below
    // ─────────────────────────────────────────────
    private boolean hasNumericDataBelow(Sheet sheet,
                                        int headerRow, int col, int rowsToCheck) {
        int found = 0;
        for (int r = headerRow + 1;
             r <= Math.min(headerRow + rowsToCheck + 5,
                     sheet.getLastRowNum()); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            Cell cell = row.getCell(col);
            if (cell == null) continue;
            if (cell.getCellType() == CellType.NUMERIC ||
                    cell.getCellType() == CellType.FORMULA) {
                found++;
                if (found >= rowsToCheck) return true;
            }
        }
        return false;
    }

    // ─────────────────────────────────────────────
    // Helper — check if row has criteria keyword
    // ─────────────────────────────────────────────
    private boolean hasCriteriaKeyword(Row row) {
        for (Cell cell : row) {
            if (cell.getCellType() != CellType.STRING) continue;
            String val = cell.getStringCellValue().trim().toLowerCase();
            for (String keyword : CRITERIA_KEYWORDS.keySet()) {
                if (val.contains(keyword)) return true;
            }
        }
        return false;
    }

    // ─────────────────────────────────────────────
    // Helper — check if value is a unit string
    // ─────────────────────────────────────────────
    private boolean isUnit(String val) {
        String lower = val.toLowerCase();
        return lower.contains("kwh") || lower.contains("m²") ||
                lower.contains("cr")  || lower.contains("lakh") ||
                lower.contains("kg")  || lower.contains("eq") ||
                lower.contains("%")   || lower.contains("/");
    }

    // ─────────────────────────────────────────────
    // Helper — check if value is numeric
    // ─────────────────────────────────────────────
    private boolean isNumeric(String val) {
        try {
            Double.parseDouble(val);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // ─────────────────────────────────────────────
    // Helper — get string value from any cell type
    // ─────────────────────────────────────────────
    private String getCellStringValue(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(
                    (long) cell.getNumericCellValue()
            );
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(cell.getStringCellValue());
                } catch (Exception e) {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            default -> "";
        };
    }

    // ─────────────────────────────────────────────
    // Helper — get numeric value safely
    // ─────────────────────────────────────────────
    private double getNumericValue(Cell cell) {
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case FORMULA -> {
                try {
                    yield cell.getNumericCellValue();
                } catch (Exception e) {
                    yield 0.0;
                }
            }
            case STRING -> {
                try {
                    yield Double.parseDouble(
                            cell.getStringCellValue().trim()
                    );
                } catch (NumberFormatException e) {
                    yield 0.0;
                }
            }
            default -> 0.0;
        };
    }

    // ─────────────────────────────────────────────
    // Inner class — header info
    // ─────────────────────────────────────────────
    private static class HeaderInfo {
        List<String> technologies;
        List<Integer> colIndices;
        int dataStartRow;

        HeaderInfo(List<String> technologies,
                   List<Integer> colIndices,
                   int dataStartRow) {
            this.technologies = technologies;
            this.colIndices = colIndices;
            this.dataStartRow = dataStartRow;
        }
    }
}