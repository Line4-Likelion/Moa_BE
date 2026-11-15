package com.moa.moa_be.domain.analysis.ocr;

import java.util.List;

/**
 * OCR 업체(Vision, CLOVA 등)와 상관없는 서버 내부 표준 형식 (API 명세서 ① OcrResult).
 *
 * @param width  원본 이미지 가로(px)
 * @param height 원본 이미지 세로(px)
 * @param blocks 글자 덩어리 목록 (문단 단위)
 */
public record OcrResult(int width, int height, List<Block> blocks) {

    /**
     * @param id   b1, b2 … (LLM이 근거로 가리키는 ID)
     * @param text 블록 글자
     * @param box  [x, y, w, h] 원본 대비 0~1 비율
     */
    public record Block(String id, String text, double[] box) {
    }

    public static OcrResult empty() {
        return new OcrResult(0, 0, List.of());
    }

    public boolean hasText() {
        return blocks.stream().anyMatch(b -> !b.text().isBlank());
    }

    public String fullText() {
        return String.join("\n", blocks.stream().map(Block::text).toList());
    }
}
