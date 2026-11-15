package com.moa.moa_be.domain.analysis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml 의 analysis.* 설정값.
 * API 키는 환경변수(GOOGLE_VISION_API_KEY, OPENAI_API_KEY)로 주입.
 */
@ConfigurationProperties(prefix = "analysis")
public record AnalysisProperties(Vision vision, OpenAi openai) {

    public record Vision(String apiKey, String baseUrl) {
        public Vision {
            if (baseUrl == null || baseUrl.isBlank()) {
                baseUrl = "https://vision.googleapis.com";
            }
        }
    }

    public record OpenAi(String apiKey, String model, String baseUrl) {
        public OpenAi {
            if (model == null || model.isBlank()) {
                model = "gpt-4.1-mini";
            }
            if (baseUrl == null || baseUrl.isBlank()) {
                baseUrl = "https://api.openai.com";
            }
        }
    }
}
