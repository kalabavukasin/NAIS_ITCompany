package com.itcompany.recruitment.controller;

import com.itcompany.recruitment.model.Application;
import com.itcompany.recruitment.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
@Tag(name = "Applications", description = "Job application management APIs")
public class ApplicationController {
    
    private final ApplicationService applicationService;
    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }
    
    @PostMapping
    @Operation(summary = "Submit new application")
    public ResponseEntity<Application> submitApplication(@RequestBody Application application) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(applicationService.submitApplication(application));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get application by ID")
    public ResponseEntity<Application> getApplication(@PathVariable String id) {
        return applicationService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{id}/status")
    @Operation(summary = "Update application status")
    public ResponseEntity<Application> updateStatus(
            @PathVariable String id,
            @RequestParam String status,
            @RequestParam(required = false) String hrNotes) {
        return ResponseEntity.ok(applicationService.updateApplicationStatus(id, status, hrNotes));
    }
    
    @GetMapping("/job/{jobPostingId}")
    @Operation(summary = "Get applications for a job posting")
    public ResponseEntity<List<Application>> getApplicationsByJob(@PathVariable String jobPostingId) {
        return ResponseEntity.ok(applicationService.findByJobPostingId(jobPostingId));
    }
    
    @GetMapping("/candidate/{candidateId}")
    @Operation(summary = "Get applications by candidate")
    public ResponseEntity<List<Application>> getApplicationsByCandidate(@PathVariable String candidateId) {
        return ResponseEntity.ok(applicationService.findByCandidateId(candidateId));
    }
    
    @PostMapping("/shortlist/{jobPostingId}")
    @Operation(summary = "Generate shortlist for a job posting")
    public ResponseEntity<List<Application>> generateShortlist(
            @PathVariable String jobPostingId,
            @RequestParam(defaultValue = "10") int topN) {
        return ResponseEntity.ok(applicationService.generateShortlist(jobPostingId, topN));
    }
    
    @GetMapping
    @Operation(summary = "Get all applications")
    public ResponseEntity<List<Application>> getAllApplications() {
        return ResponseEntity.ok(applicationService.findAll());
    }
    
    @GetMapping("/statistics/{jobPostingId}")
    @Operation(summary = "Get application statistics for a job posting")
    public ResponseEntity<Map<String, Object>> getStatistics(@PathVariable String jobPostingId) {
        return ResponseEntity.ok(applicationService.getApplicationStatistics(jobPostingId));
    }
    
}
