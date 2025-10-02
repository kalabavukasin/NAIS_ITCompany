package com.itcompany.recruitment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class QdrantConfig {

    @Value("${qdrant.url:http://localhost:6333}")
    private String qdrantUrl;

    @Bean
    public RestTemplate qdrantRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public String qdrantUrl() {
        return qdrantUrl;
    }
}
