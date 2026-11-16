package com.moa.moa_be.domain.analysis.ocr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moa.moa_be.domain.analysis.config.AnalysisProperties;
import com.moa.moa_be.domain.analysis.service.AnalysisException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Google Cloud Vision TEXT_DETECTION 으로 글자와 위치를 추출.
 * 응답의 fullTextAnnotation 을 문단(paragraph) 단위 블록으로 변환한다.
 */
@Slf4j
@Component
public class GoogleVisionOcrClient implements OcrClient {

    private final RestClient visionRestClient;
    private final AnalysisProperties properties;

    public GoogleVisionOcrClient(@Qualifier("visionRestClient") RestClient visionRestClient,
                                 AnalysisProperties properties) {
        this.visionRestClient = visionRestClient;
        this.properties = properties;
    }

    @Override
    public OcrResult read(byte[] image) {
        Map<String, Object> request = Map.of("requests", List.of(Map.of(
                "image", Map.of("content", Base64.getEncoder().encodeToString(image)),
                "features", List.of(Map.of("type", "TEXT_DETECTION")),
                "imageContext", Map.of("languageHints", List.of("ko", "en"))
        )));

        VisionResponse response;
        try {
            response = visionRestClient.post()
                    .uri(uri -> uri.path("/v1/images:annotate")
                            .queryParam("key", properties.vision().apiKey())
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(VisionResponse.class);
        } catch (RestClientException e) {
            log.error("Vision OCR 호출 실패: {}", e.getMessage());
            throw new AnalysisException("OCR 호출 실패", e);
        }

        if (response == null || response.responses() == null || response.responses().isEmpty()) {
            return OcrResult.empty();
        }
        AnnotateResponse result = response.responses().get(0);
        if (result.error() != null && result.error().message() != null) {
            log.error("Vision OCR 오류: {}", result.error().message());
            throw new AnalysisException("OCR 오류: " + result.error().message());
        }
        if (result.fullTextAnnotation() == null || result.fullTextAnnotation().pages() == null
                || result.fullTextAnnotation().pages().isEmpty()) {
            return OcrResult.empty(); // 글자가 없는 사진
        }
        return toOcrResult(result.fullTextAnnotation().pages().get(0));
    }

    private OcrResult toOcrResult(Page page) {
        int width = page.width();
        int height = page.height();
        List<OcrResult.Block> blocks = new ArrayList<>();
        int seq = 1;

        for (Block block : nullSafe(page.blocks())) {
            for (Paragraph paragraph : nullSafe(block.paragraphs())) {
                String text = paragraphText(paragraph).trim();
                if (text.isEmpty()) {
                    continue;
                }
                blocks.add(new OcrResult.Block("b" + seq++, text,
                        toRatioBox(paragraph.boundingBox(), width, height)));
            }
        }
        return new OcrResult(width, height, blocks);
    }

    /** symbol 을 이어 붙이고, detectedBreak 에 따라 띄어쓰기를 넣는다. */
    private String paragraphText(Paragraph paragraph) {
        StringBuilder sb = new StringBuilder();
        for (Word word : nullSafe(paragraph.words())) {
            for (Symbol symbol : nullSafe(word.symbols())) {
                sb.append(symbol.text());
                String breakType = symbol.property() != null && symbol.property().detectedBreak() != null
                        ? symbol.property().detectedBreak().type() : null;
                if (breakType != null) {
                    switch (breakType) {
                        case "SPACE", "SURE_SPACE", "EOL_SURE_SPACE", "LINE_BREAK" -> sb.append(' ');
                        default -> { /* HYPHEN 등은 그대로 붙임 */ }
                    }
                }
            }
        }
        return sb.toString().replaceAll("\\s+", " ");
    }

    /** Vision 꼭짓점(px) → [x, y, w, h] 0~1 비율. 크기를 모르면 null. */
    private double[] toRatioBox(BoundingPoly poly, int width, int height) {
        if (poly == null || poly.vertices() == null || poly.vertices().isEmpty() || width <= 0 || height <= 0) {
            return null;
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = 0, maxY = 0;
        for (Vertex v : poly.vertices()) {
            int x = v.x() == null ? 0 : v.x(); // Vision 은 0인 좌표를 생략함
            int y = v.y() == null ? 0 : v.y();
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        return new double[]{
                round(clamp((double) minX / width)),
                round(clamp((double) minY / height)),
                round(clamp((double) (maxX - minX) / width)),
                round(clamp((double) (maxY - minY) / height))
        };
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }

    private static double round(double v) {
        return Math.round(v * 10000) / 10000.0;
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list == null ? List.of() : list;
    }

    // ---- Vision 응답 형식 (필요한 필드만) ----

    @JsonIgnoreProperties(ignoreUnknown = true)
    record VisionResponse(List<AnnotateResponse> responses) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AnnotateResponse(FullTextAnnotation fullTextAnnotation, Status error) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Status(Integer code, String message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FullTextAnnotation(List<Page> pages, String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Page(int width, int height, List<Block> blocks) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Block(List<Paragraph> paragraphs, BoundingPoly boundingBox) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Paragraph(List<Word> words, BoundingPoly boundingBox) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Word(List<Symbol> symbols) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Symbol(String text, TextProperty property) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TextProperty(DetectedBreak detectedBreak) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DetectedBreak(String type) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BoundingPoly(List<Vertex> vertices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Vertex(Integer x, Integer y) {
    }
}
