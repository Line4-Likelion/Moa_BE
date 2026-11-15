package com.moa.moa_be.domain.category.entity;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 모든 사용자에게 기본으로 생성되는 카테고리.
 * 선언 순서 = 목록 표시 순서. OTHER(기타)는 항상 맨 뒤.
 */
@Getter
@AllArgsConstructor
public enum DefaultCategory {
    FASHION("패션"),
    FOOD("맛집"),
    TRAVEL("여행"),
    RECIPE("레시피"),
    INFO("정보"),
    LIFE("생활"),
    OTHER("기타");

    private final String displayName;

    public static boolean contains(String name) {
        return Arrays.stream(values()).anyMatch(c -> c.displayName.equals(name));
    }

    /** 기본 카테고리면 선언 순서, 아니면 -1 */
    public static int orderOf(String name) {
        return Arrays.stream(values())
                .filter(c -> c.displayName.equals(name))
                .findFirst()
                .map(Enum::ordinal)
                .orElse(-1);
    }
}
