package com.moa.moa_be.domain.category.entity;

public enum ReminderType {
    INTERVAL,   // 주기형
    DEADLINE,   // 마감기한 기반
    RANDOM,     // 랜덤 시간 기반
    LOCATION,   // 위치 기반
    NONE        // 알림 없음
}
