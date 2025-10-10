package com.itcompany.recruitment.config;

import com.itcompany.recruitment.service.TestDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "data.loader.enabled", havingValue = "true", matchIfMissing = true)
public class DataLoader {

    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);

    private final TestDataService testDataService;

    public DataLoader(TestDataService testDataService) {
        this.testDataService = testDataService;
    }

    @EventListener
    public void handleContextRefresh(ContextRefreshedEvent event) {
        // Adding delay to wait for databases to initialize
        new Thread(() -> {
            try {
                logger.info("Waiting for databases to initialize...");
                Thread.sleep(15000); // 15 seconds delay for better stability
                
                logger.info("Starting data loading...");
                testDataService.loadTestData();
                logger.info("Data loading completed successfully!");
            } catch (Exception e) {
                logger.error("Error during data loading: ", e);
            }
        }).start();
    }
}
