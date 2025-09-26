package com.github.sarthakaggarwal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
// Removed import to avoid conflict with Apache POI Cell
// import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
// Note: Using basic iText7 functionality without advanced properties
// import com.itextpdf.layout.property.TextAlignment;
// import com.itextpdf.layout.property.UnitValue;
// Report classes are now standalone
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;

/**
 * Utility class for exporting build reports to PDF and Excel formats.
 */
public class ReportExporter {
    
    private final File outputDirectory;
    private final String projectName;
    
    public ReportExporter(File outputDirectory, String projectName) {
        this.outputDirectory = outputDirectory;
        this.projectName = projectName;
    }
    
    /**
     * Exports build dashboard data to PDF format.
     */
    public File exportDashboardToPdf(BuildMetrics buildMetrics, 
                                   BottleneckReport bottleneckReport,
                                   RegressionReport regressionReport,
                                   EfficiencyScore efficiencyScore) throws IOException {
        File pdfFile = new File(outputDirectory, "build-dashboard.pdf");
        
        try (PdfWriter writer = new PdfWriter(new FileOutputStream(pdfFile));
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {
            
            // Set up fonts
            PdfFont titleFont = PdfFontFactory.createFont();
            PdfFont headerFont = PdfFontFactory.createFont();
            PdfFont normalFont = PdfFontFactory.createFont();
            
            // Title
            document.add(new Paragraph("Build Time Dashboard")
                    .setFont(titleFont)
                    .setFontSize(20)
                    .setBold()
                    // Center alignment removed for compatibility
                    .setMarginBottom(20));
            
            // Project info
            document.add(new Paragraph("Project: " + projectName)
                    .setFont(headerFont)
                    .setFontSize(14)
                    .setMarginBottom(5));
            
            document.add(new Paragraph("Generated: " + 
                    buildMetrics.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .setFont(normalFont)
                    .setFontSize(12)
                    .setMarginBottom(20));
            
            // Build Summary
            document.add(new Paragraph("Build Summary")
                    .setFont(headerFont)
                    .setFontSize(16)
                    .setBold()
                    .setMarginBottom(10));
            
            Table summaryTable = new Table(2)
                    .setWidth(500);
            
            summaryTable.addHeaderCell(createHeaderCell("Metric", headerFont));
            summaryTable.addHeaderCell(createHeaderCell("Value", headerFont));
            
            summaryTable.addCell(createCell("Total Build Time", normalFont));
            summaryTable.addCell(createCell(String.format("%.1fs", buildMetrics.totalTime / 1000.0), normalFont));
            
            summaryTable.addCell(createCell("Phases Executed", normalFont));
            summaryTable.addCell(createCell(String.valueOf(buildMetrics.phaseTimes.size() - 1), normalFont));
            
            summaryTable.addCell(createCell("Peak Memory Usage", normalFont));
            summaryTable.addCell(createCell(String.format("%.1f MB", 
                    buildMetrics.systemMetrics.peakMemoryUsage / (1024.0 * 1024)), normalFont));
            
            summaryTable.addCell(createCell("Average CPU Usage", normalFont));
            summaryTable.addCell(createCell(String.format("%.1f%%", 
                    buildMetrics.systemMetrics.avgCpuUsage * 100), normalFont));
            
            document.add(summaryTable);
            document.add(new Paragraph("\n"));
            
            // Phase Breakdown
            document.add(new Paragraph("Phase Breakdown")
                    .setFont(headerFont)
                    .setFontSize(16)
                    .setBold()
                    .setMarginBottom(10));
            
            Table phaseTable = new Table(4)
                    .setWidth(500);
            
            phaseTable.addHeaderCell(createHeaderCell("Phase", headerFont));
            phaseTable.addHeaderCell(createHeaderCell("Duration (ms)", headerFont));
            phaseTable.addHeaderCell(createHeaderCell("Duration (s)", headerFont));
            phaseTable.addHeaderCell(createHeaderCell("% of Total", headerFont));
            
            long totalTime = buildMetrics.phaseTimes.values().stream()
                    .filter(time -> time != null)
                    .mapToLong(Long::longValue)
                    .sum();
            
            List<Map.Entry<String, Long>> sortedPhases = buildMetrics.phaseTimes.entrySet().stream()
                    .filter(e -> !e.getKey().equals("total"))
                    .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                    .collect(Collectors.toList());
            
            for (Map.Entry<String, Long> entry : sortedPhases) {
                String phase = entry.getKey();
                long duration = entry.getValue();
                double seconds = duration / 1000.0;
                double percentage = totalTime > 0 ? (duration * 100.0) / totalTime : 0;
                
                phaseTable.addCell(createCell(phase, normalFont));
                phaseTable.addCell(createCell(String.valueOf(duration), normalFont));
                phaseTable.addCell(createCell(String.format("%.1f", seconds), normalFont));
                phaseTable.addCell(createCell(String.format("%.1f%%", percentage), normalFont));
            }
            
            document.add(phaseTable);
            document.add(new Paragraph("\n"));
            
            // Analytics Section
            addAnalyticsSection(document, bottleneckReport, regressionReport, efficiencyScore, 
                              headerFont, normalFont);
        }
        
        return pdfFile;
    }
    
    /**
     * Exports build dashboard data to Excel format.
     */
    public File exportDashboardToExcel(BuildMetrics buildMetrics,
                                     BottleneckReport bottleneckReport,
                                     RegressionReport regressionReport,
                                     EfficiencyScore efficiencyScore) throws IOException {
        File excelFile = new File(outputDirectory, "build-dashboard.xlsx");
        
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fileOut = new FileOutputStream(excelFile)) {
            
            // Create styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            
            // Summary Sheet
            Sheet summarySheet = workbook.createSheet("Build Summary");
            createSummarySheet(summarySheet, buildMetrics, headerStyle, dataStyle);
            
            // Phase Details Sheet
            Sheet phaseSheet = workbook.createSheet("Phase Details");
            createPhaseDetailsSheet(phaseSheet, buildMetrics, headerStyle, dataStyle);
            
            // Analytics Sheet
            Sheet analyticsSheet = workbook.createSheet("Analytics");
            createAnalyticsSheet(analyticsSheet, bottleneckReport, regressionReport, 
                               efficiencyScore, headerStyle, dataStyle);
            
            workbook.write(fileOut);
        }
        
        return excelFile;
    }
    
    /**
     * Exports warnings report to PDF format.
     */
    public File exportWarningsToPdf(List<BuildWarning> warnings) throws IOException {
        File pdfFile = new File(outputDirectory, "build-warnings.pdf");
        
        try (PdfWriter writer = new PdfWriter(new FileOutputStream(pdfFile));
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {
            
            PdfFont titleFont = PdfFontFactory.createFont();
            PdfFont headerFont = PdfFontFactory.createFont();
            PdfFont normalFont = PdfFontFactory.createFont();
            
            // Title
            document.add(new Paragraph("Build Warnings Report")
                    .setFont(titleFont)
                    .setFontSize(20)
                    .setBold()
                    // Center alignment removed for compatibility
                    .setMarginBottom(20));
            
            document.add(new Paragraph("Total Warnings: " + warnings.size())
                    .setFont(headerFont)
                    .setFontSize(14)
                    .setMarginBottom(20));
            
            // Group warnings by type
            Map<String, List<BuildWarning>> warningsByType = warnings.stream()
                    .collect(Collectors.groupingBy(w -> w.warningType));
            
            for (Map.Entry<String, List<BuildWarning>> entry : warningsByType.entrySet()) {
                String type = entry.getKey();
                List<BuildWarning> typeWarnings = entry.getValue();
                
                document.add(new Paragraph(type + " (" + typeWarnings.size() + ")")
                        .setFont(headerFont)
                        .setFontSize(16)
                        .setBold()
                        .setMarginBottom(10));
                
                Table warningTable = new Table(4)
                        .setWidth(500);
                
                warningTable.addHeaderCell(createHeaderCell("Message", headerFont));
                warningTable.addHeaderCell(createHeaderCell("File", headerFont));
                warningTable.addHeaderCell(createHeaderCell("Line", headerFont));
                warningTable.addHeaderCell(createHeaderCell("Severity", headerFont));
                
                for (BuildWarning warning : typeWarnings) {
                    warningTable.addCell(createCell(warning.warningMessage, normalFont));
                    warningTable.addCell(createCell(warning.fileName, normalFont));
                    warningTable.addCell(createCell(String.valueOf(warning.lineNumber), normalFont));
                    warningTable.addCell(createCell(warning.severity, normalFont));
                }
                
                document.add(warningTable);
                document.add(new Paragraph("\n"));
            }
        }
        
        return pdfFile;
    }
    
    /**
     * Exports warnings report to Excel format.
     */
    public File exportWarningsToExcel(List<BuildWarning> warnings) throws IOException {
        File excelFile = new File(outputDirectory, "build-warnings.xlsx");
        
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fileOut = new FileOutputStream(excelFile)) {
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            
            Sheet warningsSheet = workbook.createSheet("Warnings");
            
            // Header row
            Row headerRow = warningsSheet.createRow(0);
            headerRow.createCell(0).setCellValue("Type");
            headerRow.createCell(1).setCellValue("Message");
            headerRow.createCell(2).setCellValue("File");
            headerRow.createCell(3).setCellValue("Line");
            headerRow.createCell(4).setCellValue("Severity");
            headerRow.createCell(5).setCellValue("Phase");
            
            for (Cell cell : headerRow) {
                cell.setCellStyle(headerStyle);
            }
            
            // Data rows
            int rowNum = 1;
            for (BuildWarning warning : warnings) {
                Row row = warningsSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(warning.warningType);
                row.createCell(1).setCellValue(warning.warningMessage);
                row.createCell(2).setCellValue(warning.fileName);
                row.createCell(3).setCellValue(warning.lineNumber);
                row.createCell(4).setCellValue(warning.severity);
                row.createCell(5).setCellValue(warning.phase);
                
                for (Cell cell : row) {
                    cell.setCellStyle(dataStyle);
                }
            }
            
            // Auto-size columns
            for (int i = 0; i < 6; i++) {
                warningsSheet.autoSizeColumn(i);
            }
            
            workbook.write(fileOut);
        }
        
        return excelFile;
    }
    
    /**
     * Exports failures report to PDF format.
     */
    public File exportFailuresToPdf(List<BuildFailure> failures) throws IOException {
        File pdfFile = new File(outputDirectory, "build-failures.pdf");
        
        try (PdfWriter writer = new PdfWriter(new FileOutputStream(pdfFile));
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {
            
            PdfFont titleFont = PdfFontFactory.createFont();
            PdfFont headerFont = PdfFontFactory.createFont();
            PdfFont normalFont = PdfFontFactory.createFont();
            
            // Title
            document.add(new Paragraph("Build Failures Report")
                    .setFont(titleFont)
                    .setFontSize(20)
                    .setBold()
                    // Center alignment removed for compatibility
                    .setMarginBottom(20));
            
            document.add(new Paragraph("Total Failures: " + failures.size())
                    .setFont(headerFont)
                    .setFontSize(14)
                    .setMarginBottom(20));
            
            for (BuildFailure failure : failures) {
                document.add(new Paragraph(failure.errorType)
                        .setFont(headerFont)
                        .setFontSize(16)
                        .setBold()
                        .setMarginBottom(10));
                
                document.add(new Paragraph("Phase: " + failure.phase)
                        .setFont(normalFont)
                        .setFontSize(12)
                        .setMarginBottom(5));
                
                document.add(new Paragraph("Message: " + failure.errorMessage)
                        .setFont(normalFont)
                        .setFontSize(12)
                        .setMarginBottom(5));
                
                if (!failure.fileName.isEmpty()) {
                    document.add(new Paragraph("File: " + failure.fileName + " (line " + failure.lineNumber + ")")
                            .setFont(normalFont)
                            .setFontSize(12)
                            .setMarginBottom(5));
                }
                
                if (!failure.sourceCodeSnippet.isEmpty()) {
                    document.add(new Paragraph("Source Code:")
                            .setFont(normalFont)
                            .setFontSize(12)
                            .setBold()
                            .setMarginBottom(5));
                    
                    document.add(new Paragraph(failure.sourceCodeSnippet)
                            .setFont(normalFont)
                            .setFontSize(10)
                            .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                            .setMarginBottom(10));
                }
                
                document.add(new Paragraph("\n"));
            }
        }
        
        return pdfFile;
    }
    
    /**
     * Exports failures report to Excel format.
     */
    public File exportFailuresToExcel(List<BuildFailure> failures) throws IOException {
        File excelFile = new File(outputDirectory, "build-failures.xlsx");
        
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fileOut = new FileOutputStream(excelFile)) {
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            
            Sheet failuresSheet = workbook.createSheet("Failures");
            
            // Header row
            Row headerRow = failuresSheet.createRow(0);
            headerRow.createCell(0).setCellValue("Error Type");
            headerRow.createCell(1).setCellValue("Phase");
            headerRow.createCell(2).setCellValue("Message");
            headerRow.createCell(3).setCellValue("File");
            headerRow.createCell(4).setCellValue("Line");
            headerRow.createCell(5).setCellValue("Source Code Snippet");
            
            for (Cell cell : headerRow) {
                cell.setCellStyle(headerStyle);
            }
            
            // Data rows
            int rowNum = 1;
            for (BuildFailure failure : failures) {
                Row row = failuresSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(failure.errorType);
                row.createCell(1).setCellValue(failure.phase);
                row.createCell(2).setCellValue(failure.errorMessage);
                row.createCell(3).setCellValue(failure.fileName);
                row.createCell(4).setCellValue(failure.lineNumber);
                row.createCell(5).setCellValue(failure.sourceCodeSnippet);
                
                for (Cell cell : row) {
                    cell.setCellStyle(dataStyle);
                }
            }
            
            // Auto-size columns
            for (int i = 0; i < 6; i++) {
                failuresSheet.autoSizeColumn(i);
            }
            
            workbook.write(fileOut);
        }
        
        return excelFile;
    }
    
    // Helper methods for PDF generation
    private com.itextpdf.layout.element.Cell createHeaderCell(String text, PdfFont font) {
        return new com.itextpdf.layout.element.Cell().add(new Paragraph(text))
                .setFont(font)
                .setBold()
                .setBackgroundColor(ColorConstants.LIGHT_GRAY);
    }
    
    private com.itextpdf.layout.element.Cell createCell(String text, PdfFont font) {
        return new com.itextpdf.layout.element.Cell().add(new Paragraph(text))
                .setFont(font);
    }
    
    private void addAnalyticsSection(Document document, BottleneckReport bottleneckReport,
                                   RegressionReport regressionReport, EfficiencyScore efficiencyScore,
                                   PdfFont headerFont, PdfFont normalFont) {
        
        // Bottleneck Analysis
        document.add(new Paragraph("Bottleneck Analysis")
                .setFont(headerFont)
                .setFontSize(16)
                .setBold()
                .setMarginBottom(10));
        
        if (!bottleneckReport.primaryBottleneck.isEmpty()) {
            document.add(new Paragraph("Primary Bottleneck: " + bottleneckReport.primaryBottleneck)
                    .setFont(normalFont)
                    .setFontSize(12)
                    .setMarginBottom(5));
            
            document.add(new Paragraph(String.format("Time Impact: %.1fs (%.1f%%)",
                    bottleneckReport.primaryBottleneckTime / 1000.0,
                    bottleneckReport.primaryBottleneckPercentage))
                    .setFont(normalFont)
                    .setFontSize(12)
                    .setMarginBottom(10));
        }
        
        // Performance Analysis
        document.add(new Paragraph("Performance Analysis")
                .setFont(headerFont)
                .setFontSize(16)
                .setBold()
                .setMarginBottom(10));
        
        String performanceStatus;
        if (regressionReport.isRegression) {
            performanceStatus = String.format("REGRESSION DETECTED (%.1fx slower)",
                    regressionReport.regressionFactor);
        } else if (regressionReport.isImprovement) {
            performanceStatus = String.format("PERFORMANCE IMPROVED (%.1fx faster)",
                    1.0 / regressionReport.regressionFactor);
        } else {
            performanceStatus = "Performance is stable";
        }
        
        document.add(new Paragraph(performanceStatus)
                .setFont(normalFont)
                .setFontSize(12)
                .setMarginBottom(10));
        
        // Efficiency Score
        document.add(new Paragraph("Efficiency Score")
                .setFont(headerFont)
                .setFontSize(16)
                .setBold()
                .setMarginBottom(10));
        
        document.add(new Paragraph(String.format("Overall Score: %.1f/100 (%s)",
                efficiencyScore.score, efficiencyScore.getLetterGrade()))
                .setFont(normalFont)
                .setFontSize(12)
                .setMarginBottom(10));
    }
    
    // Helper methods for Excel generation
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }
    
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }
    
    private void createSummarySheet(Sheet sheet, BuildMetrics buildMetrics, 
                                  CellStyle headerStyle, CellStyle dataStyle) {
        // Header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Metric");
        headerRow.createCell(1).setCellValue("Value");
        
        for (Cell cell : headerRow) {
            cell.setCellStyle(headerStyle);
        }
        
        // Data rows
        int rowNum = 1;
        
        Row row1 = sheet.createRow(rowNum++);
        row1.createCell(0).setCellValue("Total Build Time");
        row1.createCell(1).setCellValue(String.format("%.1fs", buildMetrics.totalTime / 1000.0));
        
        Row row2 = sheet.createRow(rowNum++);
        row2.createCell(0).setCellValue("Phases Executed");
        row2.createCell(1).setCellValue(buildMetrics.phaseTimes.size() - 1);
        
        Row row3 = sheet.createRow(rowNum++);
        row3.createCell(0).setCellValue("Peak Memory Usage (MB)");
        row3.createCell(1).setCellValue(String.format("%.1f", 
                buildMetrics.systemMetrics.peakMemoryUsage / (1024.0 * 1024)));
        
        Row row4 = sheet.createRow(rowNum++);
        row4.createCell(0).setCellValue("Average CPU Usage (%)");
        row4.createCell(1).setCellValue(String.format("%.1f", 
                buildMetrics.systemMetrics.avgCpuUsage * 100));
        
        // Apply data style to all data rows
        for (int i = 1; i < rowNum; i++) {
            Row row = sheet.getRow(i);
            for (Cell cell : row) {
                cell.setCellStyle(dataStyle);
            }
        }
        
        // Auto-size columns
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }
    
    private void createPhaseDetailsSheet(Sheet sheet, BuildMetrics buildMetrics,
                                       CellStyle headerStyle, CellStyle dataStyle) {
        // Header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Phase");
        headerRow.createCell(1).setCellValue("Duration (ms)");
        headerRow.createCell(2).setCellValue("Duration (s)");
        headerRow.createCell(3).setCellValue("% of Total");
        
        for (org.apache.poi.ss.usermodel.Cell cell : headerRow) {
            cell.setCellStyle(headerStyle);
        }
        
        // Calculate total time
        long totalTime = buildMetrics.phaseTimes.values().stream()
                .filter(time -> time != null)
                .mapToLong(Long::longValue)
                .sum();
        
        // Sort phases by duration
        List<Map.Entry<String, Long>> sortedPhases = buildMetrics.phaseTimes.entrySet().stream()
                .filter(e -> !e.getKey().equals("total"))
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());
        
        // Data rows
        int rowNum = 1;
        for (Map.Entry<String, Long> entry : sortedPhases) {
            String phase = entry.getKey();
            long duration = entry.getValue();
            double seconds = duration / 1000.0;
            double percentage = totalTime > 0 ? (duration * 100.0) / totalTime : 0;
            
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(phase);
            row.createCell(1).setCellValue(duration);
            row.createCell(2).setCellValue(seconds);
            row.createCell(3).setCellValue(percentage);
            
            for (Cell cell : row) {
                cell.setCellStyle(dataStyle);
            }
        }
        
        // Auto-size columns
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void createAnalyticsSheet(Sheet sheet, BottleneckReport bottleneckReport,
                                    RegressionReport regressionReport, EfficiencyScore efficiencyScore,
                                    CellStyle headerStyle, CellStyle dataStyle) {
        int rowNum = 0;
        
        // Bottleneck Analysis section
        Row bottleneckHeader = sheet.createRow(rowNum++);
        bottleneckHeader.createCell(0).setCellValue("Bottleneck Analysis");
        bottleneckHeader.getCell(0).setCellStyle(headerStyle);
        
        if (!bottleneckReport.primaryBottleneck.isEmpty()) {
            Row row1 = sheet.createRow(rowNum++);
            row1.createCell(0).setCellValue("Primary Bottleneck");
            row1.createCell(1).setCellValue(bottleneckReport.primaryBottleneck);
            
            Row row2 = sheet.createRow(rowNum++);
            row2.createCell(0).setCellValue("Time Impact (s)");
            row2.createCell(1).setCellValue(bottleneckReport.primaryBottleneckTime / 1000.0);
            
            Row row3 = sheet.createRow(rowNum++);
            row3.createCell(0).setCellValue("Percentage of Total");
            row3.createCell(1).setCellValue(bottleneckReport.primaryBottleneckPercentage);
        }
        
        rowNum++; // Empty row
        
        // Performance Analysis section
        Row perfHeader = sheet.createRow(rowNum++);
        perfHeader.createCell(0).setCellValue("Performance Analysis");
        perfHeader.getCell(0).setCellStyle(headerStyle);
        
        Row perfStatus = sheet.createRow(rowNum++);
        perfStatus.createCell(0).setCellValue("Status");
        if (regressionReport.isRegression) {
            perfStatus.createCell(1).setCellValue("REGRESSION DETECTED");
        } else if (regressionReport.isImprovement) {
            perfStatus.createCell(1).setCellValue("PERFORMANCE IMPROVED");
        } else {
            perfStatus.createCell(1).setCellValue("STABLE");
        }
        
        rowNum++; // Empty row
        
        // Efficiency Score section
        Row effHeader = sheet.createRow(rowNum++);
        effHeader.createCell(0).setCellValue("Efficiency Score");
        effHeader.getCell(0).setCellStyle(headerStyle);
        
        Row effScore = sheet.createRow(rowNum++);
        effScore.createCell(0).setCellValue("Overall Score");
        effScore.createCell(1).setCellValue(String.format("%.1f/100 (%s)", 
                efficiencyScore.score, efficiencyScore.getLetterGrade()));
        
        // Apply data style to all data cells
        for (int i = 1; i < rowNum; i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                for (Cell cell : row) {
                    if (cell.getCellStyle() != headerStyle) {
                        cell.setCellStyle(dataStyle);
                    }
                }
            }
        }
        
        // Auto-size columns
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }
}
