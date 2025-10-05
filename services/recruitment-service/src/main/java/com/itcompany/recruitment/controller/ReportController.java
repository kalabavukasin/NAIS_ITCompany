package com.itcompany.recruitment.controller;

import com.itcompany.recruitment.model.Report;
import com.itcompany.recruitment.service.ReportService;
import com.itcompany.recruitment.service.PdfReportService;
import com.itcompany.recruitment.dto.CandidateSearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private PdfReportService pdfReportService;

    /**
     * Generates comprehensive report about candidates
     */
    @PostMapping("/generate")
    public ResponseEntity<Report> generateReport(@RequestBody Map<String, String> request) {
        try {
            String title = request.getOrDefault("title", "Izveštaj o kandidatima");
            String generatedBy = request.getOrDefault("generatedBy", "System");
            
            Report report = reportService.generateCandidateReport(title, generatedBy);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generates report based on candidate search
     */
    @PostMapping("/generate/search-based")
    public ResponseEntity<Report> generateSearchBasedReport(
            @RequestBody CandidateSearchRequest searchRequest,
            @RequestParam(defaultValue = "System") String generatedBy) {
        try {
            Report report = reportService.generateSearchBasedReport(searchRequest, generatedBy);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generates PDF report
     */
    @PostMapping("/generate/pdf")
    public ResponseEntity<byte[]> generatePdfReport(@RequestBody Map<String, String> request) {
        try {
            String title = request.getOrDefault("title", "Izveštaj o kandidatima");
            String generatedBy = request.getOrDefault("generatedBy", "System");
            
            Report report = reportService.generateCandidateReport(title, generatedBy);
            byte[] pdfBytes = pdfReportService.generatePdfReport(report);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "candidate-report.pdf");
            headers.setContentLength(pdfBytes.length);
            
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generates PDF report based on search
     */
    @PostMapping("/generate/pdf/search-based")
    public ResponseEntity<byte[]> generateSearchBasedPdfReport(
            @RequestBody CandidateSearchRequest searchRequest,
            @RequestParam(defaultValue = "System") String generatedBy) {
        try {
            Report report = reportService.generateSearchBasedReport(searchRequest, generatedBy);
            byte[] pdfBytes = pdfReportService.generatePdfReport(report);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "search-results-report.pdf");
            headers.setContentLength(pdfBytes.length);
            
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Returns information about available report types
     */
    @GetMapping("/types")
    public ResponseEntity<Map<String, Object>> getReportTypes() {
        Map<String, Object> reportTypes = Map.of(
            "simpleSections", Map.of(
                "candidatesByLocation", "Analiza kandidata po lokaciji",
                "candidatesBySkills", "Analiza kandidata po veštinama"
            ),
            "complexSections", Map.of(
                "candidateAnalysis", "Kompleksna analiza kandidata sa statistika",
                "searchAnalysis", "Analiza rezultata pretrage"
            ),
            "supportedFormats", new String[]{"JSON", "PDF"},
            "dataSources", new String[]{"Elasticsearch", "Qdrant Vector Database"}
        );
        
        return ResponseEntity.ok(reportTypes);
    }

    /**
     * Health check endpoint for reports
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Report Generator",
            "version", "1.0"
        ));
    }
}
