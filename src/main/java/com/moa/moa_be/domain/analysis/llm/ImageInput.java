package com.moa.moa_be.domain.analysis.llm;

/**
 * 글자가 거의 없는 사진일 때 LLM 에 함께 보내는 원본 이미지.
 */
public record ImageInput(byte[] bytes, String mimeType) {

    /** 파일 앞부분(magic bytes)으로 형식 판별. 모르면 image/jpeg */
    public static ImageInput of(byte[] bytes) {
        return new ImageInput(bytes, detectMimeType(bytes));
    }

    static String detectMimeType(byte[] b) {
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "image/png";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        return "image/jpeg";
    }
}
