package com.company.fucomhgra.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
public class SensitivityServices {

    private final FucomService fucomService;
    private final HgraService hgraService;

    public SensitivityServices(FucomService fucomService, HgraService hgraService) {
        this.fucomService = fucomService;
        this.hgraService = hgraService;
    }

    private static final List<String> CRITERIA = List.of(
            "Power", "Land", "CapEx", "O&M",
            "BOD", "COD", "TSS", "GWP", "EP"
    );

    private static final List<String> BENEFIT_CRITERIA = List.of("BOD", "COD", "TSS");

    // ─────────────────────────────────────────────
    // MAIN METHOD
    // ─────────────────────────────────────────────
    public Map<String, Object> runSensitivityAnalysis(
            MultipartFile file,
            List<String> priorityOrder,
            Map<String, Double> comparativeRatios
    ) throws Exception {

        // Step 1: Parse Excel
        Map<String, List<Map<String, Double>>> allData = parseExcel(file);

        // Step 2: Validate minimum 2 technologies
        if (allData.size() < 2) {
            throw new IllegalArgumentException(
                    "At least 2 technology sheets are required. Found: " + allData.size()
            );
        }

        // Step 3: Calculate averages
        Map<String, Map<String, Double>> averages = calculateAverages(allData);

        // Step 4: Compute FUCOM weights
        Map<String, Double> weights = fucomService.computeWeights(
                priorityOrder, comparativeRatios
        );

        // Step 5: Run simulations
        List<String> technologies = new ArrayList<>(allData.keySet());
        Map<String, Map<Integer, Integer>> rankCounts = new LinkedHashMap<>();
        Map<String, List<Double>> grgScores = new LinkedHashMap<>();

        for (String tech : technologies) {
            rankCounts.put(tech, new LinkedHashMap<>());
            grgScores.put(tech, new ArrayList<>());
            for (int r = 1; r <= technologies.size(); r++) {
                rankCounts.get(tech).put(r, 0);
            }
        }

        List<Map<String, Object>> allSimulations = new ArrayList<>();
        int simulationNumber = 0;
        Map<String, Integer> rowCountPerTech = new LinkedHashMap<>();

        for (String varyingTech : technologies) {
            List<Map<String, Double>> rows = allData.get(varyingTech);
            rowCountPerTech.put(varyingTech, rows.size());

            for (int i = 0; i < rows.size(); i++) {
                simulationNumber++;

                Map<String, Map<String, Double>> matrix = new LinkedHashMap<>();
                for (String tech : technologies) {
                    matrix.put(tech, tech.equals(varyingTech)
                            ? rows.get(i)
                            : averages.get(tech));
                }

                // Run FUCOM-HGRA
                Map<String, Map<String, Double>> normalized =
                        hgraService.normalise(matrix, BENEFIT_CRITERIA);
                Map<String, Map<String, Double>> grc =
                        hgraService.computeGRC(normalized);
                Map<String, Double> grg =
                        hgraService.computeGRG(grc, weights);

                // Rank
                List<Map.Entry<String, Double>> sorted =
                        new ArrayList<>(grg.entrySet());
                sorted.sort(Map.Entry.<String, Double>comparingByValue().reversed());

                Map<String, Integer> ranks = new LinkedHashMap<>();
                for (int r = 0; r < sorted.size(); r++) {
                    String tech = sorted.get(r).getKey();
                    int rank = r + 1;
                    ranks.put(tech, rank);
                    rankCounts.get(tech).merge(rank, 1, Integer::sum);
                    grgScores.get(tech).add(grg.get(tech));
                }

                Map<String, Object> sim = new LinkedHashMap<>();
                sim.put("simulation", simulationNumber);
                sim.put("varyingTechnology", varyingTech);
                sim.put("row", i + 1);
                sim.put("ranks", ranks);
                sim.put("grgScores", grg);
                allSimulations.add(sim);
            }
        }

        // Step 6: Statistics
        Map<String, Map<String, Object>> statistics = new LinkedHashMap<>();
        for (String tech : technologies) {
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("rankDistribution", rankCounts.get(tech));
            stats.put("rowsUsed", rowCountPerTech.get(tech));

            int mostFrequentRank = rankCounts.get(tech).entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(1);
            stats.put("mostFrequentRank", mostFrequentRank);

            int mostFrequentCount = rankCounts.get(tech).get(mostFrequentRank);
            double stabilityScore = (double) mostFrequentCount / simulationNumber * 100;
            stats.put("stabilityScore", Math.round(stabilityScore * 100.0) / 100.0);

            double avgGrg = grgScores.get(tech).stream()
                    .mapToDouble(Double::doubleValue).average().orElse(0.0);
            double minGrg = grgScores.get(tech).stream()
                    .mapToDouble(Double::doubleValue).min().orElse(0.0);
            double maxGrg = grgScores.get(tech).stream()
                    .mapToDouble(Double::doubleValue).max().orElse(0.0);

            stats.put("averageGRG", Math.round(avgGrg * 10000.0) / 10000.0);
            stats.put("minGRG",     Math.round(minGrg * 10000.0) / 10000.0);
            stats.put("maxGRG",     Math.round(maxGrg * 10000.0) / 10000.0);
            statistics.put(tech, stats);
        }

        // Step 7: Conclusion
        String mostStableTech = statistics.entrySet().stream()
                .filter(e -> (int) e.getValue().get("mostFrequentRank") == 1)
                .max(Comparator.comparingDouble(
                        e -> (double) e.getValue().get("stabilityScore")))
                .map(Map.Entry::getKey)
                .orElse("No clear winner — rankings are unstable");

        // Build response
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalSimulations", simulationNumber);
        response.put("rowCountPerTechnology", rowCountPerTech);
        response.put("technologies", technologies);
        response.put("fucomWeights", weights);
        response.put("averageValues", averages);
        response.put("statistics", statistics);
        response.put("conclusion", mostStableTech +
                " is the most stable top-ranked technology");
        response.put("simulations", allSimulations);

        return response;
    }

