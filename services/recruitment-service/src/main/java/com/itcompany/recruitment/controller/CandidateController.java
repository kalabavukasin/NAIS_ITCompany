package com.itcompany.recruitment.controller;

import com.itcompany.recruitment.model.Candidate;
import com.itcompany.recruitment.dto.CandidateSearchRequest;
import com.itcompany.recruitment.dto.SimpleCandidateSearchRequest;
import com.itcompany.recruitment.service.CandidateService;
import com.itcompany.recruitment.service.TransactionalCandidateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/candidates")
@Tag(name = "Candidates", description = "Candidate management APIs")
public class CandidateController {

    private final CandidateService candidateService;
    private final TransactionalCandidateService transactionalCandidateService;
    
    public CandidateController(CandidateService candidateService, 
                             TransactionalCandidateService transactionalCandidateService) {
        this.candidateService = candidateService;
        this.transactionalCandidateService = transactionalCandidateService;
    }
    
    @PostMapping
    @Operation(summary = "Create new candidate with transactional processing")
    public ResponseEntity<Candidate> createCandidate(@RequestBody Candidate candidate) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(transactionalCandidateService.createCandidate(candidate));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get candidate by ID")
    public ResponseEntity<Candidate> getCandidate(@PathVariable String id) {
        return candidateService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update candidate with transactional processing")
    public ResponseEntity<Candidate> updateCandidate(
            @PathVariable String id,
            @RequestBody Candidate candidate) {
        return ResponseEntity.ok(transactionalCandidateService.updateCandidate(id, candidate));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete candidate with transactional processing")
    public ResponseEntity<Void> deleteCandidate(@PathVariable String id) {
        transactionalCandidateService.deleteCandidate(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping
    @Operation(summary = "Get all candidates")
    public ResponseEntity<List<Candidate>> getAllCandidates() {
        return ResponseEntity.ok(candidateService.findAll());
    }
    
    @PostMapping("/search")
    @Operation(summary = "Simple candidate search with basic filters")
    public ResponseEntity<List<Candidate>> searchCandidates(
            @RequestBody SimpleCandidateSearchRequest request) {
        return ResponseEntity.ok(
            candidateService.simpleSearchCandidates(request));
    }
    
    @PostMapping("/hybrid-search")
    @Operation(summary = "Hybrid search combining text and vector search")
    public ResponseEntity<List<Candidate>> hybridSearch(
            @RequestParam String searchText,
            @RequestParam(required = false) List<String> skills,
            @RequestParam(required = false) String location) {
        return ResponseEntity.ok(
            candidateService.hybridSearch(searchText, skills, location));
    }
    
    @GetMapping("/rank/{jobPostingId}")
    @Operation(summary = "Rank candidates for a specific job posting")
    public ResponseEntity<List<Candidate>> rankCandidates(
            @PathVariable String jobPostingId) {
        return ResponseEntity.ok(candidateService.rankCandidates(jobPostingId));
    }
}
