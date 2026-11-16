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
               실천에 필요한 정보를 넣는다. value 는 blocks 글자를 그대로 옮긴다.
               - 사진에 있는 정보만 넣는다. 단, 이 카테고리를 실천하는 데 꼭 필요한데 사진에 없는 정보
                 (맛집의 영업시간, 행사의 장소 등)는 최대 2개까지 value=null 로 넣어도 된다.
               - 가격(price)은 원·₩·$ 같은 화폐 표시가 있거나 가격임이 분명한 숫자만 넣는다.
               - facts 는 최대 6개.
               - place 에는 사진에서 확인할 수 있는 실제 방문 장소 정보를 넣는다.
               - name 은 매장명, 시설명, 행사장명 등 장소의 이름이다.
               - address 는 사진에 명시된 주소 또는 위치 정보만 넣는다.
               - 동네명, 상권명, 역 이름 등 구체적인 주소가 아닌 정보는 임의로 상세 주소로 바꾸지 않는다.
               - 장소 정보가 없으면 name 과 address 를 모두 null 로 둔다.
            7. 모든 값에는 근거가 된 블록 ID 를 source_block_ids 에 넣는다. 근거가 없으면 빈 배열.
            8. guide 는 사용자가 이 스크린샷을 '실천'하기 위한 짧은 가이드다.
               lines 는 1~3문장, preparations 는 준비물, links 는 blocks 에 실제로 적힌 URL 만.
            9. checklist 는 실천 순서대로 2~5개, 각 20자 이내.
            10. 모든 문장은 한국어 해요체로 쓴다.
            11. SNS·앱 화면의 UI 글자는 내용이 아니므로 무시한다.
                예: 상단 시계·배터리, 조회수, 좋아요·리트윗·댓글·공유 수, "답글 게시하기"·"팔로우" 같은 버튼,
                "X에서 제작됨" 같은 앱 표시, 작성자 아이디·게시 시각. 이런 글자를 facts·마감기한의 근거로 쓰지 않는다.
            12. 이미지가 함께 주어지면 사진 속 물건·장소의 종류를 파악하는 데 사용한다.
                다만 facts 의 value 는 사진에 실제로 적힌 글자만 쓴다.
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
        Map<String, Object> place = object(
                "name", nullableString(),
                "address", nullableString()
        );
        Map<String, Object> root = object(
                "deadline", deadline,
                "category", Map.of("type", "string", "enum", categories),
                "category_confidence", Map.of("type", "number"),
                "title", Map.of("type", "string"),
                "summary", nullableString(),
                "facts", Map.of("type", "array", "items", fact),
                "guide", guide,
                "checklist", stringArray(),
                "place", place
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
