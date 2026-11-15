package com.moa.moa_be.domain.capture.service;

import static com.moa.moa_be.domain.capture.service.CaptureTimes.toKst;

import com.moa.moa_be.domain.analysis.dto.AnalysisResult;
import com.moa.moa_be.domain.analysis.entity.AnalysisStatus;
import com.moa.moa_be.domain.analysis.entity.ScreenshotAnalysis;
import com.moa.moa_be.domain.capture.dto.CaptureDetailResponse;
import com.moa.moa_be.domain.capture.dto.CaptureParts.CategoryInfo;
import com.moa.moa_be.domain.capture.dto.CaptureParts.ChecklistItem;
import com.moa.moa_be.domain.capture.dto.CaptureParts.DeadlineInfo;
import com.moa.moa_be.domain.capture.dto.CaptureParts.ErrorBody;
import com.moa.moa_be.domain.capture.dto.CaptureParts.FactItem;
import com.moa.moa_be.domain.capture.dto.CaptureParts.GuideInfo;
import com.moa.moa_be.domain.capture.dto.CaptureParts.LinkItem;
import com.moa.moa_be.domain.capture.dto.CaptureParts.LinkType;
import com.moa.moa_be.domain.capture.dto.CaptureParts.Progress;
import com.moa.moa_be.domain.capture.dto.CaptureSummaryResponse;
import com.moa.moa_be.domain.capture.entity.CaptureStatus;
import com.moa.moa_be.domain.capture.entity.ChecklistCheck;
import com.moa.moa_be.domain.capture.entity.FactOverride;
import com.moa.moa_be.domain.capture.repository.CaptureQueryRepository;
import com.moa.moa_be.domain.capture.repository.CaptureRow;
import com.moa.moa_be.domain.capture.repository.ChecklistCheckRepository;
import com.moa.moa_be.domain.capture.repository.FactOverrideRepository;
import com.moa.moa_be.domain.category.entity.Category;
import com.moa.moa_be.domain.category.repository.CategoryRepository;
import com.moa.moa_be.domain.screenshot.entity.ScreenshotClassification;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * 캡처(A)·분석 결과(B)·사용자 수정(C)을 합쳐 응답 DTO 를 만든다.
 * 분석 내용(제목·카테고리·마감·핵심 정보 등)은 실천 항목(TODO·DONE)에만 내려간다.
 */
@Component
public class CaptureAssembler {

    static final String CHECKLIST_ID_PREFIX = "chk_";

    private static final Set<String> MAP_HOSTS = Set.of(
            "map.naver.com", "m.map.naver.com", "naver.me", "map.kakao.com", "place.map.kakao.com", "kko.to",
            "maps.google.com", "maps.app.goo.gl", "goo.gl");

    private final CategoryRepository categoryRepository;
    private final ChecklistCheckRepository checklistCheckRepository;
    private final FactOverrideRepository factOverrideRepository;
    private final CaptureQueryRepository captureQueryRepository;
    private final JsonMapper jsonMapper;

    public CaptureAssembler(CategoryRepository categoryRepository,
                            ChecklistCheckRepository checklistCheckRepository,
                            FactOverrideRepository factOverrideRepository,
                            CaptureQueryRepository captureQueryRepository,
                            ObjectProvider<JsonMapper> jsonMapper) {
        this.categoryRepository = categoryRepository;
        this.checklistCheckRepository = checklistCheckRepository;
        this.factOverrideRepository = factOverrideRepository;
        this.captureQueryRepository = captureQueryRepository;
        this.jsonMapper = jsonMapper.getIfAvailable(() -> JsonMapper.builder().build());
    }

    // ---- 요약형 ----

    public CaptureSummaryResponse toSummary(Long userId, CaptureRow row) {
        return toSummaries(userId, List.of(row)).get(0);
    }

    public List<CaptureSummaryResponse> toSummaries(Long userId, List<CaptureRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        CategoryLookup categories = categoryLookup(userId, rows);
        List<Long> analyzedIds = rows.stream().filter(CaptureAssembler::hasAnalysisContent)
                .map(r -> r.capture().getId()).toList();
        Map<Long, List<ChecklistCheck>> checks = analyzedIds.isEmpty() ? Map.of()
                : checklistCheckRepository.findAllByCaptureIdIn(analyzedIds).stream()
                .collect(Collectors.groupingBy(c -> c.getCapture().getId()));

        return rows.stream().map(row -> {
            AnalysisResult result = readResult(row);
            Set<Integer> checked = currentCheckedItems(checks.getOrDefault(row.capture().getId(), List.of()),
                    row.analysis());
            return summary(row, result, categories, checked);
        }).toList();
    }

