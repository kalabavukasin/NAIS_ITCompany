package com.itcompany.recruitment.service;

import com.itcompany.recruitment.model.*;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class PdfReportService {

    public byte[] generatePdfReport(Report report) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        try (PdfWriter writer = new PdfWriter(outputStream);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            // Font setup
            PdfFont font = PdfFontFactory.createFont("Helvetica", EmbeddingStrategy.PREFER_EMBEDDED);
            PdfFont boldFont = PdfFontFactory.createFont("Helvetica-Bold", EmbeddingStrategy.PREFER_EMBEDDED);

            // Header
            addHeader(document, report, boldFont);
            
            // Metadata section
            addMetadataSection(document, report, font, boldFont);
            
            // Simple sections
            if (report.getSimpleSections() != null && !report.getSimpleSections().isEmpty()) {
                addSimpleSections(document, report.getSimpleSections(), font, boldFont);
            }
            
            // Complex sections
            if (report.getComplexSections() != null && !report.getComplexSections().isEmpty()) {
                addComplexSections(document, report.getComplexSections(), font, boldFont);
            }
            
            // Footer
            addFooter(document, report, font);
        }
        
        return outputStream.toByteArray();
    }

    private void addHeader(Document document, Report report, PdfFont boldFont) {
        // Title
        Paragraph title = new Paragraph(report.getTitle())
            .setFont(boldFont)
            .setFontSize(20)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(10);
        document.add(title);

        // Description
        if (report.getDescription() != null) {
            Paragraph description = new Paragraph(report.getDescription())
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20)
                .setFontColor(ColorConstants.GRAY);
            document.add(description);
        }

        // Generated info
        Paragraph generatedInfo = new Paragraph(
            String.format("Generisano: %s | Autor: %s", 
                report.getGeneratedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                report.getGeneratedBy()))
            .setFontSize(10)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(30)
            .setFontColor(ColorConstants.DARK_GRAY);
        document.add(generatedInfo);
    }

    private void addMetadataSection(Document document, Report report, PdfFont font, PdfFont boldFont) {
        if (report.getMetadata() == null) return;

        Paragraph sectionTitle = new Paragraph("Metadati izveštaja")
            .setFont(boldFont)
            .setFontSize(14)
            .setMarginTop(20)
            .setMarginBottom(10);
        document.add(sectionTitle);

        Table metadataTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}))
            .setWidth(UnitValue.createPercentValue(100))
            .setMarginBottom(20);

        ReportMetadata metadata = report.getMetadata();
        
        addTableRow(metadataTable, "Verzija izveštaja", metadata.getReportVersion(), font, boldFont);
        addTableRow(metadataTable, "Datum izvlačenja podataka", 
            metadata.getDataExtractedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")), font, boldFont);
        addTableRow(metadataTable, "Izvor podataka", metadata.getDataSource(), font, boldFont);
        addTableRow(metadataTable, "Broj prostih sekcija", String.valueOf(metadata.getTotalSimpleSections()), font, boldFont);
        addTableRow(metadataTable, "Broj složenih sekcija", String.valueOf(metadata.getTotalComplexSections()), font, boldFont);

        document.add(metadataTable);
    }

    private void addSimpleSections(Document document, List<SimpleSection> sections, PdfFont font, PdfFont boldFont) {
        for (SimpleSection section : sections) {
            // Section title
            Paragraph sectionTitle = new Paragraph(section.getTitle())
                .setFont(boldFont)
                .setFontSize(14)
                .setMarginTop(20)
                .setMarginBottom(10);
            document.add(sectionTitle);

            // Section description
            if (section.getDescription() != null) {
                Paragraph description = new Paragraph(section.getDescription())
                    .setFont(font)
                    .setFontSize(10)
                    .setMarginBottom(10)
                    .setFontColor(ColorConstants.GRAY);
                document.add(description);
            }

            // Data table
            if (section.getData() != null && !section.getData().isEmpty()) {
                addDataTable(document, section, font, boldFont);
            }

            // Summary
            Paragraph summary = new Paragraph(
                String.format("Ukupno zapisa: %d | Tip upita: %s", 
                    section.getTotalRecords(), section.getQueryType()))
                .setFont(font)
                .setFontSize(9)
                .setMarginTop(5)
                .setFontColor(ColorConstants.DARK_GRAY);
            document.add(summary);
        }
    }

    private void addComplexSections(Document document, List<ComplexSection> sections, PdfFont font, PdfFont boldFont) {
        for (ComplexSection section : sections) {
            // Section title
            Paragraph sectionTitle = new Paragraph(section.getTitle())
                .setFont(boldFont)
                .setFontSize(14)
                .setMarginTop(20)
                .setMarginBottom(10);
            document.add(sectionTitle);

            // Section description
            if (section.getDescription() != null) {
                Paragraph description = new Paragraph(section.getDescription())
                    .setFont(font)
                    .setFontSize(10)
                    .setMarginBottom(10)
                    .setFontColor(ColorConstants.GRAY);
                document.add(description);
            }

            // Statistics
            if (section.getStatistics() != null && !section.getStatistics().isEmpty()) {
                addStatisticsTable(document, section.getStatistics(), font, boldFont);
            }

            // Detailed data
            if (section.getDetailedData() != null && !section.getDetailedData().isEmpty()) {
                addDetailedDataTable(document, section.getDetailedData(), font, boldFont);
            }

            // Related queries
            if (section.getRelatedQueries() != null && !section.getRelatedQueries().isEmpty()) {
                Paragraph relatedQueries = new Paragraph("Povezani upiti: " + String.join(", ", section.getRelatedQueries()))
                    .setFont(font)
                    .setFontSize(9)
                    .setMarginTop(5)
                    .setFontColor(ColorConstants.DARK_GRAY);
                document.add(relatedQueries);
            }
        }
    }

    private void addDataTable(Document document, SimpleSection section, PdfFont font, PdfFont boldFont) {
        if (section.getData().isEmpty()) return;

        // Get column headers from first data row
        Map<String, Object> firstRow = section.getData().get(0);
        String[] headers = firstRow.keySet().toArray(new String[0]);

        Table table = new Table(UnitValue.createPercentArray(new float[headers.length]))
            .setWidth(UnitValue.createPercentValue(100))
            .setMarginBottom(15);

        // Add header row
        for (String header : headers) {
            Cell headerCell = new Cell()
                .add(new Paragraph(header).setFont(boldFont).setFontSize(10))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER);
            table.addHeaderCell(headerCell);
        }

        // Add data rows (limit to first 20 rows for PDF readability)
        int maxRows = Math.min(section.getData().size(), 20);
        for (int i = 0; i < maxRows; i++) {
            Map<String, Object> row = section.getData().get(i);
            for (String header : headers) {
                Object value = row.get(header);
                String cellValue = value != null ? value.toString() : "";
                Cell dataCell = new Cell()
                    .add(new Paragraph(cellValue).setFont(font).setFontSize(9))
                    .setTextAlignment(TextAlignment.LEFT);
                table.addCell(dataCell);
            }
        }

        document.add(table);

        if (section.getData().size() > 20) {
            Paragraph note = new Paragraph(
                String.format("Prikazano prvih 20 od %d zapisa", section.getData().size()))
                .setFont(font)
                .setFontSize(8)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER);
            document.add(note);
        }
    }

    private void addStatisticsTable(Document document, Map<String, Object> statistics, PdfFont font, PdfFont boldFont) {
        Paragraph statsTitle = new Paragraph("Statistike")
            .setFont(boldFont)
            .setFontSize(12)
            .setMarginTop(10)
            .setMarginBottom(5);
        document.add(statsTitle);

        Table statsTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}))
            .setWidth(UnitValue.createPercentValue(80))
            .setMarginBottom(15);

        for (Map.Entry<String, Object> entry : statistics.entrySet()) {
            addTableRow(statsTable, entry.getKey(), entry.getValue().toString(), font, boldFont);
        }

        document.add(statsTable);
    }

    private void addDetailedDataTable(Document document, List<Map<String, Object>> detailedData, PdfFont font, PdfFont boldFont) {
        if (detailedData.isEmpty()) return;

        Paragraph dataTitle = new Paragraph("Detaljni podaci")
            .setFont(boldFont)
            .setFontSize(12)
            .setMarginTop(10)
            .setMarginBottom(5);
        document.add(dataTitle);

        // Get column headers from first data row
        Map<String, Object> firstRow = detailedData.get(0);
        String[] headers = firstRow.keySet().toArray(new String[0]);

        Table table = new Table(UnitValue.createPercentArray(new float[headers.length]))
            .setWidth(UnitValue.createPercentValue(100))
            .setMarginBottom(15);

        // Add header row
        for (String header : headers) {
            Cell headerCell = new Cell()
                .add(new Paragraph(header).setFont(boldFont).setFontSize(10))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER);
            table.addHeaderCell(headerCell);
        }

        // Add data rows (limit to first 15 rows for PDF readability)
        int maxRows = Math.min(detailedData.size(), 15);
        for (int i = 0; i < maxRows; i++) {
            Map<String, Object> row = detailedData.get(i);
            for (String header : headers) {
                Object value = row.get(header);
                String cellValue = value != null ? value.toString() : "";
                Cell dataCell = new Cell()
                    .add(new Paragraph(cellValue).setFont(font).setFontSize(9))
                    .setTextAlignment(TextAlignment.LEFT);
                table.addCell(dataCell);
            }
        }

        document.add(table);

        if (detailedData.size() > 15) {
            Paragraph note = new Paragraph(
                String.format("Prikazano prvih 15 od %d zapisa", detailedData.size()))
                .setFont(font)
                .setFontSize(8)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER);
            document.add(note);
        }
    }

    private void addTableRow(Table table, String key, String value, PdfFont font, PdfFont boldFont) {
        Cell keyCell = new Cell()
            .add(new Paragraph(key).setFont(boldFont).setFontSize(10))
            .setBackgroundColor(ColorConstants.LIGHT_GRAY);
        table.addCell(keyCell);

        Cell valueCell = new Cell()
            .add(new Paragraph(value).setFont(font).setFontSize(10));
        table.addCell(valueCell);
    }

    private void addFooter(Document document, Report report, PdfFont font) {
        Paragraph footer = new Paragraph(
            String.format("Izveštaj generisan %s | NAIS IT Company Recruitment System", 
                report.getGeneratedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))))
            .setFont(font)
            .setFontSize(8)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(30)
            .setFontColor(ColorConstants.GRAY);
        document.add(footer);
    }
}
