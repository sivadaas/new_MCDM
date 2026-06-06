package com.company.fucomhgra.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Service
public class PdfExportService {

    // Fonts
    private static final Font TITLE_FONT   = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.DARK_GRAY);
    private static final Font HEADING_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.WHITE);
    private static final Font NORMAL_FONT  = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
    private static final Font BOLD_FONT    = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
    private static final Font RANK_FONT    = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.WHITE);

    // Colors
    private static final BaseColor HEADER_COLOR  = new BaseColor(26, 79, 122);   // Deep blue
    private static final BaseColor GOLD_COLOR    = new BaseColor(245, 166, 35);  // Gold for rank 1
    private static final BaseColor SILVER_COLOR  = new BaseColor(192, 192, 192); // Silver for rank 2
    private static final BaseColor BRONZE_COLOR  = new BaseColor(205, 127, 50);  // Bronze for rank 3
    private static final BaseColor ROW_ALT_COLOR = new BaseColor(245, 247, 250); // Alternate row

    public byte[] generateAnalysisReport(
            String projectName,
            String userName,
            List<Map<String, Object>> rankings,
            Map<String, Double> weights
    ) throws Exception {

        Document document = new Document(PageSize.A4, 50, 50, 60, 60);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        // ── Title Section ──
        addTitle(document, projectName, userName);

        // ── Rankings Section ──
        addRankingsSection(document, rankings);

        // ── Weights Section ──
        addWeightsSection(document, weights);

        // ── Footer ──
        addFooter(document);

        document.close();
        return out.toByteArray();
    }

    private void addTitle(Document doc, String projectName, String userName)
            throws Exception {
        // App name
        Paragraph appName = new Paragraph("WastewaterRank", TITLE_FONT);
        appName.setAlignment(Element.ALIGN_CENTER);
        doc.add(appName);

        // Subtitle
        Paragraph subtitle = new Paragraph(
                "FUCOM-HGRA Analysis Report",
                new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.GRAY)
        );
        subtitle.setAlignment(Element.ALIGN_CENTER);
        doc.add(subtitle);

        doc.add(Chunk.NEWLINE);

        // Project info table
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        addInfoRow(infoTable, "Project", projectName);
        addInfoRow(infoTable, "Prepared By", userName);
        addInfoRow(infoTable, "Date", java.time.LocalDate.now().toString());
        addInfoRow(infoTable, "Method", "FUCOM-HGRA (Grey Relational Analysis)");
        doc.add(infoTable);

        doc.add(Chunk.NEWLINE);
    }

    private void addRankingsSection(Document doc, List<Map<String, Object>> rankings)
            throws Exception {
        // Section heading
        Paragraph heading = new Paragraph("Technology Rankings", BOLD_FONT);
        heading.setSpacingBefore(10);
        doc.add(heading);
        doc.add(Chunk.NEWLINE);

        // Rankings table
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 3, 2, 3});

        // Table headers
        addTableHeader(table, "Rank");
        addTableHeader(table, "Technology");
        addTableHeader(table, "GRG Score");
        addTableHeader(table, "Performance");

        // Table rows
        for (Map<String, Object> result : rankings) {
            int rank = ((Number) result.get("rank")).intValue();
            String tech = (String) result.get("technology");
            double score = ((Number) result.get("grgScore")).doubleValue();

            BaseColor rankColor = rank == 1 ? GOLD_COLOR :
                    rank == 2 ? SILVER_COLOR :
                    rank == 3 ? BRONZE_COLOR : BaseColor.WHITE;

            // Rank cell
            PdfPCell rankCell = new PdfPCell(new Phrase(String.valueOf(rank), RANK_FONT));
            rankCell.setBackgroundColor(rankColor);
            rankCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            rankCell.setPadding(8);
            table.addCell(rankCell);

            // Technology cell
            PdfPCell techCell = new PdfPCell(new Phrase(tech, BOLD_FONT));
            techCell.setPadding(8);
            techCell.setBackgroundColor(rank % 2 == 0 ? ROW_ALT_COLOR : BaseColor.WHITE);
            table.addCell(techCell);

            // Score cell
            PdfPCell scoreCell = new PdfPCell(
                    new Phrase(String.format("%.4f", score), NORMAL_FONT)
            );
            scoreCell.setPadding(8);
            scoreCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            scoreCell.setBackgroundColor(rank % 2 == 0 ? ROW_ALT_COLOR : BaseColor.WHITE);
            table.addCell(scoreCell);

            // Performance bar (text based)
            String bar = "█".repeat((int)(score * 20));
            PdfPCell barCell = new PdfPCell(new Phrase(bar, NORMAL_FONT));
            barCell.setPadding(8);
            barCell.setBackgroundColor(rank % 2 == 0 ? ROW_ALT_COLOR : BaseColor.WHITE);
            table.addCell(barCell);
        }

        doc.add(table);
        doc.add(Chunk.NEWLINE);
    }

    private void addWeightsSection(Document doc, Map<String, Double> weights)
            throws Exception {
        Paragraph heading = new Paragraph("FUCOM Criteria Weights", BOLD_FONT);
        heading.setSpacingBefore(10);
        doc.add(heading);
        doc.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 2, 3});

        addTableHeader(table, "Priority");
        addTableHeader(table, "Criterion");
        addTableHeader(table, "Weight");

        int i = 1;
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            BaseColor bg = i % 2 == 0 ? ROW_ALT_COLOR : BaseColor.WHITE;

            PdfPCell priorityCell = new PdfPCell(new Phrase(String.valueOf(i), NORMAL_FONT));
            priorityCell.setPadding(8);
            priorityCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            priorityCell.setBackgroundColor(bg);
            table.addCell(priorityCell);

            PdfPCell criterionCell = new PdfPCell(new Phrase(entry.getKey(), NORMAL_FONT));
            criterionCell.setPadding(8);
            criterionCell.setBackgroundColor(bg);
            table.addCell(criterionCell);

            PdfPCell weightCell = new PdfPCell(
                    new Phrase(String.format("%.4f", entry.getValue()), NORMAL_FONT)
            );
            weightCell.setPadding(8);
            weightCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            weightCell.setBackgroundColor(bg);
            table.addCell(weightCell);

            i++;
        }
        doc.add(table);
    }

    private void addFooter(Document doc) throws Exception {
        doc.add(Chunk.NEWLINE);
        Paragraph footer = new Paragraph(
                "Generated by WastewaterRank | FUCOM-HGRA Method | " +
                        java.time.LocalDate.now().toString(),
                new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY)
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);
    }

    private void addTableHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADING_FONT));
        cell.setBackgroundColor(HEADER_COLOR);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8);
        table.addCell(cell);
    }

    private void addInfoRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, BOLD_FONT));
        labelCell.setPadding(6);
        labelCell.setBackgroundColor(ROW_ALT_COLOR);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, NORMAL_FONT));
        valueCell.setPadding(6);
        table.addCell(valueCell);
    }
}
