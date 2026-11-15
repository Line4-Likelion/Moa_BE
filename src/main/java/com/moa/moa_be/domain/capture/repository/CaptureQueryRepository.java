package com.moa.moa_be.domain.capture.repository;

import com.moa.moa_be.domain.analysis.entity.AnalysisStatus;
import com.moa.moa_be.domain.analysis.entity.ScreenshotAnalysis;
import com.moa.moa_be.domain.capture.entity.CaptureStatus;
import com.moa.moa_be.domain.screenshot.entity.Classification;
import com.moa.moa_be.domain.screenshot.entity.ScreenshotClassification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 캡처(screenshot_classifications)와 분석 결과(screenshot_analyses)를 묶어서 조회.
 * 두 테이블은 FK 없이 capture_id 로만 연결돼 있어 JPQL 의 join ... on 을 쓴다.
 */
@Repository
@RequiredArgsConstructor
public class CaptureQueryRepository {

    private static final String FROM = """
            from ScreenshotClassification c
            left join ScreenshotAnalysis a on a.captureId = c.id
            where c.user.id = :userId
            """;

    private final EntityManager em;

    /** 삭제 기록(DELETE 분류)은 없는 것으로 본다 */
    public Optional<CaptureRow> findOne(Long userId, Long captureId) {
        List<Object[]> rows = em.createQuery("select c, a " + FROM + " and c.id = :id and c.classification <> :deleted",
                        Object[].class)
                .setParameter("userId", userId)
                .setParameter("id", captureId)
                .setParameter("deleted", Classification.DELETE)
                .getResultList();
        return rows.stream().findFirst().map(CaptureQueryRepository::toRow);
    }

    public List<CaptureRow> findAllByIds(Long userId, List<Long> captureIds) {
        return em.createQuery("select c, a " + FROM + " and c.id in :ids and c.classification <> :deleted",
                        Object[].class)
                .setParameter("userId", userId)
                .setParameter("ids", captureIds)
                .setParameter("deleted", Classification.DELETE)
                .getResultList().stream().map(CaptureQueryRepository::toRow).toList();
    }

    public List<CaptureRow> findPage(Long userId, CaptureListCondition condition, int offset, int limit) {
        Map<String, Object> params = new HashMap<>();
        String jpql = "select c, a " + FROM + where(condition, params) + orderBy(condition.sort());
        TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class)
                .setParameter("userId", userId)
                .setFirstResult(offset)
                .setMaxResults(limit);
        params.forEach(query::setParameter);
        return query.getResultList().stream().map(CaptureQueryRepository::toRow).toList();
    }

    public long count(Long userId, CaptureListCondition condition) {
        Map<String, Object> params = new HashMap<>();
        TypedQuery<Long> query = em.createQuery("select count(c) " + FROM + where(condition, params), Long.class)
                .setParameter("userId", userId);
        params.forEach(query::setParameter);
        return query.getSingleResult();
    }

    /** 카테고리별 실천 대기(TODO) 개수 [categoryId → count] */
    public Map<Long, Long> countTodoByCategory(Long userId) {
        Map<String, Object> params = new HashMap<>();
        String jpql = "select a.categoryId, count(c) " + FROM + statusCondition(CaptureStatus.TODO, params)
                + " and a.categoryId is not null group by a.categoryId";
        TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class).setParameter("userId", userId);
        params.forEach(query::setParameter);
        Map<Long, Long> counts = new HashMap<>();
        query.getResultList().forEach(r -> counts.put((Long) r[0], (Long) r[1]));
        return counts;
    }

    /** 완료율 계산 대상(TODO·DONE)의 [실천 분류 시각, 완료 시각(없으면 null)] */
    public List<Instant[]> findCompletionTargets(Long userId) {
        Map<String, Object> params = new HashMap<>();
        String jpql = "select c.classifiedAt, c.completedAt " + FROM
                + " and c.classification = :practice and a.status = :succeeded";
        params.put("practice", Classification.PRACTICE);
        params.put("succeeded", AnalysisStatus.SUCCEEDED);
        TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class).setParameter("userId", userId);
        params.forEach(query::setParameter);
        return query.getResultList().stream()
                .map(r -> new Instant[]{(Instant) r[0], (Instant) r[1]})
                .toList();
    }

    /** 안함(DISMISS) 후 되돌리기 시간이 지났는데 아직 분석 결과가 남아 있는 캡처 id */
    public List<Long> findDismissedWithAnalysis(Instant dismissedBefore) {
        return em.createQuery("""
                        select c.id from ScreenshotClassification c, ScreenshotAnalysis a
                        where a.captureId = c.id and c.classification = :reference and c.dismissed = true
                          and c.statusChangedAt < :before
                        """, Long.class)
                .setParameter("reference", Classification.REFERENCE)
                .setParameter("before", dismissedBefore)
                .getResultList();
    }

    private static String where(CaptureListCondition condition, Map<String, Object> params) {
        StringBuilder sb = new StringBuilder(statusCondition(condition.status(), params));
        if (condition.snoozed() != null) {
            sb.append(condition.snoozed()
                    ? " and c.snoozeUntil > :today"
                    : " and (c.snoozeUntil is null or c.snoozeUntil <= :today)");
            params.put("today", condition.today());
        }
        if (condition.completedFrom() != null) {
            sb.append(" and c.completedAt >= :completedFrom and c.completedAt < :completedTo");
            params.put("completedFrom", condition.completedFrom());
            params.put("completedTo", condition.completedTo());
        }
        return sb.toString();
    }

    private static String statusCondition(CaptureStatus status, Map<String, Object> params) {
        return switch (status) {
            case TODO, DONE -> {
                params.put("practice", Classification.PRACTICE);
                params.put("succeeded", AnalysisStatus.SUCCEEDED);
                yield " and c.classification = :practice and a.status = :succeeded and c.completedAt is "
                        + (status == CaptureStatus.TODO ? "null" : "not null");
            }
            case REFERENCE -> {
                params.put("reference", Classification.REFERENCE);
                yield " and c.classification = :reference";
            }
            default -> throw new IllegalArgumentException("목록 조회를 지원하지 않는 상태: " + status);
        };
    }

    private static String orderBy(CaptureListCondition.Sort sort) {
        String recent = "coalesce(c.statusChangedAt, c.classifiedAt) desc, c.id desc";
        return switch (sort) {
            case DEADLINE -> " order by case when a.deadlineDate is null then 1 else 0 end, a.deadlineDate asc, "
                    + recent;
            case RECENT -> " order by " + recent;
        };
    }

    private static CaptureRow toRow(Object[] r) {
        return new CaptureRow((ScreenshotClassification) r[0], (ScreenshotAnalysis) r[1]);
    }
}
