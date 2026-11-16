package com.moa.moa_be.domain.capture.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/** 명세 기준 시간대(Asia/Seoul) 변환 */
public final class CaptureTimes {

    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private CaptureTimes() {
    }

    public static LocalDate today() {
        return LocalDate.now(KST);
    }

    public static OffsetDateTime toKst(Instant instant) {
        return instant == null ? null : instant.atZone(KST).toOffsetDateTime().truncatedTo(ChronoUnit.SECONDS);
    }

    public static Instant startOf(LocalDate date) {
        return date.atStartOfDay(KST).toInstant();
    }
}