    // ─────────────────────────────────────────────
    // Parse Excel — robust version
    // ─────────────────────────────────────────────
    private Map<String, List<Map<String, Double>>> parseExcel(MultipartFile file)
            throws Exception {

        Map<String, List<Map<String, Double>>> result = new LinkedHashMap<>();
        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Set<String> seenSheets = new HashSet<>();

        for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
            Sheet sheet = workbook.getSheetAt(s);

            // Skip sheets with less than 2 rows
            if (sheet.getLastRowNum() < 2) {
                continue;
            }

            // Normalize sheet name
            String techName = sheet.getSheetName()
                    .trim()
                    .toUpperCase()
                    .replaceAll("\\s+", "");

            // Check for duplicates
            if (seenSheets.contains(techName)) {
                workbook.close();
                throw new IllegalArgumentException(
                        "Duplicate sheet name found: " + techName
                );
            }
            seenSheets.add(techName);

            // Smart header detection — scan first 10 rows
            Row headerRow = null;
            int dataStartRow = 1;

            for (int r = 0; r <= Math.min(10, sheet.getLastRowNum()); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                for (Cell cell : row) {
                    if (cell.getCellType() != CellType.STRING) continue;
                    String val = cell.getStringCellValue().trim().toLowerCase();
                    if (val.contains("power") || val.contains("bod") ||
                            val.contains("cod")   || val.contains("gwp") ||
                            val.contains("land")  || val.contains("capex") ||
                            val.contains("tss")   || val.contains("ep")) {
                        headerRow = row;
                        dataStartRow = r + 1;
                        break;
                    }
                }
                if (headerRow != null) break;
            }

            if (headerRow == null) {
                workbook.close();
                throw new IllegalArgumentException(
                        "Could not find header row in sheet: " + techName +
                                ". Make sure columns include Power, BOD, COD, TSS, GWP, EP, Land, CapEx, O&M"
                );
            }

            // Find column indices
            Map<String, Integer> columnIndices = findColumnIndices(headerRow);

            // Check for missing columns
            List<String> missingCriteria = CRITERIA.stream()
                    .filter(c -> !columnIndices.containsKey(c))
                    .toList();

            if (!missingCriteria.isEmpty()) {
                workbook.close();
                throw new IllegalArgumentException(
                        "Missing columns in sheet '" + techName + "': " + missingCriteria +
                                ". Please make sure all 9 criteria columns are present."
                );
            }

            // Read data rows
            List<Map<String, Double>> rows = new ArrayList<>();
            int skippedRows = 0;

            for (int r = dataStartRow; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) { skippedRows++; continue; }

                Map<String, Double> rowData = new LinkedHashMap<>();
                boolean validRow = true;

                for (String criterion : CRITERIA) {
                    Integer colIdx = columnIndices.get(criterion);
                    Cell cell = row.getCell(colIdx);

                    if (cell == null ||
                            cell.getCellType() == CellType.BLANK ||
                            cell.getCellType() == CellType.STRING) {
                        validRow = false;
                        skippedRows++;
                        break;
                    }
                    rowData.put(criterion, getNumericValue(cell));
                }

                if (validRow) rows.add(rowData);
            }

