package com.itcompany.recruitment.service;

import com.itcompany.recruitment.model.*;
import com.itcompany.recruitment.dto.CandidateSearchRequest;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private CandidateService candidateService;


    /**
     * Generates comprehensive report about candidates with simple and complex sections
     */
    public Report generateCandidateReport(String reportTitle, String generatedBy) {
        LocalDateTime now = LocalDateTime.now();
        
        // Creating simple sections
        List<SimpleSection> simpleSections = Arrays.asList(
            generateCandidatesByLocationSection(),
            generateCandidatesBySkillsSection(),
            generateCandidatesByExperienceSection(),
            generateCandidatesBySalarySection()
        );

        // Creating complex section
        List<ComplexSection> complexSections = Arrays.asList(
            generateCandidateAnalysisSection()
        );

        // Creating metadata
        ReportMetadata metadata = new ReportMetadata(
            "1.0",
            now,
            "Elasticsearch + Qdrant Vector Database",
            Map.of(
                "totalQueries", 8,
                "averageQueryTime", "150ms",
                "dataFreshness", "Real-time",
                "filtersApplied", "location, skills, experience, salary"
            ),
            simpleSections.size(),
            complexSections.size(),
            Map.of(
                "reportType", "Candidate Analysis",
                "dataRange", "All candidates",
                "vectorSearchEnabled", true
            )
        );

        return new Report(
            UUID.randomUUID().toString(),
            reportTitle,
            "Kompletan izveštaj o kandidatima sa analizom lokacija, veština i performansi",
            now,
            generatedBy,
            simpleSections,
            complexSections,
            metadata
        );
    }

    /**
     * Basic section 1: Candidates by location (with optional filtering)
     */
    private SimpleSection generateCandidatesByLocationSection() {
        List<Candidate> allCandidates = candidateService.findAll();
        
        // OPTIONAL FILTERING: Filter candidates with location
        List<Candidate> filteredCandidates = allCandidates.stream()
            .filter(candidate -> candidate.getLocation() != null && !candidate.getLocation().trim().isEmpty())
            .collect(Collectors.toList());
        
        // Grouping by location (using filtered candidates)
        Map<String, Long> candidatesByLocation = filteredCandidates.stream()
            .collect(Collectors.groupingBy(
                Candidate::getLocation,
                Collectors.counting()
            ));

        // Converting to format suitable for display
        List<Map<String, Object>> data = candidatesByLocation.entrySet().stream()
            .map(entry -> {
                Map<String, Object> record = new HashMap<>();
                record.put("location", entry.getKey());
                record.put("candidateCount", entry.getValue());
                return record;
            })
            .sorted((a, b) -> Long.compare(
                (Long) b.get("candidateCount"), 
                (Long) a.get("candidateCount")
            ))
            .collect(Collectors.toList());

        // Configuration for bar chart
        Map<String, Object> chartConfig = Map.of(
            "type", "bar",
            "xAxis", "location",
            "yAxis", "candidateCount",
            "title", "Broj kandidata po lokaciji",
            "colors", Arrays.asList("#3498db", "#e74c3c", "#2ecc71", "#f39c12")
        );

        return new SimpleSection(
            "location-analysis",
            "Analiza kandidata po lokaciji",
            "Prikaz broja kandidata grupisano po lokaciji (filtrirano: samo kandidati sa lokacijom)",
            "location_grouping",
            Map.of(
                "groupBy", "location",
                "filterApplied", "candidates_with_location_only",
                "totalCandidates", allCandidates.size(),
                "filteredCandidates", filteredCandidates.size()
            ),
            data,
            filteredCandidates.size(),
            "chart",
            chartConfig
        );
    }

    /**
     * Basic section 2: Candidates by skills (with optional filtering)
     */
    private SimpleSection generateCandidatesBySkillsSection() {
        List<Candidate> allCandidates = candidateService.findAll();
        
        // OPTIONAL FILTERING: Filter candidates with skills
        List<Candidate> filteredCandidates = allCandidates.stream()
            .filter(candidate -> candidate.getSkills() != null && !candidate.getSkills().isEmpty())
            .collect(Collectors.toList());
        
        // Collecting all skills (using filtered candidates)
        Map<String, Long> skillsCount = filteredCandidates.stream()
            .flatMap(candidate -> candidate.getSkills().stream())
            .collect(Collectors.groupingBy(
                skill -> skill,
                Collectors.counting()
            ));

        // Top 10 skills
        List<Map<String, Object>> data = skillsCount.entrySet().stream()
            .map(entry -> {
                Map<String, Object> record = new HashMap<>();
                record.put("skill", entry.getKey());
                record.put("candidateCount", entry.getValue());
                return record;
            })
            .sorted((a, b) -> Long.compare(
                (Long) b.get("candidateCount"), 
                (Long) a.get("candidateCount")
            ))
            .limit(10)
            .collect(Collectors.toList());

        // Konfiguracija za pie chart
        Map<String, Object> chartConfig = Map.of(
            "type", "pie",
            "labelField", "skill",
            "valueField", "candidateCount",
            "title", "Top 10 veština kandidata",
            "showLegend", true
        );

        return new SimpleSection(
            "skills-analysis",
            "Analiza veština kandidata",
            "Prikaz najpopularnijih veština među kandidatima (filtrirano: samo kandidati sa veštinama)",
            "skills_grouping",
            Map.of(
                "topSkills", 10,
                "filterApplied", "candidates_with_skills_only",
                "totalCandidates", allCandidates.size(),
                "filteredCandidates", filteredCandidates.size()
            ),
            data,
            filteredCandidates.size(),
            "chart",
            chartConfig
        );
    }

    /**
     * Basic section 3: Candidates by years of experience (with optional filtering)
     */
    private SimpleSection generateCandidatesByExperienceSection() {
        List<Candidate> allCandidates = candidateService.findAll();
        
        // OPTIONAL FILTERING: Filter candidates with experience
        List<Candidate> filteredCandidates = allCandidates.stream()
            .filter(candidate -> candidate.getYearsOfExperience() != null && candidate.getYearsOfExperience() >= 0)
            .collect(Collectors.toList());
        
        // Grouping by years of experience
        Map<String, Long> experienceGroups = filteredCandidates.stream()
            .collect(Collectors.groupingBy(
                candidate -> {
                    int exp = candidate.getYearsOfExperience();
                    if (exp < 2) return "0-2 godine";
                    else if (exp < 5) return "2-5 godina";
                    else if (exp < 10) return "5-10 godina";
                    else return "10+ godina";
                },
                Collectors.counting()
            ));

        List<Map<String, Object>> data = experienceGroups.entrySet().stream()
            .map(entry -> {
                Map<String, Object> record = new HashMap<>();
                record.put("experienceRange", entry.getKey());
                record.put("candidateCount", entry.getValue());
                return record;
            })
            .sorted((a, b) -> Long.compare(
                (Long) b.get("candidateCount"), 
                (Long) a.get("candidateCount")
            ))
            .collect(Collectors.toList());

        Map<String, Object> chartConfig = Map.of(
            "type", "bar",
            "xAxis", "experienceRange",
            "yAxis", "candidateCount",
            "title", "Distribucija godina iskustva",
            "colors", Arrays.asList("#9b59b6", "#e67e22", "#1abc9c", "#34495e")
        );

        return new SimpleSection(
            "experience-analysis",
            "Analiza kandidata po godinama iskustva",
            "Prikaz distribucije kandidata po godinama iskustva (filtrirano: samo kandidati sa iskustvom)",
            "experience_grouping",
            Map.of(
                "groupBy", "yearsOfExperience",
                "filterApplied", "candidates_with_experience_only",
                "totalCandidates", allCandidates.size(),
                "filteredCandidates", filteredCandidates.size()
            ),
            data,
            filteredCandidates.size(),
            "chart",
            chartConfig
        );
    }

    /**
     * Basic section 4: Candidates by expected salary (with optional filtering)
     */
    private SimpleSection generateCandidatesBySalarySection() {
        List<Candidate> allCandidates = candidateService.findAll();
        
        // OPTIONAL FILTERING: Filter candidates with expected salary
        List<Candidate> filteredCandidates = allCandidates.stream()
            .filter(candidate -> candidate.getExpectedSalary() != null && candidate.getExpectedSalary() > 0)
            .collect(Collectors.toList());
        
        // Grouping by expected salary
        Map<String, Long> salaryGroups = filteredCandidates.stream()
            .collect(Collectors.groupingBy(
                candidate -> {
                    double salary = candidate.getExpectedSalary();
                    if (salary < 50000) return "< 50k";
                    else if (salary < 100000) return "50k-100k";
                    else if (salary < 150000) return "100k-150k";
                    else return "150k+";
                },
                Collectors.counting()
            ));

        List<Map<String, Object>> data = salaryGroups.entrySet().stream()
            .map(entry -> {
                Map<String, Object> record = new HashMap<>();
                record.put("salaryRange", entry.getKey());
                record.put("candidateCount", entry.getValue());
                return record;
            })
            .sorted((a, b) -> Long.compare(
                (Long) b.get("candidateCount"), 
                (Long) a.get("candidateCount")
            ))
            .collect(Collectors.toList());

        Map<String, Object> chartConfig = Map.of(
            "type", "pie",
            "labelField", "salaryRange",
            "valueField", "candidateCount",
            "title", "Distribucija očekivane plate",
            "showLegend", true
        );

        return new SimpleSection(
            "salary-analysis",
            "Analiza kandidata po očekivanoj plati",
            "Prikaz distribucije kandidata po očekivanoj plati (filtrirano: samo kandidati sa platom)",
            "salary_grouping",
            Map.of(
                "groupBy", "expectedSalary",
                "filterApplied", "candidates_with_salary_only",
                "totalCandidates", allCandidates.size(),
                "filteredCandidates", filteredCandidates.size()
            ),
            data,
            filteredCandidates.size(),
            "chart",
            chartConfig
        );
    }

    /**
     * Complex section: Comprehensive candidate analysis
     */
    private ComplexSection generateCandidateAnalysisSection() {
        List<Candidate> allCandidates = candidateService.findAll();
        
        // Aggregated statistics
        Map<String, Object> statistics = calculateCandidateStatistics(allCandidates);
        
        // Detailed data for top candidates
        List<Map<String, Object>> detailedData = allCandidates.stream()
            .filter(candidate -> candidate.getMatchScore() != null)
            .sorted((a, b) -> Double.compare(
                b.getMatchScore() != null ? b.getMatchScore() : 0.0,
                a.getMatchScore() != null ? a.getMatchScore() : 0.0
            ))
            .limit(20)
            .map(candidate -> {
                Map<String, Object> record = new HashMap<>();
                record.put("id", candidate.getId());
                record.put("name", (candidate.getFirstName() != null ? candidate.getFirstName() : "") + 
                                   (candidate.getLastName() != null ? " " + candidate.getLastName() : ""));
                record.put("email", candidate.getEmail());
                record.put("location", candidate.getLocation());
                record.put("yearsOfExperience", candidate.getYearsOfExperience());
                record.put("expectedSalary", candidate.getExpectedSalary());
                record.put("matchScore", candidate.getMatchScore());
                record.put("skillsCount", candidate.getSkills() != null ? candidate.getSkills().size() : 0);
                return record;
            })
            .collect(Collectors.toList());

        // Aggregated data by categories
        List<Map<String, Object>> aggregatedData = Arrays.asList(
            createAggregatedData("experience", "Godine iskustva", allCandidates),
            createAggregatedData("salary", "Očekivana plata", allCandidates),
            createAggregatedData("skills", "Broj veština", allCandidates)
        );

        // Configuration for dashboard
        Map<String, Object> chartConfig = Map.of(
            "type", "dashboard",
            "layout", "2x2",
            "charts", Arrays.asList(
                Map.of("type", "histogram", "field", "yearsOfExperience", "title", "Distribucija godina iskustva"),
                Map.of("type", "scatter", "xField", "yearsOfExperience", "yField", "expectedSalary", "title", "Iskustvo vs Plata"),
                Map.of("type", "bar", "field", "skillsCount", "title", "Broj veština po kandidatu"),
                Map.of("type", "pie", "field", "location", "title", "Distribucija po lokaciji")
            )
        );

        return new ComplexSection(
            "candidate-analysis",
            "Kompleksna analiza kandidata",
            "Detaljna analiza performansi, veština i karakteristika kandidata",
            "complex_analysis",
            Map.of(
                "analysisType", "comprehensive",
                "includeVectorScores", true,
                "topCandidates", 20
            ),
            aggregatedData,
            detailedData,
            statistics,
            "complex_chart",
            chartConfig,
            Arrays.asList("location_grouping", "skills_grouping", "vector_search")
        );
    }

    /**
     * Calculates basic statistics about candidates
     */
    private Map<String, Object> calculateCandidateStatistics(List<Candidate> candidates) {
        if (candidates.isEmpty()) {
            return Map.of();
        }

        // Years of experience
        List<Integer> experienceYears = candidates.stream()
            .filter(c -> c.getYearsOfExperience() != null)
            .map(Candidate::getYearsOfExperience)
            .collect(Collectors.toList());

        // Expected salaries
        List<Double> salaries = candidates.stream()
            .filter(c -> c.getExpectedSalary() != null)
            .map(Candidate::getExpectedSalary)
            .collect(Collectors.toList());

        // Number of skills
        List<Integer> skillsCounts = candidates.stream()
            .map(c -> c.getSkills() != null ? c.getSkills().size() : 0)
            .collect(Collectors.toList());

        Map<String, Object> stats = new HashMap<>();
        
        // Statistics for years of experience
        if (!experienceYears.isEmpty()) {
            stats.put("experience", Map.of(
                "min", Collections.min(experienceYears),
                "max", Collections.max(experienceYears),
                "avg", experienceYears.stream().mapToInt(Integer::intValue).average().orElse(0.0),
                "count", experienceYears.size()
            ));
        }

        // Statistics for salaries
        if (!salaries.isEmpty()) {
            stats.put("salary", Map.of(
                "min", Collections.min(salaries),
                "max", Collections.max(salaries),
                "avg", salaries.stream().mapToDouble(Double::doubleValue).average().orElse(0.0),
                "count", salaries.size()
            ));
        }

        // Statistics for skills
        if (!skillsCounts.isEmpty()) {
            stats.put("skills", Map.of(
                "min", Collections.min(skillsCounts),
                "max", Collections.max(skillsCounts),
                "avg", skillsCounts.stream().mapToInt(Integer::intValue).average().orElse(0.0),
                "totalUniqueSkills", candidates.stream()
                    .filter(c -> c.getSkills() != null)
                    .flatMap(c -> c.getSkills().stream())
                    .distinct()
                    .count()
            ));
        }

        // General statistics
        stats.put("totalCandidates", candidates.size());
        stats.put("candidatesWithLocation", candidates.stream()
            .filter(c -> c.getLocation() != null && !c.getLocation().trim().isEmpty())
            .count());
        stats.put("candidatesWithSkills", candidates.stream()
            .filter(c -> c.getSkills() != null && !c.getSkills().isEmpty())
            .count());

        return stats;
    }

    /**
     * Creates aggregated data for a given category
     */
    private Map<String, Object> createAggregatedData(String category, String title, List<Candidate> candidates) {
        Map<String, Object> data = new HashMap<>();
        data.put("category", category);
        data.put("title", title);
        
        switch (category) {
            case "experience":
                Map<String, Long> experienceGroups = candidates.stream()
                    .filter(c -> c.getYearsOfExperience() != null)
                    .collect(Collectors.groupingBy(
                        c -> {
                            int exp = c.getYearsOfExperience();
                            if (exp < 2) return "0-2 godine";
                            else if (exp < 5) return "2-5 godina";
                            else if (exp < 10) return "5-10 godina";
                            else return "10+ godina";
                        },
                        Collectors.counting()
                    ));
                data.put("groups", experienceGroups);
                break;
                
            case "salary":
                Map<String, Long> salaryGroups = candidates.stream()
                    .filter(c -> c.getExpectedSalary() != null)
                    .collect(Collectors.groupingBy(
                        c -> {
                            double salary = c.getExpectedSalary();
                            if (salary < 50000) return "< 50k";
                            else if (salary < 100000) return "50k-100k";
                            else if (salary < 150000) return "100k-150k";
                            else return "150k+";
                        },
                        Collectors.counting()
                    ));
                data.put("groups", salaryGroups);
                break;
                
            case "skills":
                Map<String, Long> skillsGroups = candidates.stream()
                    .collect(Collectors.groupingBy(
                        c -> {
                            int skillCount = c.getSkills() != null ? c.getSkills().size() : 0;
                            if (skillCount == 0) return "Bez veština";
                            else if (skillCount < 5) return "1-4 veštine";
                            else if (skillCount < 10) return "5-9 veština";
                            else return "10+ veština";
                        },
                        Collectors.counting()
                    ));
                data.put("groups", skillsGroups);
                break;
        }
        
        return data;
    }

    /**
     * Generates report based on candidate search   
     */
    public Report generateSearchBasedReport(CandidateSearchRequest searchRequest, String generatedBy) {
        LocalDateTime now = LocalDateTime.now();
        
        // Performing search
        List<Candidate> searchResults = candidateService.searchCandidatesWithVectorAndFilters(searchRequest);
        
        // Creating simple sections based on search results
        List<SimpleSection> simpleSections = Arrays.asList(
            generateSearchResultsSection(searchResults, searchRequest),
            generateSearchFiltersSection(searchRequest)
        );

        // Creating complex section for analysis of results
        List<ComplexSection> complexSections = Arrays.asList(
            generateSearchAnalysisSection(searchResults, searchRequest)
        );

        // Metadata
        ReportMetadata metadata = new ReportMetadata(
            "1.0",
            now,
            "Elasticsearch + Qdrant Vector Database",
            Map.of(
                "searchQuery", searchRequest.getSearchText() != null ? searchRequest.getSearchText() : "N/A",
                "filtersApplied", searchRequest.getRequiredSkills() != null ? searchRequest.getRequiredSkills().size() : 0,
                "resultsCount", searchResults.size(),
                "queryTime", "Real-time"
            ),
            simpleSections.size(),
            complexSections.size(),
            Map.of(
                "reportType", "Search Results Analysis",
                "searchType", "Vector + Filter Search",
                "vectorSearchEnabled", true
            )
        );

        return new Report(
            UUID.randomUUID().toString(),
            "Izveštaj pretrage kandidata",
            "Izveštaj na osnovu kriterija pretrage kandidata",
            now,
            generatedBy,
            simpleSections,
            complexSections,
            metadata
        );
    }

    private SimpleSection generateSearchResultsSection(List<Candidate> results, CandidateSearchRequest request) {
        List<Map<String, Object>> data = results.stream()
            .map(candidate -> {
                Map<String, Object> record = new HashMap<>();
                record.put("id", candidate.getId());
                record.put("name", (candidate.getFirstName() != null ? candidate.getFirstName() : "") + 
                                   (candidate.getLastName() != null ? " " + candidate.getLastName() : ""));
                record.put("email", candidate.getEmail());
                record.put("location", candidate.getLocation());
                record.put("yearsOfExperience", candidate.getYearsOfExperience());
                record.put("expectedSalary", candidate.getExpectedSalary());
                record.put("matchScore", candidate.getMatchScore());
                record.put("skills", candidate.getSkills());
                return record;
            })
            .collect(Collectors.toList());

        return new SimpleSection(
            "search-results",
            "Rezultati pretrage",
            "Kandidati koji odgovaraju kriterijima pretrage",
            "vector_filter_search",
            Map.of(
                "searchText", request.getSearchText(),
                "location", request.getLocation(),
                "minExperience", request.getMinExperience(),
                "maxSalary", request.getMaxExpectedSalary()
            ),
            data,
            results.size(),
            "table",
            null
        );
    }

    private SimpleSection generateSearchFiltersSection(CandidateSearchRequest request) {
        Map<String, Object> filters = new HashMap<>();
        if (request.getRequiredSkills() != null) {
            filters.put("requiredSkills", request.getRequiredSkills());
        }
        if (request.getLocation() != null) {
            filters.put("location", request.getLocation());
        }
        if (request.getMinExperience() != null) {
            filters.put("minExperience", request.getMinExperience());
        }
        if (request.getMaxExpectedSalary() != null) {
            filters.put("maxExpectedSalary", request.getMaxExpectedSalary());
        }

        return new SimpleSection(
            "search-filters",
            "Primenjeni filteri",
            "Kriteriji koji su korišćeni u pretrazi",
            "filter_analysis",
            filters,
            Arrays.asList(filters),
            1,
            "list",
            null
        );
    }

    private ComplexSection generateSearchAnalysisSection(List<Candidate> results, CandidateSearchRequest request) {
        Map<String, Object> statistics = calculateCandidateStatistics(results);
        
        // Analysis of search results
        Map<String, Object> searchAnalysis = new HashMap<>();
        searchAnalysis.put("totalResults", results.size());
        searchAnalysis.put("averageMatchScore", results.stream()
            .filter(c -> c.getMatchScore() != null)
            .mapToDouble(Candidate::getMatchScore)
            .average()
            .orElse(0.0));
        searchAnalysis.put("topMatchScore", results.stream()
            .filter(c -> c.getMatchScore() != null)
            .mapToDouble(Candidate::getMatchScore)
            .max()
            .orElse(0.0));

        return new ComplexSection(
            "search-analysis",
            "Analiza rezultata pretrage",
            "Detaljna analiza rezultata pretrage kandidata",
            "search_analysis",
            Map.of("searchRequest", request),
            Arrays.asList(searchAnalysis),
            results.stream().map(candidate -> {
                Map<String, Object> record = new HashMap<>();
                record.put("id", candidate.getId());
                record.put("name", (candidate.getFirstName() != null ? candidate.getFirstName() : "") + 
                                   (candidate.getLastName() != null ? " " + candidate.getLastName() : ""));
                record.put("matchScore", candidate.getMatchScore());
                record.put("relevance", candidate.getMatchScore() != null && candidate.getMatchScore() > 0.7 ? "Visoka" : "Srednja");
                return record;
            }).collect(Collectors.toList()),
            statistics,
            "dashboard",
            Map.of("type", "search_dashboard"),
            Arrays.asList("vector_search", "filter_analysis")
        );
    }
}
