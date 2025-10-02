package com.itcompany.recruitment.controller;

import com.itcompany.recruitment.model.JobPosting;
import com.itcompany.recruitment.service.JobPostingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/job-postings")
@Tag(name = "Job Postings", description = "Job posting management APIs")
public class JobPostingController {
    
    private final JobPostingService jobPostingService;

    public JobPostingController(JobPostingService jobPostingService) {
        this.jobPostingService = jobPostingService;
    }
    
    @PostMapping
    @Operation(summary = "Create new job posting")
    public ResponseEntity<JobPosting> createJobPosting(@RequestBody JobPosting jobPosting) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(jobPostingService.createJobPosting(jobPosting));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get job posting by ID")
    public ResponseEntity<JobPosting> getJobPosting(@PathVariable String id) {
        return jobPostingService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update job posting")
    public ResponseEntity<JobPosting> updateJobPosting(
            @PathVariable String id,
            @RequestBody JobPosting jobPosting) {
        return ResponseEntity.ok(jobPostingService.updateJobPosting(id, jobPosting));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete job posting")
    public ResponseEntity<Void> deleteJobPosting(@PathVariable String id) {
        jobPostingService.deleteJobPosting(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping
    @Operation(summary = "Get all job postings")
    public ResponseEntity<List<JobPosting>> getAllJobPostings() {
        return ResponseEntity.ok(jobPostingService.findAll());
    }
    
    @GetMapping("/active")
    @Operation(summary = "Get active job postings")
    public ResponseEntity<List<JobPosting>> getActiveJobPostings() {
        return ResponseEntity.ok(jobPostingService.findActivePostings());
    }
    
    @GetMapping("/department/{department}")
    @Operation(summary = "Get job postings by department")
    public ResponseEntity<List<JobPosting>> getByDepartment(@PathVariable String department) {
        return ResponseEntity.ok(jobPostingService.findByDepartment(department));
    }
    
}