    // ---- 상세형 ----

    public CaptureDetailResponse toDetail(Long userId, CaptureRow row) {
        ScreenshotClassification c = row.capture();
        ScreenshotAnalysis a = row.analysis();
        AnalysisResult result = readResult(row);
        CategoryLookup categories = categoryLookup(userId, List.of(row));
        Set<Integer> checked = result == null ? Set.of()
                : currentCheckedItems(checklistCheckRepository.findAllByCaptureId(c.getId()), a);
        CaptureSummaryResponse s = summary(row, result, categories, checked);

        List<FactItem> facts = List.of();
        List<LinkItem> links = List.of();
        GuideInfo guide = null;
        List<ChecklistItem> checklist = List.of();
        if (result != null) {
            Map<String, String> overrides = currentFactOverrides(c.getId(), a);
            facts = facts(result, overrides);
            if (result.guide() != null) {
                guide = new GuideInfo(result.guide().title(), nullToEmpty(result.guide().lines()),
                        nullToEmpty(result.guide().preparations()));
                links = nullToEmpty(result.guide().links()).stream().map(CaptureAssembler::toLink).toList();
            }
            checklist = checklist(result, checked);
        }
        ErrorBody error = s.status() == CaptureStatus.FAILED
                ? new ErrorBody(a.getErrorCode(), a.getErrorMessage()) : null;

        return new CaptureDetailResponse(s.id(), s.fileKey(), s.status(), s.category(), s.title(), s.summary(),
                s.deadline(), s.checklistProgress(), s.snoozeUntil(), s.dismissed(), s.takenAt(),
                s.createdAt(), s.statusChangedAt(),
                result != null ? a.getCategorySource() : null,
                facts, links, guide, checklist, null, error);
    }

    // ---- 다른 서비스에서 쓰는 도우미 ----

    /** 실천 항목(TODO·DONE)의 검증된 분석 결과. 그 외 상태면 null */
    public AnalysisResult readResult(CaptureRow row) {
        if (!hasAnalysisContent(row) || row.analysis().getResultJson() == null) {
            return null;
        }
        return jsonMapper.readValue(row.analysis().getResultJson(), AnalysisResult.class);
    }

    /** 다시 분석하기 전에 기록된 사용자 수정은 무시 */
    public static boolean isCurrent(LocalDateTime recordedAt, ScreenshotAnalysis analysis) {
        return analysis != null && !recordedAt.isBefore(analysis.getRequestedAt());
    }

    public Set<Integer> currentCheckedItems(Collection<ChecklistCheck> checks, ScreenshotAnalysis analysis) {
        return checks.stream()
                .filter(c -> isCurrent(c.getRecordedAt(), analysis))
                .map(ChecklistCheck::getItemNo)
                .collect(Collectors.toSet());
    }

    public Map<String, String> currentFactOverrides(Long captureId, ScreenshotAnalysis analysis) {
        return factOverrideRepository.findAllByCaptureId(captureId).stream()
                .filter(f -> isCurrent(f.getRecordedAt(), analysis))
                .collect(Collectors.toMap(FactOverride::getFactKey, FactOverride::getValue));
    }

    public static List<ChecklistItem> checklist(AnalysisResult result, Set<Integer> checked) {
        List<String> texts = nullToEmpty(result.checklist());
        return IntStream.range(0, texts.size())
                .mapToObj(i -> new ChecklistItem(CHECKLIST_ID_PREFIX + (i + 1), texts.get(i), checked.contains(i + 1)))
                .toList();
    }

    public static Progress progress(AnalysisResult result, Set<Integer> checked) {
        int total = nullToEmpty(result.checklist()).size();
        if (total == 0) {
            return null;
        }
        int done = (int) checked.stream().filter(n -> n >= 1 && n <= total).count();
        return new Progress(done, total);
    }

