package com.itcompany.recruitment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TestDataService {

    private static final Logger logger = LoggerFactory.getLogger(TestDataService.class);

    @Autowired
    @Qualifier("qdrantRestTemplate")
    private RestTemplate qdrantRestTemplate;

    @Autowired
    @Qualifier("elasticsearchRestTemplate")
    private RestTemplate elasticsearchRestTemplate;

    @Autowired
    private VectorizationService vectorizationService;

    @Value("${qdrant.url}")
    private String qdrantUrl;

    @Value("${elasticsearch.url}")
    private String elasticsearchUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void loadTestData() {
        logger.info("Loading test data...");
        
        try {
            loadCandidatesData();
            loadJobAdvertisementsData();
            loadApplicationsData();
            logger.info("Test data loaded successfully!");
        } catch (Exception e) {
            logger.error("Error loading test data: {}", e.getMessage(), e);
        }
    }

    private void loadCandidatesData() {
        logger.info("Loading candidates test data...");
        
        List<Map<String, Object>> candidates = createCandidatesData();
        List<Map<String, Object>> points = new ArrayList<>();
        
        for (int i = 0; i < candidates.size(); i++) {
            Map<String, Object> candidate = candidates.get(i);
            
            // Create point for Qdrant with real vectorization
            Map<String, Object> point = new HashMap<>();
            point.put("id", i + 1);
            
            // Vectorize CV content instead of random vector
            String cvContent = (String) candidate.get("cv_content");
            float[] cvVector = vectorizationService.vectorizeText(cvContent);
            point.put("vector", cvVector);
            point.put("payload", candidate);
            points.add(point);
            
            // Add to Elasticsearch with String ID
            String candidateId = "candidate_" + (i + 1);
            candidate.put("id", candidateId); // Add ID to the candidate data
            addToElasticsearch("candidates", candidateId, candidate);
        }
        
        // Batch add to Qdrant
        addBatchToQdrant("candidates", points);
        
        logger.info("Loaded {} candidates", candidates.size());
    }

    private void loadJobAdvertisementsData() {
        logger.info("Loading job advertisements test data...");
        
        List<Map<String, Object>> jobs = createJobAdvertisementsData();
        List<Map<String, Object>> points = new ArrayList<>();
        
        for (int i = 0; i < jobs.size(); i++) {
            Map<String, Object> job = jobs.get(i);
            
            // Create point for Qdrant with real vectorization
            Map<String, Object> point = new HashMap<>();
            point.put("id", i + 1);
            
            // Vectorize job description instead of random vector
            String description = (String) job.get("description");
            float[] descriptionVector = vectorizationService.vectorizeText(description);
            point.put("vector", descriptionVector);
            point.put("payload", job);
            points.add(point);
            
            // Add to Elasticsearch with String ID
            String jobId = "job_" + (i + 1);
            job.put("id", jobId); // Add ID to the job data
            addToElasticsearch("job_advertisements", jobId, job);
        }
        
        // Batch add to Qdrant
        addBatchToQdrant("job_advertisements", points);
        
        logger.info("Loaded {} job advertisements", jobs.size());
    }

    private void loadApplicationsData() {
        logger.info("Loading applications test data...");
        
        List<Map<String, Object>> applications = createApplicationsData();
        List<Map<String, Object>> points = new ArrayList<>();
        
        for (int i = 0; i < applications.size(); i++) {
            Map<String, Object> application = applications.get(i);
            
            // Create point for Qdrant with real vectorization
            Map<String, Object> point = new HashMap<>();
            point.put("id", i + 1);
            
            // Vectorize application content
            String coverLetter = (String) application.get("coverLetter");
            float[] coverLetterVector = vectorizationService.vectorizeText(coverLetter);
            point.put("vector", coverLetterVector);
            point.put("payload", application);
            points.add(point);
            
            // Add to Elasticsearch with String ID
            String applicationId = "application_" + (i + 1);
            application.put("id", applicationId);
            addToElasticsearch("applications", applicationId, application);
        }
        
        // Add batch to Qdrant
        addBatchToQdrant("applications", points);
        
        logger.info("Loaded {} applications", applications.size());
    }

    private List<Map<String, Object>> createCandidatesData() {
        List<Map<String, Object>> candidates = new ArrayList<>();
        
        String[] names = {"Marko Petrovic", "Ana Jovanovic", "Stefan Nikolic", "Milica Stojanovic", 
                         "Nikola Djordjevic", "Jovana Popovic", "Milos Radovic", "Tijana Markovic",
                         "Aleksandar Vukovic", "Sara Petrovic"};
        
        String[] skills = {"Java, Spring Boot, MySQL", "Python, Django, PostgreSQL", 
                          "JavaScript, React, Node.js", "C#, .NET, SQL Server",
                          "Python, FastAPI, MongoDB", "Java, Spring, Elasticsearch",
                          "JavaScript, Vue.js, MySQL", "Python, Flask, Redis",
                          "Java, Quarkus, PostgreSQL", "TypeScript, Angular, MongoDB"};
        
        String[] locations = {"Belgrade", "Novi Sad", "Nis", "Kragujevac", "Subotica"};
        
        // Different CV contents for better vector search
        String[] cvTemplates = {
            "Experienced Java developer with 5 years of experience in enterprise software development. " +
            "Specialized in Spring Boot, microservices architecture, and cloud technologies. " +
            "Led multiple projects involving REST APIs, database design, and team management.",
            
            "Python developer with 3 years of experience in data science and web development. " +
            "Expert in Django, FastAPI, machine learning libraries, and PostgreSQL. " +
            "Passionate about AI and data analysis with strong mathematical background.",
            
            "Frontend developer with 4 years of experience in modern JavaScript frameworks. " +
            "Expert in React, Vue.js, TypeScript, and responsive web design. " +
            "Strong focus on user experience and performance optimization.",
            
            "Full-stack .NET developer with 6 years of experience in enterprise applications. " +
            "Proficient in C#, ASP.NET Core, SQL Server, and Azure cloud services. " +
            "Experience with DevOps practices and CI/CD pipelines.",
            
            "DevOps engineer with 5 years of experience in cloud infrastructure and automation. " +
            "Expert in Docker, Kubernetes, AWS, and infrastructure as code. " +
            "Strong background in monitoring, logging, and security practices.",
            
            "Data scientist with 4 years of experience in machine learning and analytics. " +
            "Proficient in Python, R, TensorFlow, and statistical modeling. " +
            "Experience with big data technologies and business intelligence.",
            
            "Mobile app developer with 3 years of experience in cross-platform development. " +
            "Expert in React Native, Flutter, and native iOS/Android development. " +
            "Strong focus on user interface design and app store optimization.",
            
            "Cloud solutions architect with 7 years of experience in enterprise cloud migration. " +
            "Expert in AWS, Azure, Terraform, and cloud security best practices. " +
            "Led multiple cloud transformation projects for Fortune 500 companies.",
            
            "QA automation engineer with 4 years of experience in test automation and quality assurance. " +
            "Proficient in Selenium, Cypress, and various testing frameworks. " +
            "Strong background in performance testing and continuous integration.",
            
            "Product manager with 5 years of experience in agile product development. " +
            "Expert in product strategy, user research, and cross-functional team leadership. " +
            "Strong analytical skills and experience with data-driven decision making."
        };
        
        for (int i = 0; i < 10; i++) {
            Map<String, Object> candidate = new HashMap<>();
            candidate.put("name", names[i]);
            candidate.put("email", "candidate" + (i + 1) + "@email.com");
            candidate.put("phone", "+381 6" + String.format("%08d", 10000000 + i));
            candidate.put("skills", skills[i]);
            candidate.put("experience", 2 + (i % 5));
            candidate.put("location", locations[i % locations.length]);
            candidate.put("cv_content", cvTemplates[i]);
            candidate.put("education", "Faculty of Technical Sciences, Computer Science");
            candidate.put("languages", "Serbian (native), English (fluent)");
            candidate.put("created_at", "2024-01-" + String.format("%02d", (i % 28) + 1) + "T10:00:00Z");
            
            candidates.add(candidate);
        }
        
        return candidates;
    }

    private List<Map<String, Object>> createJobAdvertisementsData() {
        List<Map<String, Object>> jobs = new ArrayList<>();
        
        String[] titles = {"Senior Java Developer", "Python Backend Engineer", "Frontend React Developer",
                          "Full Stack .NET Developer", "DevOps Engineer", "Data Scientist",
                          "Mobile App Developer", "Cloud Solutions Architect", "QA Automation Engineer",
                          "Product Manager"};
        
        String[] descriptions = {
            "We are looking for an experienced Java developer to join our enterprise software team. " +
            "You will work on microservices architecture, Spring Boot applications, and cloud-based solutions. " +
            "The role involves designing REST APIs, working with MySQL databases, and collaborating with cross-functional teams.",
            
            "Join our Python team and work on exciting backend projects using Django and FastAPI. " +
            "You will develop scalable web applications, work with PostgreSQL databases, and implement data processing pipelines. " +
            "Experience with machine learning libraries and cloud deployment is a plus.",
            
            "Create amazing user interfaces with React and modern web technologies. " +
            "You will work on responsive web applications, implement component libraries, and optimize performance. " +
            "Strong experience with TypeScript, HTML/CSS, and modern build tools is required.",
            
            "Full stack development using .NET Core and modern frontend frameworks. " +
            "You will build enterprise applications, work with SQL Server databases, and implement cloud solutions. " +
            "Experience with Azure services and DevOps practices is highly valued.",
            
            "Manage our cloud infrastructure and deployment pipelines using Docker and Kubernetes. " +
            "You will work with AWS/Azure services, implement monitoring solutions, and ensure high availability. " +
            "Strong background in infrastructure as code and security best practices is essential.",
            
            "Analyze data and build machine learning models using Python and advanced analytics tools. " +
            "You will work with large datasets, implement statistical models, and create data visualizations. " +
            "Experience with Pandas, Scikit-learn, and business intelligence tools is required.",
            
            "Develop mobile applications for iOS and Android using React Native and Flutter. " +
            "You will create cross-platform apps, implement native features, and optimize app performance. " +
            "Strong focus on user experience and app store optimization is essential.",
            
            "Design and implement cloud-based solutions using AWS and Azure services. " +
            "You will architect scalable systems, work with Terraform, and ensure security compliance. " +
            "Experience with enterprise cloud migration and multi-cloud strategies is highly valued.",
            
            "Ensure quality through automated testing using Selenium and Cypress frameworks. " +
            "You will develop test automation scripts, implement CI/CD pipelines, and perform performance testing. " +
            "Strong background in quality assurance methodologies and continuous integration is required.",
            
            "Lead product development and strategy using agile methodologies and data-driven approaches. " +
            "You will work with cross-functional teams, conduct user research, and drive product innovation. " +
            "Strong analytical skills and experience with product management tools is essential."
        };
        
        String[] requirements = {"Java 8+, Spring Boot, MySQL, 3+ years experience",
                               "Python 3.8+, Django/FastAPI, PostgreSQL, 2+ years experience",
                               "React, TypeScript, HTML/CSS, 2+ years experience",
                               "C#, .NET Core, SQL Server, 3+ years experience",
                               "Docker, Kubernetes, AWS/Azure, 4+ years experience",
                               "Python, Pandas, Scikit-learn, 3+ years experience",
                               "React Native, Flutter, 2+ years experience",
                               "AWS, Azure, Terraform, 5+ years experience",
                               "Selenium, Cypress, 2+ years experience",
                               "Product management, Agile, 4+ years experience"};
        
        String[] companies = {"TechCorp", "InnovateSoft", "DataFlow", "CloudTech", "DevSolutions",
                            "AI Innovations", "MobileFirst", "CloudScale", "QualityAssured", "ProductPro"};
        
        String[] locations = {"Belgrade", "Novi Sad", "Nis", "Remote", "Hybrid"};
        
        for (int i = 0; i < 10; i++) {
            Map<String, Object> job = new HashMap<>();
            job.put("title", titles[i]);
            job.put("description", descriptions[i]);
            job.put("requirements", requirements[i]);
            job.put("location", locations[i % locations.length]);
            job.put("salary_min", 80000 + (i * 10000));
            job.put("salary_max", 120000 + (i * 15000));
            job.put("company", companies[i]);
            job.put("employment_type", i % 2 == 0 ? "Full-time" : "Contract");
            job.put("experience_level", i < 3 ? "Junior" : i < 7 ? "Mid-level" : "Senior");
            job.put("created_at", "2024-01-" + String.format("%02d", (i % 28) + 1) + "T09:00:00Z");
            
            jobs.add(job);
        }
        
        return jobs;
    }

    private List<Map<String, Object>> createApplicationsData() {
        List<Map<String, Object>> applications = new ArrayList<>();
        
        String[] candidateIds = {"candidate_1", "candidate_2", "candidate_3", "candidate_4", "candidate_5",
                                "candidate_6", "candidate_7", "candidate_8", "candidate_9", "candidate_10"};
        
        String[] jobIds = {"job_1", "job_2", "job_3", "job_4", "job_5",
                          "job_6", "job_7", "job_8", "job_9", "job_10"};
        
        String[] statuses = {"Applied", "Under Review", "Interview Scheduled", "Rejected", "Accepted"};
        
        String[] coverLetters = {
            "I am very interested in this position and believe my skills match your requirements perfectly. I have extensive experience in Java development and would love to contribute to your team.",
            "With my background in software engineering and passion for technology, I am excited about the opportunity to join your company and work on innovative projects.",
            "I am writing to express my strong interest in this role. My experience in full-stack development and problem-solving skills make me an ideal candidate for this position.",
            "Having worked in the tech industry for several years, I am confident that I can bring valuable expertise and fresh perspectives to your development team.",
            "I am enthusiastic about this opportunity and believe my technical skills and collaborative approach would be a great fit for your organization.",
            "With my strong foundation in programming and eagerness to learn, I am excited about the possibility of contributing to your company's success.",
            "I am very interested in this position and would love to discuss how my experience can benefit your team and help achieve your project goals.",
            "My passion for technology and proven track record in software development make me an excellent candidate for this role.",
            "I am excited about the opportunity to apply my skills and knowledge to contribute meaningfully to your team and company objectives.",
            "With my technical expertise and strong work ethic, I am confident that I would be a valuable addition to your development team."
        };
        
        for (int i = 0; i < 10; i++) {
            Map<String, Object> application = new HashMap<>();
            application.put("candidateId", candidateIds[i]);
            application.put("jobPostingId", jobIds[i]);
            application.put("status", statuses[i % statuses.length]);
            application.put("coverLetter", coverLetters[i]);
            application.put("appliedAt", "2024-01-" + String.format("%02d", (i % 28) + 1) + "T10:00:00Z");
            application.put("notes", "Application submitted through company website");
            application.put("resumeUrl", "/resumes/candidate_" + (i + 1) + ".pdf");
            application.put("portfolioUrl", "https://portfolio.com/candidate_" + (i + 1));
            
            applications.add(application);
        }
        
        return applications;
    }

    private void addBatchToQdrant(String collection, List<Map<String, Object>> points) {
        try {
            String url = qdrantUrl + "/collections/" + collection + "/points";
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("points", points);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            
            ResponseEntity<String> response = qdrantRestTemplate.exchange(
                url, 
                HttpMethod.PUT, 
                request, 
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully added {} points to Qdrant collection {}", 
                           points.size(), collection);
            }
        } catch (Exception e) {
            logger.warn("Failed to add batch data to Qdrant collection {}: {}", 
                       collection, e.getMessage());
        }
    }

    private void addToElasticsearch(String index, String id, Map<String, Object> data) {
        try {
            String url = elasticsearchUrl + "/" + index + "/_doc/" + id;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(data, headers);
            
            elasticsearchRestTemplate.put(url, request);
        } catch (Exception e) {
            logger.warn("Failed to add data to Elasticsearch index {}: {}", index, e.getMessage());
        }
    }

    private List<Float> generateRandomVector(int size) {
        List<Float> vector = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            vector.add((float) (Math.random() * 2 - 1)); // Random values between -1 and 1
        }
        return vector;
    }
}
