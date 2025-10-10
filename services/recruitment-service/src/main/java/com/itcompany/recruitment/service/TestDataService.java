package com.itcompany.recruitment.service;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            
            // Create point for Qdrant with multiple vectorized fields
            Map<String, Object> point = new HashMap<>();
            // Use sequential ID for test data (starting from 1000 to avoid conflicts with real candidates)
            int qdrantId = 1000 + i + 1;
            point.put("id", qdrantId);
            
            // Vectorize multiple fields for better search capabilities
            String cvContent = (String) candidate.get("cv_content");
            String[] skillsArray = (String[]) candidate.get("skills");
            List<String> skillsList = Arrays.asList(skillsArray);
            String skills = String.join(", ", skillsList);
            //String workExperience = generateWorkExperienceText(candidate);
            
            // Create combined vector from multiple fields
            String combinedText = cvContent + " " + skills;
            float[] combinedVector = vectorizationService.vectorizeText(combinedText);
            point.put("vector", combinedVector);
            
            // Add individual vectorized fields for multi-vector search
           /* Map<String, Object> vectors = new HashMap<>();
            vectors.put("cv_vector", vectorizationService.vectorizeText(cvContent));
            vectors.put("skills_vector", vectorizationService.vectorizeText(skills));
            vectors.put("experience_vector", vectorizationService.vectorizeText(workExperience));
            point.put("named_vectors", vectors);*/
            
            // Add to Elasticsearch with String ID first
            String candidateId = "candidate_" + (i + 1);
            candidate.put("id", candidateId);
            
            // Add Qdrant ID to payload for reference
            candidate.put("qdrant_id", qdrantId);
            point.put("payload", candidate);
            points.add(point);
            
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
            
            // Create point for Qdrant with multiple vectorized fields
            Map<String, Object> point = new HashMap<>();
            // Use sequential ID for test data (starting from 2000 to avoid conflicts with real job postings)
            int qdrantId = 2000 + i + 1;
            point.put("id", qdrantId);
            
            // Vectorize multiple fields for better search capabilities
            String title = (String) job.get("title");
            String description = (String) job.get("description");
            String company = (String) job.get("company");
            @SuppressWarnings("unchecked")
            List<String> skillsList = (List<String>) job.get("skills_required");
            String skills = String.join(", ", skillsList);
            
            // Create combined vector from multiple fields
            String combinedText = title + " " + description + " " + company + " " + skills;
            float[] combinedVector = vectorizationService.vectorizeText(combinedText);
            point.put("vector", combinedVector);
            
            // Add to Elasticsearch with String ID first
            String jobId = "job_" + (i + 1);
            job.put("id", jobId);
            
            // Add Qdrant ID to payload for reference
            job.put("qdrant_id", qdrantId);
            point.put("payload", job);
            points.add(point);
            
            addToElasticsearch("job_advertisements", jobId, job);
        }
        
        // Batch add to Qdrant
        addBatchToQdrant("job_advertisements", points);
        
        logger.info("Loaded {} job advertisements", jobs.size());
    }

    private void loadApplicationsData() {
        logger.info("Loading applications test data...");
        
        List<Map<String, Object>> applications = createApplicationsData();
        
        for (int i = 0; i < applications.size(); i++) {
            Map<String, Object> application = applications.get(i);
            
            // Add to Elasticsearch only (no vectorization needed for applications)
            String applicationId = "application_" + (i + 1);
            application.put("id", applicationId);
            addToElasticsearch("applications", applicationId, application);
        }
        
        logger.info("Loaded {} applications", applications.size());
    }

    private List<Map<String, Object>> createCandidatesData() {
        List<Map<String, Object>> candidates = new ArrayList<>();
        
        // Expanded data arrays for 1000+ candidates
        String[] firstNames = {"Marko", "Ana", "Stefan", "Milica", "Nikola", "Jovana", "Milos", "Tijana", 
                              "Aleksandar", "Sara", "Petar", "Jelena", "Vladimir", "Marija", "Dusan", 
                              "Natasa", "Bojan", "Tamara", "Nemanja", "Jovana", "Milan", "Snezana", 
                              "Dejan", "Vesna", "Zoran", "Gordana", "Slobodan", "Radmila", "Miodrag", "Biljana"};
        
        String[] lastNames = {"Petrovic", "Jovanovic", "Nikolic", "Stojanovic", "Djordjevic", "Popovic", 
                             "Radovic", "Markovic", "Vukovic", "Petrovic", "Ilic", "Milic", "Pavlovic", 
                             "Stefanovic", "Lazic", "Mitic", "Jankovic", "Kostic", "Ristic", "Mladenovic"};
        
        String[] locations = {"Belgrade", "Novi Sad", "Nis", "Kragujevac", "Subotica", "Cacak", "Zrenjanin", 
                             "Pancevo", "Novi Pazar", "Kraljevo", "Smederevo", "Leskovac", "Uzice", "Vranje", 
                             "Sabac", "Pozarevac", "Krusevac", "Sombor", "Zajecar", "Sremska Mitrovica"};
        
        String[] skillCategories = {
            "Java, Spring Boot, MySQL, Hibernate, Maven, Git",
            "Python, Django, FastAPI, PostgreSQL, Pandas, NumPy",
            "JavaScript, React, Node.js, Express, MongoDB, TypeScript",
            "C#, .NET Core, ASP.NET, SQL Server, Entity Framework, Azure",
            "Python, Flask, Redis, Celery, Docker, Kubernetes",
            "Java, Spring, Elasticsearch, Kibana, Logstash, Microservices",
            "JavaScript, Vue.js, MySQL, Webpack, Babel, Sass",
            "Python, Scikit-learn, TensorFlow, Jupyter, Matplotlib, Seaborn",
            "Go, Gin, PostgreSQL, gRPC, Protocol Buffers, Docker",
            "PHP, Laravel, MySQL, Composer, Redis, Apache",
            "Ruby, Rails, PostgreSQL, RSpec, Capybara, Sidekiq",
            "Scala, Akka, Spark, Kafka, Cassandra, SBT",
            "Rust, Actix, PostgreSQL, Tokio, Serde, Cargo",
            "Kotlin, Spring Boot, PostgreSQL, Gradle, JUnit, MockK",
            "Swift, iOS, Xcode, Core Data, Alamofire, SnapKit",
            "Dart, Flutter, Firebase, Provider, Bloc, GetX",
            "C++, Qt, CMake, Boost, OpenCV, Eigen",
            "C, Linux, Make, GDB, Valgrind, POSIX",
            "Assembly, Embedded Systems, Microcontrollers, RTOS, I2C, SPI",
            "MATLAB, Simulink, Control Systems, Signal Processing, Image Processing"
        };
        
        String[] jobTitles = {
            "Senior Software Engineer", "Full Stack Developer", "Backend Developer", "Frontend Developer",
            "DevOps Engineer", "Data Scientist", "Machine Learning Engineer", "Mobile App Developer",
            "Cloud Solutions Architect", "QA Automation Engineer", "Product Manager", "Technical Lead",
            "Software Architect", "Database Administrator", "System Administrator", "Security Engineer",
            "UI/UX Designer", "Business Analyst", "Project Manager", "Scrum Master"
        };
        
        String[] companies = {
            "TechCorp", "InnovateSoft", "DataFlow", "CloudTech", "DevSolutions", "AI Innovations", 
            "MobileFirst", "CloudScale", "QualityAssured", "ProductPro", "CodeCraft", "DataDriven",
            "CloudNative", "AgileWorks", "TechForward", "InnovationLab", "DigitalCraft", "SmartTech",
            "FutureSoft", "EliteCode", "ProDev", "TechMasters", "CodeGenius", "DataWise", "CloudPro"
        };
        
        String[] universities = {
            "Faculty of Technical Sciences, University of Novi Sad",
            "School of Electrical Engineering, University of Belgrade",
            "Faculty of Mathematics, University of Belgrade",
            "Faculty of Technical Sciences, University of Nis",
            "Faculty of Computer Science, University of Kragujevac",
            "Faculty of Organizational Sciences, University of Belgrade",
            "Faculty of Economics, University of Belgrade",
            "Faculty of Mechanical Engineering, University of Belgrade",
            "Faculty of Civil Engineering, University of Belgrade",
            "Faculty of Mining and Geology, University of Belgrade"
        };
        
        String[] degreeTypes = {"Bachelor of Science", "Master of Science", "Master of Engineering", 
                               "Bachelor of Engineering", "Master of Business Administration", "PhD"};
        
        String[] fieldsOfStudy = {
            "Computer Science", "Software Engineering", "Information Technology", "Computer Engineering",
            "Data Science", "Artificial Intelligence", "Cybersecurity", "Information Systems",
            "Mathematics", "Statistics", "Physics", "Electrical Engineering"
        };
        
        String[] certifications = {
            "AWS Certified Solutions Architect", "Google Cloud Professional", "Microsoft Azure Expert",
            "Oracle Certified Professional", "Cisco Certified Network Associate", "PMP Certification",
            "Scrum Master Certification", "ITIL Foundation", "CompTIA Security+", "Kubernetes Administrator"
        };
        
        // Generate 1000+ candidates
        for (int i = 0; i < 1200; i++) {
            Map<String, Object> candidate = new HashMap<>();
            
            // Basic information
            String firstName = firstNames[i % firstNames.length];
            String lastName = lastNames[i % lastNames.length];
            candidate.put("firstName", firstName);
            candidate.put("lastName", lastName);
            candidate.put("name", firstName + " " + lastName);
            candidate.put("email", firstName.toLowerCase() + "." + lastName.toLowerCase() + (i + 1) + "@email.com");
            candidate.put("phone", "+381 6" + String.format("%08d", 10000000 + (i % 1000000)));
            candidate.put("location", locations[i % locations.length]);
            
            // Skills and experience - use random selection instead of modulo
            String skills = skillCategories[(int)(Math.random() * skillCategories.length)];
            candidate.put("skills", skills.split(", "));
            int yearsExp = 1 + (int)(Math.random() * 15); // 1-15 years experience
            candidate.put("experience", yearsExp);
            candidate.put("yearsOfExperience", yearsExp);
            
            // CV content for vectorization - use random job title and company
            String jobTitle = jobTitles[(int)(Math.random() * jobTitles.length)];
            String company = companies[(int)(Math.random() * companies.length)];
            
            // Determine CV template based on skills, not just job title
            String cvTemplateType = determineCvTemplateType(skills);
            String cvContent = generateCvContent(firstName, lastName, skills, jobTitle, company, yearsExp, cvTemplateType);
            candidate.put("cvContent", cvContent);
            candidate.put("cv_content", cvContent);
            
            // Additional fields for better filtering
            candidate.put("currentPosition", jobTitle);
            candidate.put("expectedSalary", 50000 + (int)(Math.random() * 100000)); // $50k - $150k
            candidate.put("preferredEmploymentType", Math.random() < 0.33 ? "Full-time" : Math.random() < 0.66 ? "Part-time" : "Contract");
            candidate.put("willingToRelocate", Math.random() < 0.5);
            candidate.put("dateOfBirth", generateRandomDate(1970, 2000));
            candidate.put("linkedinProfile", "https://linkedin.com/in/" + firstName.toLowerCase() + lastName.toLowerCase());
            candidate.put("githubProfile", "https://github.com/" + firstName.toLowerCase() + lastName.toLowerCase());
            candidate.put("matchScore", Math.random() * 100);
            
            // Work experience
            candidate.put("workExperiences", generateWorkExperiences(companies, jobTitles, 1 + (int)(Math.random() * 4)));
            
            // Education history
            candidate.put("educationHistory", generateEducationHistory(universities, degreeTypes, fieldsOfStudy));
            
            // Certifications
            candidate.put("certifications", generateCertifications(certifications, 1 + (int)(Math.random() * 3)));
            
            // Languages
            String[] languages = {"Serbian (native)", "English (fluent)", "German (intermediate)", "French (basic)"};
            candidate.put("languages", languages[(int)(Math.random() * languages.length)]);
            
            // Registration date
            candidate.put("registrationDate", generateRandomDateTime(2020, 2024));
            candidate.put("created_at", generateRandomDateTime(2020, 2024));
            
            candidates.add(candidate);
        }
        
        return candidates;
    }
    
    private String determineCvTemplateType(String skills) {
        String skillsLower = skills.toLowerCase();
        if (skillsLower.contains("java") || skillsLower.contains("spring")) {
            return "java";
        } else if (skillsLower.contains("python") || skillsLower.contains("django") || skillsLower.contains("tensorflow")) {
            return "python";
        } else if (skillsLower.contains("react") || skillsLower.contains("javascript") || skillsLower.contains("frontend")) {
            return "frontend";
        } else if (skillsLower.contains("docker") || skillsLower.contains("kubernetes") || skillsLower.contains("aws")) {
            return "devops";
        } else if (skillsLower.contains("pandas") || skillsLower.contains("scikit") || skillsLower.contains("data")) {
            return "data";
        } else {
            return "general";
        }
    }
    
    private String generateCvContent(String firstName, String lastName, String skills, String currentPosition, 
                                   String company, int yearsExperience, String templateType) {
        // Different CV templates based on template type determined from skills
        String[] cvTemplates = getCvTemplates(templateType, skills);
        String template = cvTemplates[(int)(Math.random() * cvTemplates.length)];
        
        // Generate a concise, natural CV text without structured sections
        String cvText = template.replace("{firstName}", firstName)
                               .replace("{lastName}", lastName)
                               .replace("{position}", currentPosition)
                               .replace("{years}", String.valueOf(yearsExperience))
                               .replace("{company}", company)
                               .replace("{skill1}", skills.split(", ")[0])
                               .replace("{skill2}", skills.split(", ")[1])
                               .replace("{skill3}", skills.split(", ").length > 2 ? skills.split(", ")[2] : "");
        
        // Add a natural continuation sentence about skills and experience
        String[] skillSentences = {
            " Proficient in " + skills.split(", ")[0] + " and " + skills.split(", ")[1] + " with hands-on experience in modern development practices.",
            " Skilled in " + skills.split(", ")[0] + ", " + skills.split(", ")[1] + " and other cutting-edge technologies.",
            " Experienced with " + skills.split(", ")[0] + " and " + skills.split(", ")[1] + " in enterprise environments.",
            " Strong background in " + skills.split(", ")[0] + " and " + skills.split(", ")[1] + " with focus on scalable solutions.",
            " Expertise in " + skills.split(", ")[0] + " and " + skills.split(", ")[1] + " with proven track record of delivering quality software."
        };
        
        String skillSentence = skillSentences[(int)(Math.random() * skillSentences.length)];
        
        // Add experience context
        String experienceContext = yearsExperience > 5 ? 
            " With over " + yearsExperience + " years of experience, " + firstName + " has successfully led multiple projects and mentored junior developers." :
            " Having " + yearsExperience + " years of experience, " + firstName + " brings fresh perspective and strong technical skills to any development team.";
        
        return cvText + skillSentence + experienceContext;
    }
    
    private String[] getCvTemplates(String templateType, String skills) {
        if ("java".equals(templateType)) {
            return new String[]{
                "Experienced {position} with {years} years of expertise in Java ecosystem and enterprise software development. " +
                "Specialized in Spring Framework, microservices architecture, and cloud-native applications. " +
                "Currently working at {company} where I lead development of scalable backend systems using {skill1}, {skill2}, and {skill3}. " +
                "Passionate about clean code, design patterns, and mentoring junior developers.",
                
                "Senior {position} with {years} years of experience building robust Java applications and distributed systems. " +
                "Expert in Spring Boot, RESTful APIs, and database optimization. " +
                "At {company}, I architect and develop high-performance applications serving millions of users. " +
                "Strong background in {skill1}, {skill2}, and agile development methodologies.",
                
                "Lead {position} with {years} years of experience in Java development and team leadership. " +
                "Specialized in microservices, containerization, and DevOps practices. " +
                "Currently at {company}, I design and implement enterprise-grade solutions using {skill1} and {skill2}. " +
                "Committed to code quality, performance optimization, and continuous learning."
            };
        } else if ("python".equals(templateType)) {
            return new String[]{
                "Data-driven {position} with {years} years of experience in Python development and machine learning. " +
                "Expert in data analysis, statistical modeling, and building scalable data pipelines. " +
                "At {company}, I develop AI-powered solutions using {skill1}, {skill2}, and {skill3}. " +
                "Passionate about turning data into actionable insights and business value.",
                
                "Senior {position} with {years} years of expertise in Python, data science, and backend development. " +
                "Specialized in Django/FastAPI, machine learning algorithms, and cloud platforms. " +
                "Currently at {company}, I build intelligent systems and data-driven applications. " +
                "Strong background in {skill1}, {skill2}, and statistical analysis.",
                
                "Full-stack {position} with {years} years of experience in Python development and web technologies. " +
                "Expert in building scalable web applications, APIs, and data processing systems. " +
                "At {company}, I develop end-to-end solutions using {skill1}, {skill2}, and modern frameworks. " +
                "Committed to writing clean, maintainable code and following best practices."
            };
        } else if ("frontend".equals(templateType)) {
            return new String[]{
                "Creative {position} with {years} years of experience in modern frontend development and user experience design. " +
                "Expert in React, JavaScript, and responsive web design. " +
                "At {company}, I create intuitive user interfaces and interactive web applications using {skill1}, {skill2}, and {skill3}. " +
                "Passionate about user-centered design and performance optimization.",
                
                "Senior {position} with {years} years of expertise in frontend technologies and component-based architecture. " +
                "Specialized in React ecosystem, TypeScript, and state management. " +
                "Currently at {company}, I lead frontend development and mentor junior developers. " +
                "Strong background in {skill1}, {skill2}, and modern build tools.",
                
                "Lead {position} with {years} years of experience in frontend development and team management. " +
                "Expert in React, Vue.js, and cross-platform development. " +
                "At {company}, I architect scalable frontend solutions and establish development standards. " +
                "Committed to accessibility, performance, and code quality."
            };
        } else if ("devops".equals(templateType)) {
            return new String[]{
                "Infrastructure-focused {position} with {years} years of experience in cloud platforms and automation. " +
                "Expert in Docker, Kubernetes, and CI/CD pipelines. " +
                "At {company}, I manage cloud infrastructure and implement DevOps best practices using {skill1}, {skill2}, and {skill3}. " +
                "Passionate about infrastructure as code and system reliability.",
                
                "Senior {position} with {years} years of expertise in cloud architecture and deployment automation. " +
                "Specialized in AWS/Azure, containerization, and monitoring solutions. " +
                "Currently at {company}, I design and maintain scalable cloud infrastructure. " +
                "Strong background in {skill1}, {skill2}, and security best practices.",
                
                "Lead {position} with {years} years of experience in DevOps practices and team leadership. " +
                "Expert in cloud platforms, automation tools, and system architecture. " +
                "At {company}, I establish DevOps culture and implement modern deployment strategies. " +
                "Committed to reliability, security, and continuous improvement."
            };
        } else if ("data".equals(templateType)) {
            return new String[]{
                "Analytical {position} with {years} years of experience in data science and machine learning. " +
                "Expert in Python, statistical analysis, and predictive modeling. " +
                "At {company}, I develop machine learning models and data-driven solutions using {skill1}, {skill2}, and {skill3}. " +
                "Passionate about extracting insights from complex datasets and driving business decisions.",
                
                "Senior {position} with {years} years of expertise in data analysis and artificial intelligence. " +
                "Specialized in deep learning, big data processing, and model deployment. " +
                "Currently at {company}, I lead data science initiatives and mentor junior analysts. " +
                "Strong background in {skill1}, {skill2}, and statistical methods.",
                
                "Lead {position} with {years} years of experience in data science and team management. " +
                "Expert in machine learning, data engineering, and business intelligence. " +
                "At {company}, I architect data solutions and establish analytics best practices. " +
                "Committed to data quality, model accuracy, and stakeholder communication."
            };
        } else if ("general".equals(templateType)) {
            return new String[]{
                "Experienced {position} with {years} years of expertise in software development and technology leadership. " +
                "Specialized in {skill1}, {skill2}, and modern development practices. " +
                "Currently at {company}, I lead technical initiatives and deliver high-quality software solutions. " +
                "Passionate about innovation, team collaboration, and continuous learning.",
                
                "Senior {position} with {years} years of experience in software engineering and project management. " +
                "Expert in {skill1}, {skill2}, and agile methodologies. " +
                "At {company}, I architect solutions and mentor development teams. " +
                "Strong background in {skill1}, {skill2}, and system design.",
                
                "Lead {position} with {years} years of expertise in software development and team leadership. " +
                "Specialized in {skill1}, {skill2}, and technical strategy. " +
                "Currently at {company}, I drive technical excellence and innovation. " +
                "Committed to code quality, team growth, and business impact."
            };
        } else {
            // Fallback for any unexpected template types
            return new String[]{
                "Experienced {position} with {years} years of expertise in software development. " +
                "Specialized in {skill1}, {skill2}, and modern development practices. " +
                "Currently at {company}, I contribute to technical initiatives and deliver quality software solutions. " +
                "Passionate about technology and continuous learning."
            };
        }
    }
    
    
    private List<Map<String, Object>> generateWorkExperiences(String[] companies, String[] jobTitles, int count) {
        List<Map<String, Object>> experiences = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            Map<String, Object> exp = new HashMap<>();
            exp.put("company", companies[i % companies.length]);
            exp.put("position", jobTitles[i % jobTitles.length]);
            exp.put("description", "Developed and maintained software applications using modern technologies. " +
                    "Collaborated with team members to deliver high-quality solutions. " +
                    "Participated in code reviews and technical discussions.");
            exp.put("technologies", new String[]{"Java", "Spring Boot", "MySQL", "Docker", "Git"});
            exp.put("startDate", generateRandomDate(2015, 2023));
            exp.put("endDate", i == count - 1 ? null : generateRandomDate(2018, 2024));
            exp.put("isCurrent", i == count - 1);
            experiences.add(exp);
        }
        
        return experiences;
    }
    
    private List<Map<String, Object>> generateEducationHistory(String[] universities, String[] degreeTypes, 
                                                              String[] fieldsOfStudy) {
        List<Map<String, Object>> education = new ArrayList<>();
        
        Map<String, Object> degree = new HashMap<>();
        degree.put("institution", universities[0]);
        degree.put("degree", degreeTypes[0]);
        degree.put("fieldOfStudy", fieldsOfStudy[0]);
        degree.put("startDate", generateRandomDate(2010, 2018));
        degree.put("endDate", generateRandomDate(2014, 2020));
        degree.put("gpa", 3.0 + Math.random() * 1.5);
        education.add(degree);
        
        return education;
    }
    
    private List<String> generateCertifications(String[] certifications, int count) {
        List<String> certs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            certs.add(certifications[i % certifications.length]);
        }
        return certs;
    }
    
    private String generateRandomDate(int startYear, int endYear) {
        int year = startYear + (int)(Math.random() * (endYear - startYear + 1));
        int month = 1 + (int)(Math.random() * 12);
        int day = 1 + (int)(Math.random() * 28);
        return String.format("%04d-%02d-%02d", year, month, day);
    }
    
    private String generateRandomDateTime(int startYear, int endYear) {
        int year = startYear + (int)(Math.random() * (endYear - startYear + 1));
        int month = 1 + (int)(Math.random() * 12);
        int day = 1 + (int)(Math.random() * 28);
        int hour = (int)(Math.random() * 24);
        int minute = (int)(Math.random() * 60);
        return String.format("%04d-%02d-%02dT%02d:%02d:00", year, month, day, hour, minute);
    }

    private List<Map<String, Object>> createJobAdvertisementsData() {
        List<Map<String, Object>> jobs = new ArrayList<>();
        
        String[] titles = {
            "Senior Java Developer", "Python Backend Engineer", "Frontend React Developer",
            "Full Stack .NET Developer", "DevOps Engineer", "Data Scientist",
            "Mobile App Developer", "Cloud Solutions Architect", "QA Automation Engineer",
            "Product Manager", "Software Architect", "Database Administrator",
            "System Administrator", "Security Engineer", "UI/UX Designer",
            "Business Analyst", "Project Manager", "Scrum Master", "Technical Lead",
            "Machine Learning Engineer", "Blockchain Developer", "Game Developer",
            "Embedded Systems Engineer", "Network Engineer", "IT Support Specialist",
            "Cybersecurity Analyst", "Cloud Engineer", "Site Reliability Engineer",
            "Data Engineer", "Backend Developer", "Frontend Developer", "Full Stack Developer"
        };
        
        String[] companies = {
            "TechCorp", "InnovateSoft", "DataFlow", "CloudTech", "DevSolutions",
            "AI Innovations", "MobileFirst", "CloudScale", "QualityAssured", "ProductPro",
            "CodeCraft", "DataDriven", "CloudNative", "AgileWorks", "TechForward",
            "InnovationLab", "DigitalCraft", "SmartTech", "FutureSoft", "EliteCode",
            "ProDev", "TechMasters", "CodeGenius", "DataWise", "CloudPro",
            "StartupX", "ScaleUp", "TechGiant", "InnovationHub", "DigitalFirst"
        };
        
        String[] locations = {
            "Belgrade", "Novi Sad", "Nis", "Kragujevac", "Subotica", "Remote", "Hybrid",
            "Cacak", "Zrenjanin", "Pancevo", "Novi Pazar", "Kraljevo", "Smederevo",
            "Leskovac", "Uzice", "Vranje", "Sabac", "Pozarevac", "Krusevac", "Sombor"
        };
        
        String[] employmentTypes = {"Full-time", "Part-time", "Contract", "Freelance", "Internship"};
        String[] experienceLevels = {"Junior", "Mid-level", "Senior", "Lead", "Principal"};
        String[] industries = {
            "Technology", "Finance", "Healthcare", "E-commerce", "Education", "Manufacturing",
            "Consulting", "Media", "Gaming", "Telecommunications", "Automotive", "Energy"
        };
        
        // Generate 1000+ job postings
        for (int i = 0; i < 1000; i++) {
            Map<String, Object> job = new HashMap<>();
            
            String title = titles[(int)(Math.random() * titles.length)];
            String company = companies[(int)(Math.random() * companies.length)];
            String location = locations[(int)(Math.random() * locations.length)];
            String industry = industries[(int)(Math.random() * industries.length)];
            String employmentType = employmentTypes[(int)(Math.random() * employmentTypes.length)];
            String experienceLevel = experienceLevels[(int)(Math.random() * experienceLevels.length)];
            
            // Required fields for JobPosting model
            job.put("title", title);
            job.put("description", generateJobDescription(title, company, industry, location));
            job.put("department", company); // Use company as department
            job.put("location", location);
            job.put("experienceLevel", experienceLevel);
            job.put("minYearsExperience", getMinYearsForLevel(experienceLevel));
            job.put("maxYearsExperience", getMaxYearsForLevel(experienceLevel));
            job.put("requiredSkills", generateRequiredSkills(title));
            job.put("preferredSkills", generatePreferredSkills(title));
            job.put("minSalary", 40000 + (int)(Math.random() * 120000));
            job.put("maxSalary", 60000 + (int)(Math.random() * 150000));
            job.put("employmentType", employmentType);
            job.put("isActive", true);
            job.put("postedDate", generateRandomDateTime(2023, 2024));
            job.put("applicationDeadline", generateRandomDateTime(2024, 2025));
            job.put("hrManagerId", "hr_" + (1 + (int)(Math.random() * 10)));
            job.put("numberOfPositions", 1 + (int)(Math.random() * 5));
            
            // Additional fields
            job.put("company", company);
            job.put("industry", industry);
            job.put("job_type", Math.random() < 0.5 ? "Permanent" : "Contract");
            job.put("remote_allowed", Math.random() < 0.33);
            job.put("benefits", generateBenefits());
            job.put("skills_required", generateRequiredSkills(title));
            job.put("posted_date", generateRandomDateTime(2023, 2024));
            job.put("application_deadline", generateRandomDateTime(2024, 2025));
            job.put("created_at", generateRandomDateTime(2023, 2024));
            
            jobs.add(job);
        }
        
        return jobs;
    }
    
    private String generateJobDescription(String title, String company, String industry, String location) {
        // Generate concise, natural job descriptions without structured sections
        String[] jobTemplates = getJobDescriptionTemplates(title, industry);
        String template = jobTemplates[(int)(Math.random() * jobTemplates.length)];
        
        return template.replace("{company}", company)
                      .replace("{industry}", industry.toLowerCase())
                      .replace("{title}", title.toLowerCase())
                      .replace("{location}", location);
    }
    
    private String[] getJobDescriptionTemplates(String title, String industry) {
        // Generate concise, natural job descriptions for all positions
        String[] baseTemplates = {
            "We are a leading {industry} company seeking a talented {title} to join our innovative team in {location}. " +
            "We are looking for an experienced {title} who is passionate about technology and software development. " +
            "You will work on cutting-edge projects using modern technologies and contribute to our platform's success. " +
            "This role offers the opportunity to work with a talented team and grow your career in a dynamic environment. " +
            "We offer competitive salary, flexible working arrangements, professional development opportunities and a collaborative work environment.",
            
            "Join {company} as a {title} and be part of our mission to revolutionize the {industry} sector through innovative technology solutions. " +
            "We are seeking a skilled {title} to join our development team in {location}. " +
            "You will be responsible for building high-quality applications and contributing to our platform's success. " +
            "This is an excellent opportunity to work with modern technologies and grow your career in a dynamic environment. " +
            "We offer attractive compensation package, health insurance, learning budget and flexible schedule.",
            
            "We are a data-driven {industry} company looking for a passionate {title} to join our team in {location} and help us build intelligent solutions. " +
            "We are seeking a talented {title} who loves working with technology and building innovative systems. " +
            "You will work on exciting projects involving modern development practices and cutting-edge technologies. " +
            "This role offers the opportunity to work with a talented team and make a real impact. " +
            "We offer competitive salary, flexible working arrangements, access to latest tools and professional development opportunities."
        };
        
        return baseTemplates;
    }
    
    
    private List<String> generateBenefits() {
        return List.of(
            "Health Insurance", "Dental Insurance", "Vision Insurance", "Life Insurance",
            "401(k) Matching", "Paid Time Off", "Flexible Schedule", "Remote Work",
            "Professional Development", "Gym Membership", "Free Meals", "Transportation Allowance"
        );
    }
    
    private List<String> generateRequiredSkills(String title) {
        if (title.toLowerCase().contains("java")) {
            return List.of("Java", "Spring Boot", "MySQL", "Maven", "Git", "REST APIs");
        } else if (title.toLowerCase().contains("python")) {
            return List.of("Python", "Django", "FastAPI", "PostgreSQL", "Pandas", "NumPy");
        } else if (title.toLowerCase().contains("react")) {
            return List.of("React", "JavaScript", "TypeScript", "HTML", "CSS", "Node.js");
        } else if (title.toLowerCase().contains("devops")) {
            return List.of("Docker", "Kubernetes", "AWS", "Terraform", "Jenkins", "Linux");
        } else if (title.toLowerCase().contains("data")) {
            return List.of("Python", "SQL", "Pandas", "Scikit-learn", "TensorFlow", "Jupyter");
        } else {
            return List.of("Programming", "Problem Solving", "Teamwork", "Communication", "Agile", "Git");
        }
    }
    
    private List<String> generatePreferredSkills(String title) {
        if (title.toLowerCase().contains("java")) {
            return List.of("Microservices", "Docker", "Kubernetes", "Redis", "MongoDB", "Kafka");
        } else if (title.toLowerCase().contains("python")) {
            return List.of("Machine Learning", "Docker", "Kubernetes", "Redis", "MongoDB", "Celery");
        } else if (title.toLowerCase().contains("react")) {
            return List.of("Redux", "Next.js", "GraphQL", "Docker", "Jest", "Webpack");
        } else if (title.toLowerCase().contains("devops")) {
            return List.of("Prometheus", "Grafana", "ELK Stack", "Ansible", "Helm", "Istio");
        } else if (title.toLowerCase().contains("data")) {
            return List.of("Machine Learning", "Deep Learning", "Apache Spark", "Hadoop", "Docker", "Kubernetes");
        } else {
            return List.of("Leadership", "Mentoring", "Architecture", "Cloud Computing", "CI/CD", "Monitoring");
        }
    }
    
    private int getMinYearsForLevel(String experienceLevel) {
        switch (experienceLevel.toLowerCase()) {
            case "junior": return 0;
            case "mid-level": return 2;
            case "senior": return 5;
            case "lead": return 7;
            case "principal": return 10;
            default: return 0;
        }
    }
    
    private int getMaxYearsForLevel(String experienceLevel) {
        switch (experienceLevel.toLowerCase()) {
            case "junior": return 2;
            case "mid-level": return 5;
            case "senior": return 8;
            case "lead": return 12;
            case "principal": return 15;
            default: return 5;
        }
    }

    private List<Map<String, Object>> createApplicationsData() {
        List<Map<String, Object>> applications = new ArrayList<>();
        
        String[] statuses = {"Applied", "Under Review", "Interview Scheduled", "Technical Interview", 
                           "Final Interview", "Rejected", "Accepted", "Withdrawn", "On Hold"};
        
        String[] hrReviewers = {"John Smith", "Sarah Johnson", "Mike Davis", "Lisa Wilson", "Tom Brown",
                               "Emma Taylor", "David Miller", "Anna Garcia", "Chris Anderson", "Maria Rodriguez"};
        
        // Generate 2000+ applications
        for (int i = 0; i < 2000; i++) {
            Map<String, Object> application = new HashMap<>();
            
            // Random candidate and job IDs
            String candidateId = "candidate_" + (1 + (int)(Math.random() * 1200));
            String jobId = "job_" + (1 + (int)(Math.random() * 1000));
            
            application.put("candidateId", candidateId);
            application.put("jobPostingId", jobId);
            application.put("status", statuses[(int)(Math.random() * statuses.length)]);
            application.put("coverLetter", generateCoverLetter(i));
            application.put("applicationDate", generateRandomDateTime(2023, 2024));
            application.put("appliedAt", generateRandomDateTime(2023, 2024));
            
            // Match scores
            application.put("overallMatchScore", Math.random() * 100);
            application.put("skillMatchScore", Math.random() * 100);
            application.put("experienceMatchScore", Math.random() * 100);
            application.put("cvMatchScore", Math.random() * 100);
            
            // Additional fields
            application.put("ranking", 1 + (int)(Math.random() * 100));
            application.put("hrNotes", generateHrNotes(i));
            application.put("reviewedBy", Math.random() < 0.33 ? hrReviewers[(int)(Math.random() * hrReviewers.length)] : null);
            application.put("reviewDate", Math.random() < 0.33 ? generateRandomDateTime(2023, 2024) : null);
            application.put("isShortlisted", Math.random() < 0.2);
            application.put("notes", "Application submitted through company website");
            application.put("resumeUrl", "/resumes/" + candidateId + ".pdf");
            application.put("portfolioUrl", "https://portfolio.com/" + candidateId);
            application.put("linkedinUrl", "https://linkedin.com/in/" + candidateId);
            application.put("githubUrl", "https://github.com/" + candidateId);
            
            // Interview details
            String status = statuses[(int)(Math.random() * statuses.length)];
            if (status.contains("Interview")) {
                application.put("interviewDate", generateRandomDateTime(2024, 2025));
                application.put("interviewType", Math.random() < 0.5 ? "Video Call" : "In-Person");
                application.put("interviewer", hrReviewers[(int)(Math.random() * hrReviewers.length)]);
            }
            
            applications.add(application);
        }
        
        return applications;
    }
    
    private String generateCoverLetter(int index) {
        String[] templates = {
            "I am writing to express my strong interest in this position. With my extensive experience in software development and passion for technology, I believe I would be a valuable addition to your team. I am particularly excited about the opportunity to work on innovative projects and contribute to your company's success.",
            
            "I am very interested in this role and believe my skills and experience align perfectly with your requirements. Having worked in the tech industry for several years, I have developed strong technical abilities and collaborative skills that would make me an ideal candidate for this position.",
            
            "With my background in computer science and proven track record in software development, I am excited about the possibility of joining your team. I am confident that my technical expertise and problem-solving abilities would be a great fit for your organization.",
            
            "I am enthusiastic about this opportunity and would love to discuss how my experience can benefit your team. My strong foundation in programming and eagerness to learn make me an excellent candidate for this role.",
            
            "Having reviewed the job description, I am confident that my skills and experience make me a strong candidate for this position. I am particularly drawn to your company's innovative approach and would be thrilled to contribute to your ongoing projects.",
            
            "I am writing to apply for this position as I believe my technical skills and passion for software development align well with your requirements. I am excited about the opportunity to work with a dynamic team and contribute to meaningful projects.",
            
            "With my comprehensive background in software engineering and strong analytical skills, I am confident that I would be a valuable addition to your team. I am particularly interested in the technical challenges this role presents.",
            
            "I am very interested in this position and believe my experience in full-stack development and problem-solving skills make me an ideal candidate. I am excited about the opportunity to work on cutting-edge technologies and contribute to your company's growth.",
            
            "Having worked in various software development roles, I have gained valuable experience that I believe would be beneficial to your team. I am particularly drawn to your company's mission and would be honored to contribute to your success.",
            
            "I am writing to express my interest in this role as I believe my technical expertise and collaborative approach would be a great fit for your organization. I am excited about the opportunity to work on challenging projects and grow professionally."
        };
        
        return templates[index % templates.length];
    }
    
    private String generateHrNotes(int index) {
        String[] notes = {
            "Strong technical background, good communication skills",
            "Excellent candidate, highly recommended for next round",
            "Good experience but may need additional training",
            "Outstanding technical skills, perfect cultural fit",
            "Average candidate, consider for junior position",
            "Excellent problem-solving abilities, strong team player",
            "Good potential but lacks some required experience",
            "Outstanding candidate, fast-track for final interview",
            "Good technical skills, needs improvement in communication",
            "Excellent cultural fit, strong technical background"
        };
        
        return index % 3 == 0 ? notes[index % notes.length] : null;
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
}