    static LinkItem toLink(String url) {
        if (url.regionMatches(true, 0, "tel:", 0, 4)) {
            return new LinkItem(LinkType.PHONE, url.substring(4), url);
        }
        String host = host(url);
        LinkType type = host != null && MAP_HOSTS.contains(host) ? LinkType.MAP : LinkType.WEB;
        return new LinkItem(type, host != null ? host : url, url);
    }

    // ---- 내부 ----

    private static boolean hasAnalysisContent(CaptureRow row) {
        CaptureStatus status = row.status();
        return (status == CaptureStatus.TODO || status == CaptureStatus.DONE)
                && row.analysis().getStatus() == AnalysisStatus.SUCCEEDED;
    }

    private CaptureSummaryResponse summary(CaptureRow row, AnalysisResult result,
                                           CategoryLookup categories, Set<Integer> checked) {
        ScreenshotClassification c = row.capture();
        ScreenshotAnalysis a = row.analysis();
        boolean analyzed = result != null;
        CategoryInfo category = analyzed ? categories.of(a.getCategoryId())
                : row.status() == CaptureStatus.REFERENCE ? categories.ofReference(c.getCategoryId())
                : null;
        return new CaptureSummaryResponse(
                c.getId(),
                c.getFileKey(),
                row.status(),
                category,
                analyzed ? a.getTitle() : null,
                analyzed ? result.summary() : null,
                analyzed ? deadline(a, result) : null,
                analyzed ? progress(result, checked) : null,
                c.getSnoozeUntil(),
                c.isDismissed(),
                null,
                toKst(c.getClassifiedAt()),
                toKst(c.getStatusChangedAt()));
    }

    private static DeadlineInfo deadline(ScreenshotAnalysis a, AnalysisResult result) {
        if (a.getDeadlineStatus() == null) {
            return null;
        }
        // 날짜·상태는 사용자가 고칠 수 있어 컬럼 값, 문구·위치는 AI 원본
        AnalysisResult.Deadline ai = result.deadline();
        return new DeadlineInfo(a.getDeadlineStatus(), a.getDeadlineDate(),
                ai != null ? ai.label() : null, ai != null ? ai.box() : null);
    }

    private static List<FactItem> facts(AnalysisResult result, Map<String, String> overrides) {
        List<FactItem> facts = new ArrayList<>();
        for (AnalysisResult.Fact f : nullToEmpty(result.facts())) {
            String edited = overrides.get(f.key());
            facts.add(new FactItem(f.key(), f.label(), edited != null ? edited : f.value(), f.box(), edited != null));
        }
        return facts;
    }

    private CategoryLookup categoryLookup(Long userId, List<CaptureRow> rows) {
        boolean needed = rows.stream().anyMatch(r -> r.status() == CaptureStatus.REFERENCE
                || (hasAnalysisContent(r) && r.analysis().getCategoryId() != null));
        if (!needed) {
            return new CategoryLookup(Map.of(), Map.of());
        }
        Map<Long, Category> byId = categoryRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
        return new CategoryLookup(byId, captureQueryRepository.countTodoByCategory(userId));
    }

    private record CategoryLookup(Map<Long, Category> byId, Map<Long, Long> todoCounts) {

        /** 실천 항목: 분석 결과의 카테고리 (없거나 지워졌으면 null) */
        CategoryInfo of(Long id) {
            return id == null ? null : info(byId.get(id));
        }

        /** 참고함 항목: 카테고리가 없거나 지워졌으면 "기타"(미분류) */
        CategoryInfo ofReference(Long id) {
            Category category = id == null ? null : byId.get(id);
            if (category == null) {
                category = byId.values().stream().filter(Category::isOther).findFirst().orElse(null);
            }
            return info(category);
        }

        private CategoryInfo info(Category category) {
            return category == null ? null : new CategoryInfo(category.getId(), category.getName(),
                    category.isCustom(), todoCounts.getOrDefault(category.getId(), 0L));
        }
    }

    private static String host(String url) {
        try {
            String withScheme = url.contains("://") ? url : "https://" + url;
            String host = URI.create(withScheme).getHost();
            if (host == null) {
                return null;
            }
            host = host.toLowerCase(Locale.ROOT);
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static <T> List<T> nullToEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }
}
