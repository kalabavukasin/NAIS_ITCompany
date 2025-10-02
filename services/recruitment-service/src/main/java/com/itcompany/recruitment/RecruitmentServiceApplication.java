package com.itcompany.recruitment;

import com.itcompany.recruitment.service.DatabaseSetupService;
import com.itcompany.recruitment.service.TestDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class RecruitmentServiceApplication {

	private static final Logger logger = LoggerFactory.getLogger(RecruitmentServiceApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(RecruitmentServiceApplication.class, args);
	}

	@Bean
	public CommandLineRunner initializeDatabases(
			@Autowired DatabaseSetupService databaseSetupService,
			@Autowired TestDataService testDataService) {
		return args -> {
			logger.info("Starting database initialization...");
			
			// Wait a bit for databases to be ready
			Thread.sleep(10000);
			
			// Initialize databases
			databaseSetupService.initializeDatabases();
			
			// Load test data
			testDataService.loadTestData();
			
			logger.info("Application startup completed!");
		};
	}
}
