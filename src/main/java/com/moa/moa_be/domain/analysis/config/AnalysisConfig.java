package com.moa.moa_be.domain.analysis.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AnalysisProperties.class)
public class AnalysisConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration OCR_READ_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration LLM_READ_TIMEOUT = Duration.ofSeconds(45);

    @Bean
    public RestClient visionRestClient(AnalysisProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.vision().baseUrl())
                .requestFactory(requestFactory(OCR_READ_TIMEOUT))
                .build();
    }

    @Bean
    public RestClient openAiRestClient(AnalysisProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.openai().baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.openai().apiKey())
                .requestFactory(requestFactory(LLM_READ_TIMEOUT))
                .build();
    }

    private JdkClientHttpRequestFactory requestFactory(Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);
        return factory;
    }
}
