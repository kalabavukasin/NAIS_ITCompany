package com.itcompany.recruitment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;

@Configuration
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${elasticsearch.url:http://localhost:9200}")
    private String elasticsearchUrl;

    @Override
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
                .connectedTo(extractHostFromUrl(elasticsearchUrl))
                .withConnectTimeout(30000)
                .withSocketTimeout(30000)
                .build();
    }

    @Bean
    public RestTemplate elasticsearchRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public String elasticsearchUrl() {
        return elasticsearchUrl;
    }

    private String extractHostFromUrl(String url) {
        // Extract host from URL (e.g., "http://elasticsearch:9200" -> "elasticsearch:9200")
        return url.replace("http://", "").replace("https://", "");
    }
}