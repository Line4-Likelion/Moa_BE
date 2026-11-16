package com.moa.moa_be.domain.analysis.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.moa.moa_be.domain.analysis.config.AnalysisProperties;
import com.moa.moa_be.domain.analysis.ocr.OcrResult;
import com.moa.moa_be.domain.analysis.service.AnalysisException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.json.JsonMapper;

/**
 * OpenAI Chat Completions + Structured Outputs(json_schema, strict)로 분석.
 * LLM 에는 블록 ID와 글자만 보내고 좌표는 보내지 않는다 (명세서 ②).
 */
@Slf4j
@Component
public class OpenAiAnalysisClient implements AnalysisClient {

    private final RestClient openAiRestClient;
    private final AnalysisProperties properties;
    private final JsonMapper jsonMapper;

    public OpenAiAnalysisClient(@Qualifier("openAiRestClient") RestClient openAiRestClient,
                                AnalysisProperties properties,
                                ObjectProvider<JsonMapper> jsonMapper) {
        this.openAiRestClient = openAiRestClient;
        this.properties = properties;
        this.jsonMapper = jsonMapper.getIfAvailable(() -> JsonMapper.builder().build());
    }

    @Override
    public LlmAnalysis analyze(OcrResult ocr, List<String> categories, LocalDate today, ImageInput image) {
        Map<String, Object> userInput = new LinkedHashMap<>();
        userInput.put("today", today.toString());
        userInput.put("categories", categories);
        userInput.put("blocks", ocr.blocks().stream()
                .map(b -> Map.of("id", b.id(), "text", b.text()))
                .toList());

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", properties.openai().model());
        request.put("messages", List.of(
                Map.of("role", "system", "content", AnalysisPrompt.SYSTEM),
                Map.of("role", "user", "content", userContent(jsonMapper.writeValueAsString(userInput), image))
        ));
        request.put("response_format", AnalysisPrompt.responseFormat(categories));

        ChatResponse response;
        try {
            response = openAiRestClient.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ChatResponse.class);
        } catch (RestClientException e) {
            log.error("OpenAI 호출 실패: {}", e.getMessage());
            throw new AnalysisException("LLM 호출 실패", e);
        }

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new AnalysisException("LLM 응답 없음");
        }
        Message message = response.choices().get(0).message();
        if (message == null || message.refusal() != null) {
            throw new AnalysisException("LLM 이 응답을 거부함: " + (message == null ? null : message.refusal()));
        }
        if (message.content() == null || message.content().isBlank()) {
            throw new AnalysisException("LLM 응답 내용 없음");
        }

        try {
            return jsonMapper.readValue(message.content(), LlmAnalysis.class);
        } catch (RuntimeException e) {
            log.warn("LLM 응답 JSON 파싱 실패: {}", message.content());
            throw new AnalysisException("LLM 응답이 JSON 형식이 아님", e);
        }
    }

    /** 텍스트만 보내거나, 이미지가 있으면 [텍스트, 이미지] 배열로 보냄 */
    private Object userContent(String text, ImageInput image) {
        if (image == null) {
            return text;
        }
        String dataUrl = "data:" + image.mimeType() + ";base64,"
                + Base64.getEncoder().encodeToString(image.bytes());
        return List.of(
                Map.of("type", "text", "text", text),
                // detail=low: 저해상도로 보내서 토큰·비용 절약 (물건 종류 파악용이라 충분)
                Map.of("type", "image_url", "image_url", Map.of("url", dataUrl, "detail", "low"))
        );
    }

    // ---- OpenAI 응답 형식 (필요한 필드만) ----

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatResponse(List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(Message message, @JsonProperty("finish_reason") String finishReason) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String content, String refusal) {
    }
}
