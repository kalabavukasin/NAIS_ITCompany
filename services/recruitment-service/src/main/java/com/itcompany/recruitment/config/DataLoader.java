package com.itcompany.recruitment.config;

import com.itcompany.recruitment.model.*;
import com.itcompany.recruitment.service.CandidateService;
import com.itcompany.recruitment.service.JobPostingService;
import com.itcompany.recruitment.service.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
@ConditionalOnProperty(name = "data.loader.enabled", havingValue = "true", matchIfMissing = false)
public class DataLoader {

    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);

    private final CandidateService candidateService;
    private final JobPostingService jobPostingService;
    private final ApplicationService applicationService;

    public DataLoader(CandidateService candidateService, 
                     JobPostingService jobPostingService, 
                     ApplicationService applicationService) {
        this.candidateService = candidateService;
        this.jobPostingService = jobPostingService;
        this.applicationService = applicationService;
    }

    @EventListener
    public void handleContextRefresh(ContextRefreshedEvent event) {
        // Dodajemo delay da se baze podataka inicijalizuju
        new Thread(() -> {
            try {
                logger.info("Waiting for databases to initialize...");
                Thread.sleep(10000); // 10 sekundi delay
                
                logger.info("Starting data loading...");
                
                // Kreiraj job postings
                createJobPostings();
                
                // Kreiraj kandidate
                createCandidates();
                
                // Kreiraj prijave
                createApplications();
                
                logger.info("Data loading completed!");
            } catch (Exception e) {
                logger.error("Error during data loading: ", e);
            }
        }).start();
    }

    private void createJobPostings() {
        logger.info("Creating job postings...");
        
        String[] jobTitles = {
            "Senior Java Developer", "Full Stack Developer", "Frontend React Developer",
            "Backend Python Developer", "DevOps Engineer", "Data Scientist",
            "Machine Learning Engineer", "Cloud Architect", "Mobile App Developer",
            "UI/UX Designer", "Product Manager", "Scrum Master",
            "QA Engineer", "System Administrator", "Database Administrator"
        };

        String[] departments = {"Engineering", "Data Science", "Product", "DevOps", "Design"};
        String[] locations = {"Belgrade", "Novi Sad", "Niš", "Remote", "Hybrid"};
        String[] experienceLevels = {"Junior", "Medior", "Senior", "Lead", "Principal"};
        String[] employmentTypes = {"Full-time", "Part-time", "Contract", "Internship"};

        for (int i = 0; i < 50; i++) {
            JobPosting job = new JobPosting();
            job.setId("job_" + (i + 1));
            job.setTitle(jobTitles[i % jobTitles.length] + " " + (i + 1));
            job.setDescription(generateJobDescription(jobTitles[i % jobTitles.length]));
            job.setDepartment(departments[i % departments.length]);
            job.setLocation(locations[i % locations.length]);
            job.setExperienceLevel(experienceLevels[i % experienceLevels.length]);
            job.setMinYearsExperience(i % 5 + 1);
            job.setMaxYearsExperience(i % 5 + 5);
            job.setRequiredSkills(generateSkills());
            job.setPreferredSkills(generateSkills());
            job.setMinSalary(50000.0 + (i * 1000));
            job.setMaxSalary(80000.0 + (i * 1000));
            job.setEmploymentType(employmentTypes[i % employmentTypes.length]);
            job.setIsActive(true);
            job.setPostedDate(LocalDateTime.now().minusDays(i % 30));
            job.setApplicationDeadline(LocalDateTime.now().plusDays(30 + i % 30));
            job.setHrManagerId("HR" + (i % 10 + 1));
            job.setNumberOfPositions(i % 3 + 1);

            jobPostingService.createJobPosting(job);
        }
    }

    private void createCandidates() {
        logger.info("Creating candidates...");
        
        String[] firstNames = {"Marko", "Ana", "Petar", "Jovana", "Stefan", "Milica", "Nikola", "Jelena", "Aleksandar", "Marija"};
        String[] lastNames = {"Petrović", "Jovanović", "Nikolić", "Marković", "Đorđević", "Stojanović", "Ilić", "Milošević", "Radović", "Kostić"};
        String[] locations = {"Belgrade", "Novi Sad", "Niš", "Kragujevac", "Subotica", "Remote"};
        String[] positions = {"Software Developer", "Data Scientist", "DevOps Engineer", "Product Manager", "Designer"};

        for (int i = 0; i < 200; i++) {
            Candidate candidate = new Candidate();
            candidate.setId("candidate_" + (i + 1));
            candidate.setFirstName(firstNames[i % firstNames.length]);
            candidate.setLastName(lastNames[i % lastNames.length]);
            candidate.setEmail("candidate" + i + "@example.com");
            candidate.setPhone("+381" + (600000000 + i));
            candidate.setLocation(locations[i % locations.length]);
            candidate.setCvContent(generateCvContent(firstNames[i % firstNames.length], lastNames[i % lastNames.length]));
            candidate.setSkills(generateSkills());
            candidate.setYearsOfExperience(i % 10 + 1);
            candidate.setWorkExperiences(generateWorkExperiences());
            candidate.setEducationHistory(generateEducationHistory());
            candidate.setCertifications(generateCertifications());
            candidate.setCurrentPosition(positions[i % positions.length]);
            candidate.setExpectedSalary(40000.0 + (i * 500));
            candidate.setPreferredEmploymentType("Full-time");
            candidate.setWillingToRelocate(i % 2 == 0);
            candidate.setDateOfBirth(LocalDate.now().minusYears(25 + i % 20));
            candidate.setLinkedinProfile("https://linkedin.com/in/candidate" + i);
            candidate.setGithubProfile("https://github.com/candidate" + i);
            candidate.setRegistrationDate(LocalDateTime.now().minusDays(i % 365));

            candidateService.createCandidate(candidate);
        }
    }

    private void createApplications() {
        logger.info("Creating applications...");
        
        // Uzmi sve job postings i kandidate
        var jobPostings = jobPostingService.findAll();
        var candidates = candidateService.findAll();
        
        Random random = new Random();
        
        for (int i = 0; i < 300; i++) {
            Application application = new Application();
            application.setId("app_" + (i + 1));
            application.setCandidateId(candidates.get(random.nextInt(candidates.size())).getId());
            application.setJobPostingId(jobPostings.get(random.nextInt(jobPostings.size())).getId());
            application.setCoverLetter(generateCoverLetter());
            application.setStatus(getRandomStatus());
            application.setIsShortlisted(random.nextBoolean());
            application.setHrNotes("Application notes " + i);
            application.setReviewedBy("HR" + (random.nextInt(10) + 1));
            application.setReviewDate(LocalDateTime.now().minusDays(random.nextInt(30)));

            try {
                applicationService.submitApplication(application);
            } catch (Exception e) {
                // Ignoriši duplikate
                logger.debug("Skipping duplicate application: " + e.getMessage());
            }
        }
    }

    private String generateJobDescription(String title) {
        return "We are looking for a " + title + " to join our team. " +
               "The ideal candidate should have strong technical skills, " +
               "experience with modern development practices, and " +
               "excellent communication skills. You will work on " +
               "cutting-edge projects and collaborate with a talented team.";
    }

    private String generateCvContent(String firstName, String lastName) {
        return "Experienced software developer with strong background in " +
               "Java, Spring Boot, and microservices architecture. " +
               "Passionate about clean code, agile methodologies, " +
               "and continuous learning. " + firstName + " " + lastName + " " +
               "has worked on various projects including web applications, " +
               "REST APIs, and database design.";
    }

    private List<String> generateSkills() {
        String[] allSkills = {
            "Java", "Python", "JavaScript", "React", "Angular", "Vue.js",
            "Spring Boot", "Django", "Node.js", "Express", "MySQL", "PostgreSQL",
            "MongoDB", "Redis", "Docker", "Kubernetes", "AWS", "Azure",
            "Git", "Jenkins", "CI/CD", "REST API", "GraphQL", "Microservices"
        };
        
        List<String> skills = new ArrayList<>();
        Random random = new Random();
        int skillCount = random.nextInt(5) + 3; // 3-7 skills
        
        for (int i = 0; i < skillCount; i++) {
            String skill = allSkills[random.nextInt(allSkills.length)];
            if (!skills.contains(skill)) {
                skills.add(skill);
            }
        }
        
        return skills;
    }

    private List<WorkExperience> generateWorkExperiences() {
        List<WorkExperience> experiences = new ArrayList<>();
        Random random = new Random();
        int expCount = random.nextInt(3) + 1; // 1-3 experiences
        
        for (int i = 0; i < expCount; i++) {
            WorkExperience exp = new WorkExperience();
            exp.setCompany("Company " + (i + 1));
            exp.setPosition("Developer " + (i + 1));
            exp.setDescription("Worked on various projects using modern technologies");
            exp.setTechnologies(generateSkills());
            exp.setStartDate(LocalDate.now().minusYears(2 + i));
            exp.setEndDate(i == expCount - 1 ? null : LocalDate.now().minusYears(1 + i));
            exp.setIsCurrent(i == expCount - 1);
            experiences.add(exp);
        }
        
        return experiences;
    }

    private List<Education> generateEducationHistory() {
        List<Education> education = new ArrayList<>();
        
        Education edu = new Education();
        edu.setInstitution("University of Belgrade");
        edu.setDegree("Bachelor of Science");
        edu.setFieldOfStudy("Computer Science");
        edu.setStartDate(LocalDate.of(2015, 9, 1));
        edu.setEndDate(LocalDate.of(2019, 6, 1));
        edu.setGpa(8.5);
        education.add(edu);
        
        return education;
    }

    private List<String> generateCertifications() {
        String[] certs = {
            "AWS Certified Developer", "Oracle Java Certified", "Google Cloud Professional",
            "Microsoft Azure Fundamentals", "Docker Certified Associate"
        };
        
        List<String> certifications = new ArrayList<>();
        Random random = new Random();
        int certCount = random.nextInt(3); // 0-2 certifications
        
        for (int i = 0; i < certCount; i++) {
            certifications.add(certs[random.nextInt(certs.length)]);
        }
        
        return certifications;
    }

    private String generateCoverLetter() {
        return "Dear Hiring Manager,\n\n" +
               "I am writing to express my interest in the position. " +
               "I believe my skills and experience make me a strong candidate. " +
               "I am excited about the opportunity to contribute to your team.\n\n" +
               "Best regards,\nCandidate";
    }

    private String getRandomStatus() {
        String[] statuses = {"PENDING", "REVIEWED", "SHORTLISTED", "REJECTED", "ACCEPTED"};
        return statuses[new Random().nextInt(statuses.length)];
    }
}
