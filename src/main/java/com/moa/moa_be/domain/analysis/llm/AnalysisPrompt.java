package com.moa.moa_be.domain.analysis.llm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM 시스템 프롬프트와 Structured Outputs 용 JSON 스키마.
 */
final class AnalysisPrompt {

    private AnalysisPrompt() {
    }

    static final String SYSTEM = """
            너는 사용자가 "나중에 실천하려고" 저장한 휴대폰 스크린샷을 분석하는 도우미야.
            입력은 OCR로 읽은 글자 블록 목록(blocks), 오늘 날짜(today), 고를 수 있는 카테고리 목록(categories)이야.

            규칙:
            1. 주어진 JSON 스키마 형식으로만 답한다.
            2. 사진(blocks)에 없는 정보는 절대 지어내지 말고 null 로 둔다.
            3. 마감기한(deadline):
               - 신청 마감, 마감일, ~까지, 기한, 종료일처럼 '이때까지 해야 하는 날짜'가 분명하면 status=FOUND, date=YYYY-MM-DD.
               - 날짜는 있지만 행사 기간·게시일·영업일 등이라 마감인지 불명확하면 status=UNCLEAR, date=null.
               - 날짜가 없으면 status=NONE, date=null.
               - 연도가 없으면 today 기준으로 가장 가까운 미래 날짜로 해석한다.
            4. category 는 categories 목록 중 하나만 고른다. 애매하면 "기타". category_confidence 는 0~1.
            5. title 은 20자 이내로, 무엇을 실천할지 알 수 있게 쓴다. summary 는 한 문장.
            6. facts 에는 장소(place), 가격(price), 영업시간(hours), 날짜(date), 연락처(contact) 등
               실천에 필요한 정보를 넣는다. value 는 blocks 글자를 그대로 옮기고, 없으면 null.
            7. 모든 값에는 근거가 된 블록 ID 를 source_block_ids 에 넣는다. 근거가 없으면 빈 배열.
            8. guide 는 사용자가 이 스크린샷을 '실천'하기 위한 짧은 가이드다.
               lines 는 1~3문장, preparations 는 준비물, links 는 blocks 에 실제로 적힌 URL 만.
            9. checklist 는 실천 순서대로 2~5개, 각 20자 이내.
            10. 모든 문장은 한국어 해요체로 쓴다.
            """;

    static Map<String, Object> responseFormat(List<String> categories) {
        Map<String, Object> deadline = object(
                "status", Map.of("type", "string", "enum", List.of("FOUND", "UNCLEAR", "NONE")),
                "date", nullableString(),
                "label", nullableString(),
                "source_block_ids", stringArray()
        );
        Map<String, Object> fact = object(
                "key", Map.of("type", "string"),
                "label", Map.of("type", "string"),
                "value", nullableString(),
                "source_block_ids", stringArray()
        );
        Map<String, Object> guide = object(
                "title", Map.of("type", "string"),
                "lines", stringArray(),
                "preparations", stringArray(),
                "links", stringArray()
        );
        Map<String, Object> root = object(
                "deadline", deadline,
                "category", Map.of("type", "string", "enum", categories),
                "category_confidence", Map.of("type", "number"),
                "title", Map.of("type", "string"),
                "summary", nullableString(),
                "facts", Map.of("type", "array", "items", fact),
                "guide", guide,
                "checklist", stringArray()
        );

        return Map.of(
                "type", "json_schema",
                "json_schema", Map.of(
                        "name", "screenshot_analysis",
                        "strict", true,
                        "schema", root
                )
        );
    }

    /** strict 모드: 모든 필드 required + additionalProperties false */
    private static Map<String, Object> object(Object... keyValues) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            properties.put((String) keyValues[i], keyValues[i + 1]);
        }
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.copyOf(properties.keySet()));
        schema.put("additionalProperties", false);
        return schema;
    }

    private static Map<String, Object> nullableString() {
        return Map.of("type", List.of("string", "null"));
    }

    private static Map<String, Object> stringArray() {
        return Map.of("type", "array", "items", Map.of("type", "string"));
    }
}
