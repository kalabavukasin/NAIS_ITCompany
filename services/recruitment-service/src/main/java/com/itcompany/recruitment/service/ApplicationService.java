package com.itcompany.recruitment.service;

import com.itcompany.recruitment.model.Application;
import com.itcompany.recruitment.model.Candidate;
import com.itcompany.recruitment.model.JobPosting;
import com.itcompany.recruitment.repository.ApplicationRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ApplicationService {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationService.class);
    private final ApplicationRepository applicationRepository;
    private final CandidateService candidateService;
    private final JobPostingService jobPostingService;
    private final VectorizationService vectorizationService;
    public ApplicationService(ApplicationRepository applicationRepository,
                             CandidateService candidateService,
                             JobPostingService jobPostingService,
                             VectorizationService vectorizationService) {
        this.applicationRepository = applicationRepository;
        this.candidateService = candidateService;
        this.jobPostingService = jobPostingService;
        this.vectorizationService = vectorizationService;
    }
    
    public Application submitApplication(Application application) {
        // Proveri da li kandidat već ima prijavu za ovaj posao
        Application existing = applicationRepository.findByCandidateIdAndJobPostingId(
            application.getCandidateId(), 
            application.getJobPostingId()
        );
        
        if (existing != null) {
            throw new IllegalStateException("Candidate already applied for this position");
        }
        
        // Vektorizuj cover letter ako postoji
        if (application.getCoverLetter() != null) {
            application.setCoverLetterVector(
                vectorizationService.vectorizeText(application.getCoverLetter())
            );
        }
        
        // Izračunaj match skorove
        calculateMatchScores(application);
        
        application.setApplicationDate(LocalDateTime.now());
        application.setStatus("PENDING");
        
        return applicationRepository.save(application);
    }
    
    private void calculateMatchScores(Application application) {
        Optional<Candidate> candidateOpt = candidateService.findById(application.getCandidateId());
        Optional<JobPosting> jobOpt = jobPostingService.findById(application.getJobPostingId());
        
        if (candidateOpt.isPresent() && jobOpt.isPresent()) {
            Candidate candidate = candidateOpt.get();
            JobPosting job = jobOpt.get();
            
            // CV Match Score (vektorska sličnost)
            if (candidate.getCvVector() != null && job.getDescriptionVector() != null) {
                double cvScore = vectorizationService.calculateCosineSimilarity(
                    candidate.getCvVector(), 
                    job.getDescriptionVector()
                );
                application.setCvMatchScore(cvScore);
            }
            
            // Skill Match Score
            if (candidate.getSkills() != null && job.getRequiredSkills() != null) {
                Set<String> candidateSkills = new HashSet<>(candidate.getSkills());
                Set<String> requiredSkills = new HashSet<>(job.getRequiredSkills());
                
                long matchedSkills = candidateSkills.stream()
                    .filter(requiredSkills::contains)
                    .count();
                
                double skillScore = requiredSkills.isEmpty() ? 1.0 : 
                    (double) matchedSkills / requiredSkills.size();
                application.setSkillMatchScore(skillScore);
            }
            
            // Experience Match Score
            if (candidate.getYearsOfExperience() != null && job.getMinYearsExperience() != null) {
                if (candidate.getYearsOfExperience() >= job.getMinYearsExperience()) {
                    application.setExperienceMatchScore(1.0);
                } else {
                    double expScore = (double) candidate.getYearsOfExperience() / 
                                     job.getMinYearsExperience();
                    application.setExperienceMatchScore(Math.min(expScore, 1.0));
                }
            }
            
            // Overall Match Score (weighted average)
            double overallScore = 0.0;
            double weights = 0.0;
            
            if (application.getCvMatchScore() != null) {
                overallScore += application.getCvMatchScore() * 0.4;
                weights += 0.4;
            }
            if (application.getSkillMatchScore() != null) {
                overallScore += application.getSkillMatchScore() * 0.35;
                weights += 0.35;
            }
            if (application.getExperienceMatchScore() != null) {
                overallScore += application.getExperienceMatchScore() * 0.25;
                weights += 0.25;
            }
            
            if (weights > 0) {
                application.setOverallMatchScore(overallScore / weights);
            }
        }
    }
    
    public Application updateApplicationStatus(String id, String status, String hrNotes) {
        Optional<Application> appOpt = applicationRepository.findById(id);
        if (appOpt.isEmpty()) {
            throw new IllegalArgumentException("Application not found");
        }
        
        Application application = appOpt.get();
        application.setStatus(status);
        if (hrNotes != null) {
            application.setHrNotes(hrNotes);
        }
        application.setReviewDate(LocalDateTime.now());
        
        if ("SHORTLISTED".equals(status)) {
            application.setIsShortlisted(true);
        }
        
        return applicationRepository.save(application);
    }
    
    public List<Application> generateShortlist(String jobPostingId, int topN) {
        List<Application> applications = applicationRepository.findByJobPostingId(jobPostingId);
        
        // Sortiraj po overall match score
        applications.sort((a, b) -> {
            Double scoreA = a.getOverallMatchScore() != null ? a.getOverallMatchScore() : 0.0;
            Double scoreB = b.getOverallMatchScore() != null ? b.getOverallMatchScore() : 0.0;
            return scoreB.compareTo(scoreA);
        });
        
        // Uzmi top N i označi kao shortlisted
        List<Application> shortlist = applications.stream()
            .limit(topN)
            .collect(Collectors.toList());
        
        for (int i = 0; i < shortlist.size(); i++) {
            Application app = shortlist.get(i);
            app.setIsShortlisted(true);
            app.setRanking(i + 1);
            app.setStatus("SHORTLISTED");
            applicationRepository.save(app);
        }
        
        return shortlist;
    }
    
    public Map<String, Object> getApplicationStatistics(String jobPostingId) {
        Map<String, Object> stats = new HashMap<>();
        
        Long totalApplications = applicationRepository.countByJobPostingId(jobPostingId);
        Long shortlisted = applicationRepository.countByJobPostingIdAndStatus(jobPostingId, "SHORTLISTED");
        Long rejected = applicationRepository.countByJobPostingIdAndStatus(jobPostingId, "REJECTED");
        Long pending = applicationRepository.countByJobPostingIdAndStatus(jobPostingId, "PENDING");
        
        List<Application> applications = applicationRepository.findByJobPostingId(jobPostingId);
        
        // Prosečni skorovi
        double avgOverallScore = applications.stream()
            .filter(a -> a.getOverallMatchScore() != null)
            .mapToDouble(Application::getOverallMatchScore)
            .average()
            .orElse(0.0);
        
        double avgSkillScore = applications.stream()
            .filter(a -> a.getSkillMatchScore() != null)
            .mapToDouble(Application::getSkillMatchScore)
            .average()
            .orElse(0.0);
        
        stats.put("totalApplications", totalApplications);
        stats.put("shortlisted", shortlisted);
        stats.put("rejected", rejected);
        stats.put("pending", pending);
        stats.put("averageOverallScore", avgOverallScore);
        stats.put("averageSkillScore", avgSkillScore);
        
        return stats;
    }
    public Optional<Application> findById(String id) {
        return applicationRepository.findById(id);
    }
    public List<Application> findByJobPostingId(String jobPostingId) {
        return applicationRepository.findByJobPostingId(jobPostingId);
    }
    public List<Application> findByCandidateId(String candidateId) {
        return applicationRepository.findByCandidateId(candidateId);
    }
    
    public List<Application> findAll() {
        return (List<Application>) applicationRepository.findAll();
    }
    
}
