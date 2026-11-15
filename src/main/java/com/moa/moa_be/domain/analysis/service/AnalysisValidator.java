package com.moa.moa_be.domain.analysis.service;

import com.moa.moa_be.domain.analysis.dto.AnalysisResult;
import com.moa.moa_be.domain.analysis.dto.AnalysisResult.DeadlineStatus;
import com.moa.moa_be.domain.analysis.llm.LlmAnalysis;
import com.moa.moa_be.domain.analysis.ocr.OcrResult;
import com.moa.moa_be.domain.category.entity.DefaultCategory;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * LLM 응답을 그대로 믿지 않고 명세서의 "서버 검증 규칙"대로 정리한다.
 *
 * <pre>
 * category 가 목록에 없음 / 확신도 0.6 미만   → "기타"
 * 날짜 형식이 틀리거나 존재하지 않는 날짜       → deadline UNCLEAR, date null
 * 값이 근거 블록 글자에 없음 (지어낸 값)        → value null
 * links 의 URL 이 OCR 글자에 없음              → 버림
 * title 20자 초과                             → 20자로 자름
 * 좌표는 LLM 이 아니라 source_block_ids 의 블록 box 를 합쳐서 서버가 채움
 * </pre>
 */
@Component
public class AnalysisValidator {

    static final double MIN_CATEGORY_CONFIDENCE = 0.6;
    static final int MAX_TITLE_LENGTH = 20;
    static final int MAX_CHECKLIST_SIZE = 5;
    static final int MAX_FACTS = 6;
    static final int MAX_NULL_FACTS = 2;
    /** 게시물 메타 정보(게시일·조회수 등)는 실천 정보가 아님 */
    private static final java.util.regex.Pattern META_LABEL =
            java.util.regex.Pattern.compile("(게시|작성|업로드|조회|좋아요|리트윗|댓글|팔로)");
    /** 경로 없이 플랫폼 도메인만 있는 링크는 앱 UI 표시라서 버림 (예: "X.com") */
    private static final java.util.Set<String> PLATFORM_DOMAINS = java.util.Set.of(
            "x.com", "twitter.com", "instagram.com", "www.instagram.com", "youtube.com", "www.youtube.com",
            "threads.net", "www.threads.net", "facebook.com", "www.facebook.com", "tiktok.com", "www.tiktok.com");
    /** 원·₩·$ 등 화폐 표시 (좋아요 수 같은 숫자를 가격으로 착각하는 것 방지) */
    private static final java.util.regex.Pattern CURRENCY =
            java.util.regex.Pattern.compile("(원|₩|\\$|￦|krw|won|달러|만원|천원|usd|€|¥|엔)", java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final String OTHER = DefaultCategory.OTHER.getDisplayName();

    public AnalysisResult validate(LlmAnalysis raw, OcrResult ocr, List<String> categories) {
        return validate(raw, ocr, categories, false);
    }

    /**
     * @param imageSent 이미지도 함께 보냈는지. 이때도 facts 값은 OCR 글자 기준으로 검증한다.
     */
    public AnalysisResult validate(LlmAnalysis raw, OcrResult ocr, List<String> categories, boolean imageSent) {
        Map<String, OcrResult.Block> blocks = ocr.blocks().stream()
                .collect(Collectors.toMap(OcrResult.Block::id, Function.identity(), (a, b) -> a));
        String ocrText = normalize(ocr.fullText());

        String category = validCategory(raw.category(), raw.categoryConfidence(), categories);
        double confidence = raw.categoryConfidence() == null ? 0 : clamp(raw.categoryConfidence());

        return new AnalysisResult(
                validDeadline(raw.deadline(), blocks),
                category,
                confidence,
                validTitle(raw.title(), raw.summary(), ocr),
                blankToNull(raw.summary()),
                validFacts(raw.facts(), blocks, ocrText),
                validGuide(raw.guide(), ocrText),
                validChecklist(raw.checklist())
        );
    }

    // ---- category ----

    private String validCategory(String category, Double confidence, List<String> categories) {
        if (category == null || !categories.contains(category)) {
            return OTHER;
        }
        if (confidence == null || confidence < MIN_CATEGORY_CONFIDENCE) {
            return OTHER;
        }
        return category;
    }

    // ---- deadline ----

    private AnalysisResult.Deadline validDeadline(LlmAnalysis.Deadline raw, Map<String, OcrResult.Block> blocks) {
        if (raw == null) {
            return new AnalysisResult.Deadline(DeadlineStatus.NONE, null, null, null);
        }
        double[] box = mergeBoxes(raw.sourceBlockIds(), blocks);
        String label = blankToNull(raw.label());
        DeadlineStatus status = parseStatus(raw.status());

        if (status == DeadlineStatus.FOUND) {
            LocalDate date = parseDate(raw.date());
            if (date == null) {
                // 날짜 형식이 틀리거나 존재하지 않는 날짜 → 임의로 정하지 않음
                return new AnalysisResult.Deadline(DeadlineStatus.UNCLEAR, null, label, box);
            }
            return new AnalysisResult.Deadline(DeadlineStatus.FOUND, date, label, box);
        }
        if (status == DeadlineStatus.UNCLEAR) {
            return new AnalysisResult.Deadline(DeadlineStatus.UNCLEAR, null, label, box);
        }
        return new AnalysisResult.Deadline(DeadlineStatus.NONE, null, null, null);
    }

    private DeadlineStatus parseStatus(String status) {
        if (status == null) {
            return DeadlineStatus.NONE;
        }
        try {
            return DeadlineStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return DeadlineStatus.UNCLEAR;
        }
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date.trim()); // YYYY-MM-DD, 2월 30일 같은 날짜는 예외
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // ---- title / summary ----

    private String validTitle(String title, String summary, OcrResult ocr) {
        String candidate = blankToNull(title);
        if (candidate == null) {
            candidate = blankToNull(summary);
        }
        if (candidate == null && ocr.hasText()) {
            candidate = ocr.blocks().get(0).text();
        }
        if (candidate == null) {
            candidate = "제목 없음";
        }
        return cut(candidate, MAX_TITLE_LENGTH);
    }

    // ---- facts ----

    private List<AnalysisResult.Fact> validFacts(List<LlmAnalysis.Fact> rawFacts,
                                                 Map<String, OcrResult.Block> blocks,
                                                 String ocrText) {
        if (rawFacts == null) {
            return List.of();
        }
        List<AnalysisResult.Fact> facts = new ArrayList<>();
        java.util.Set<String> seenKeys = new java.util.HashSet<>();
        int nullCount = 0;
        for (LlmAnalysis.Fact raw : rawFacts) {
            if (facts.size() >= MAX_FACTS) {
                break;
            }
            if (raw == null || blankToNull(raw.key()) == null || !seenKeys.add(raw.key().trim())) {
                continue;
            }
            if (raw.label() != null && META_LABEL.matcher(raw.label()).find()) {
                continue; // 게시일·조회수 같은 SNS 메타 정보
            }
            String value = blankToNull(raw.value());
            double[] box = null;

            if (value != null) {
                String sourceText = normalize(sourceText(raw.sourceBlockIds(), blocks));
                String normalizedValue = normalize(value);
                if (!sourceText.isEmpty() && sourceText.contains(normalizedValue)) {
                    box = mergeBoxes(raw.sourceBlockIds(), blocks);
                } else if (!ocrText.contains(normalizedValue)) {
                    value = null; // 사진 어디에도 없는 값 → 지어낸 값
                }
                // 근거 블록 ID 만 틀리고 글자는 사진에 있으면 값은 살리고 box 만 비움

                if (value != null && "price".equalsIgnoreCase(raw.key().trim())
                        && !CURRENCY.matcher(value).find()) {
                    continue; // 화폐 표시 없는 숫자 (좋아요·조회수 등)는 가격으로 잘못 읽은 것 → 항목 제외
                }
            }
            if (value == null) {
                if (nullCount >= MAX_NULL_FACTS) {
                    continue; // "직접 확인 필요" 항목은 최대 2개까지만
                }
                nullCount++;
            }
            facts.add(new AnalysisResult.Fact(raw.key().trim(),
                    blankToNull(raw.label()) == null ? raw.key().trim() : raw.label().trim(),
                    value, box));
        }
        return facts;
    }

    // ---- guide / checklist ----

    private AnalysisResult.Guide validGuide(LlmAnalysis.Guide raw, String ocrText) {
        if (raw == null) {
            return null;
        }
        List<String> lines = cleanList(raw.lines());
        if (lines.isEmpty()) {
            return null; // 실천 방법을 만들지 못함
        }
        List<String> links = cleanList(raw.links()).stream()
                .filter(link -> ocrText.contains(normalizeUrl(link)))
                .filter(link -> !PLATFORM_DOMAINS.contains(normalizeUrl(link)))
                .toList();
        String title = blankToNull(raw.title()) == null ? "실천 가이드" : raw.title().trim();
        return new AnalysisResult.Guide(title, lines, cleanList(raw.preparations()), links);
    }

    private List<String> validChecklist(List<String> raw) {
        return cleanList(raw).stream().limit(MAX_CHECKLIST_SIZE).toList();
    }

    // ---- box ----

    /** 여러 블록 box 를 감싸는 하나의 box. 근거가 없으면 null. */
    private double[] mergeBoxes(List<String> ids, Map<String, OcrResult.Block> blocks) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        double minX = 1, minY = 1, maxX = 0, maxY = 0;
        boolean found = false;
        for (String id : ids) {
            OcrResult.Block block = blocks.get(id);
            if (block == null || block.box() == null) {
                continue;
            }
            double[] b = block.box();
            minX = Math.min(minX, b[0]);
            minY = Math.min(minY, b[1]);
            maxX = Math.max(maxX, b[0] + b[2]);
            maxY = Math.max(maxY, b[1] + b[3]);
            found = true;
        }
        if (!found) {
            return null;
        }
        return new double[]{round(minX), round(minY), round(maxX - minX), round(maxY - minY)};
    }

    // ---- helpers ----

    private String sourceText(List<String> ids, Map<String, OcrResult.Block> blocks) {
        if (ids == null) {
            return "";
        }
        return ids.stream()
                .map(blocks::get)
                .filter(Objects::nonNull)
                .map(OcrResult.Block::text)
                .collect(Collectors.joining(" "));
    }

    /** 비교용: 공백 제거 + 소문자 ("12,000 원" == "12,000원") */
    static String normalize(String text) {
        return text == null ? "" : text.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    /** URL 비교용: 스킴과 끝 슬래시 제거 */
    static String normalizeUrl(String url) {
        return normalize(url).replaceFirst("^https?://", "").replaceAll("/+$", "");
    }

    private static List<String> cleanList(List<String> list) {
        if (list == null) {
            return List.of();
        }
        return list.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String cut(String s, int max) {
        String trimmed = s.trim();
        return trimmed.codePointCount(0, trimmed.length()) <= max
                ? trimmed
                : trimmed.substring(0, trimmed.offsetByCodePoints(0, max));
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }

    private static double round(double v) {
        return Math.round(v * 10000) / 10000.0;
    }
}
