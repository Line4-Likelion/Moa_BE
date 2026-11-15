package com.moa.moa_be.domain.category.entity;

import com.moa.moa_be.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "categories",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_categories_user_name",
                columnNames = {"user_id", "category_name"}
        )
)
public class Category extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "category_name", nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "behavior_type", nullable = false)
    private BehaviorType behaviorType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_type", nullable = false)
    private ReminderType reminderType;

    @Column(name = "reminder_interval")
    private Integer reminderInterval;

    @Column(name = "max_reminder_count")
    private Integer maxReminderCount;

    @Column(name = "location_alert_enabled", nullable = false)
    private boolean locationAlertEnabled;

    @Builder
    private Category(Long userId, String name, BehaviorType behaviorType, ReminderType reminderType) {
        this.userId = userId;
        this.name = name;
        // 카테고리별 알림 설정을 쓸지 아직 미정(알림 파트 확인 중)이라 기본값으로 채움
        this.behaviorType = behaviorType != null ? behaviorType : BehaviorType.ACTION;
        this.reminderType = reminderType != null ? reminderType : ReminderType.NONE;
        this.locationAlertEnabled = false;
    }

    public static Category of(Long userId, String name) {
        return Category.builder().userId(userId).name(name).build();
    }

    /** 기본 카테고리(패션, 맛집 …, 기타)가 아니라 사용자가 직접 만든 카테고리인지 */
    public boolean isCustom() {
        return !DefaultCategory.contains(name);
    }

    public boolean isOther() {
        return DefaultCategory.OTHER.getDisplayName().equals(name);
    }
}