            // Validate at least 1 valid row
            if (rows.isEmpty()) {
                workbook.close();
                throw new IllegalArgumentException(
                        "Sheet '" + techName + "' has no valid data rows. " +
                                skippedRows + " rows were skipped due to missing/invalid values."
                );
            }

            result.put(techName, rows);
        }

        workbook.close();

        if (result.isEmpty()) {
            throw new IllegalArgumentException(
                    "No valid sheets found in the uploaded file."
            );
        }

        return result;
    }

    // ─────────────────────────────────────────────
    // Find column indices from header row
    // ─────────────────────────────────────────────
    private Map<String, Integer> findColumnIndices(Row headerRow) {
        Map<String, Integer> indices = new HashMap<>();

        for (Cell cell : headerRow) {
            if (cell.getCellType() != CellType.STRING) continue;
            String header = cell.getStringCellValue().trim().toLowerCase();

            if (header.contains("power"))                       indices.put("Power", cell.getColumnIndex());
            else if (header.contains("land"))                   indices.put("Land",  cell.getColumnIndex());
            else if (header.contains("capex") ||
                    header.contains("capital"))                indices.put("CapEx", cell.getColumnIndex());
            else if (header.contains("o&m") ||
                    header.contains("om") ||
                    header.contains("operation"))              indices.put("O&M",   cell.getColumnIndex());
            else if (header.contains("bod"))                    indices.put("BOD",   cell.getColumnIndex());
            else if (header.contains("cod"))                    indices.put("COD",   cell.getColumnIndex());
            else if (header.contains("tss"))                    indices.put("TSS",   cell.getColumnIndex());
            else if (header.contains("gwp") ||
                    header.contains("global warming"))         indices.put("GWP",   cell.getColumnIndex());
            else if (header.contains("ep") ||
                    header.contains("eutro"))                  indices.put("EP",    cell.getColumnIndex());
        }
        return indices;
    }

    // ─────────────────────────────────────────────
    // Calculate averages
    // ─────────────────────────────────────────────
    private Map<String, Map<String, Double>> calculateAverages(
            Map<String, List<Map<String, Double>>> allData) {

        Map<String, Map<String, Double>> averages = new LinkedHashMap<>();

        for (Map.Entry<String, List<Map<String, Double>>> entry : allData.entrySet()) {
            String tech = entry.getKey();
            List<Map<String, Double>> rows = entry.getValue();
            Map<String, Double> avg = new LinkedHashMap<>();

            for (String criterion : CRITERIA) {
                double sum = rows.stream()
                        .mapToDouble(row -> row.getOrDefault(criterion, 0.0))
                        .sum();
                avg.put(criterion, Math.round((sum / rows.size()) * 10000.0) / 10000.0);
            }
            averages.put(tech, avg);
        }
        return averages;
    }

    // ─────────────────────────────────────────────
    // Get numeric value from cell
    // ─────────────────────────────────────────────
    private double getNumericValue(Cell cell) {
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING  -> {
                try {
                    yield Double.parseDouble(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    yield 0.0;
                }
            }
            default -> 0.0;
        };
    }
}