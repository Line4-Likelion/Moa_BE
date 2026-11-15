
package com.moa.moa_be.domain.alarm.service;

import com.moa.moa_be.domain.alarm.dto.CoordinateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeocodingService {

    private final RestTemplate restTemplate;

    @Value("${kakao.api.key}")
    private String kakaoApiKey;

    private record CoordinateCandidate(
            double latitude,
            double longitude,
            String placeName,
            String roadAddress,
            String jibunAddress
    ) {}

    private record SearchResult(
            List<CoordinateCandidate> candidates,
            boolean isEnd
    ) {}

    public CoordinateDto getCoordinates(String address) {
        return getCoordinates(null, address);
    }

    public CoordinateDto getCoordinates(String placeName, String address) {

        if ((address == null || address.isBlank())
                && (placeName == null || placeName.isBlank())) {
            return null;
        }

        String cleanAddress = cleanAddress(address);

        log.info("좌표 검색 시작 placeName={}, address={}",
                placeName, cleanAddress);

        CoordinateDto coordinate;

        if (cleanAddress != null && !cleanAddress.isBlank()) {

            coordinate = searchAddress(cleanAddress);

            if (coordinate != null) {
                return coordinate;
            }
        }

        if (placeName != null && !placeName.isBlank()) {

            coordinate = searchKeyword(placeName, cleanAddress);

            if (coordinate != null) {
                return coordinate;
            }
        }

        if (cleanAddress != null && !cleanAddress.isBlank()) {

            coordinate = searchKeyword(cleanAddress, cleanAddress);

            if (coordinate != null) {
                return coordinate;
            }
        }

        log.warn("장소 좌표 검색 실패 placeName={}, address={}",
                placeName, cleanAddress);

        return null;
    }

    /**
     * 검색에 불필요한 상세 주소 제거
     */
    private String cleanAddress(String address) {

        if (address == null || address.isBlank()) {
            return null;
        }

        return address
                .replaceAll("\\(우\\)\\s*\\d{5}", "")
                .replaceAll("\\s+\\d+층.*$", "")
                .replaceAll("\\s+\\d+호.*$", "")
                .replaceAll("[~～]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * 주소 검색 API
     */
    private CoordinateDto searchAddress(String address) {

        String url = "https://dapi.kakao.com/v2/local/search/address.json?query="
                + encode(address);

        SearchResult result = requestCandidates(url, "주소 검색");

        return selectBestCandidate(
                result.candidates(),
                address,
                "주소 검색"
        );
    }

    /**
     * 키워드 검색 API
     */
    private CoordinateDto searchKeyword(
            String keyword,
            String expectedAddress) {

        List<CoordinateCandidate> candidates = new ArrayList<>();

        for (int page = 1; page <= 3; page++) {

            String url =
                    "https://dapi.kakao.com/v2/local/search/keyword.json?query="
                            + encode(keyword)
                            + "&size=15"
                            + "&page=" + page;

            SearchResult result = requestCandidates(url, "키워드 검색");

            candidates.addAll(result.candidates());

            if (result.isEnd()) {
                break;
            }
        }

        return selectBestCandidate(
                candidates,
                expectedAddress,
                "키워드 검색"
        );
    }

    /**
     * 카카오 API 요청 및 검색 후보 추출
     */
    private SearchResult requestCandidates(
            String url,
            String searchType) {

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoApiKey);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    URI.create(url),
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = response.getBody();

            if (body == null) {
                log.warn("{} API 응답 body가 null입니다.", searchType);
                return new SearchResult(List.of(), true);
            }

            Object documentsObj = body.get("documents");

            if (!(documentsObj instanceof List<?> documents)) {
                log.warn("{} API 응답 documents가 올바르지 않습니다.", searchType);
                return new SearchResult(List.of(), true);
            }

            List<CoordinateCandidate> candidates = new ArrayList<>();

            for (Object document : documents) {

                if (!(document instanceof Map<?, ?> doc)) {
                    continue;
                }

                Object x = getCoordinateValue(doc, "x");
                Object y = getCoordinateValue(doc, "y");

                if (x == null || y == null) {
                    continue;
                }

                String placeName = getString(doc, "place_name");

                String roadAddress = getString(doc, "road_address_name");
                String jibunAddress = getString(doc, "address_name");

                if (roadAddress.isBlank()) {
                    roadAddress = getNestedAddress(doc, "road_address");
                }

                if (jibunAddress.isBlank()) {
                    jibunAddress = getNestedAddress(doc, "address");
                }

                candidates.add(new CoordinateCandidate(
                        Double.parseDouble(y.toString()),
                        Double.parseDouble(x.toString()),
                        placeName,
                        roadAddress,
                        jibunAddress
                ));
            }

            boolean isEnd = true;

            Object metaObj = body.get("meta");

            if (metaObj instanceof Map<?, ?> meta) {
                Object endObj = meta.get("is_end");

                if (endObj != null) {
                    isEnd = Boolean.parseBoolean(endObj.toString());
                }
            }

            log.info("{} 후보 검색 완료 count={}",
                    searchType, candidates.size());

            return new SearchResult(candidates, isEnd);

        } catch (Exception e) {
            log.error("{} API 요청 실패", searchType, e);

            return new SearchResult(List.of(), true);
        }
    }

    /**
     * 카카오 API 응답에서 좌표 조회
     */
    private Object getCoordinateValue(
            Map<?, ?> document,
            String key) {

        Object value = document.get(key);

        if (value != null) {
            return value;
        }

        String[] addressKeys = {"road_address", "address"};

        for (String addressKey : addressKeys) {

            Object addressObj = document.get(addressKey);

            if (addressObj instanceof Map<?, ?> addressMap) {

                value = addressMap.get(key);

                if (value != null) {
                    return value;
                }
            }
        }

        return null;
    }

    /**
     * 주소 일치도를 기준으로 최적의 좌표 선택
     */
    private CoordinateDto selectBestCandidate(
            List<CoordinateCandidate> candidates,
            String expectedAddress,
            String searchType) {

        if (expectedAddress == null || expectedAddress.isBlank()
                || candidates.isEmpty()) {
            return null;
        }

        CoordinateCandidate bestCandidate = null;
        int bestScore = 0;
        boolean ambiguous = false;

        for (CoordinateCandidate candidate : candidates) {

            int score = Math.max(
                    getAddressScore(expectedAddress, candidate.roadAddress()),
                    getAddressScore(expectedAddress, candidate.jibunAddress())
            );

            if (score > bestScore) {
                bestScore = score;
                bestCandidate = candidate;
                ambiguous = false;

            } else if (score > 0 && score == bestScore
                    && bestCandidate != null
                    && !isSameCoordinate(bestCandidate, candidate)) {

                ambiguous = true;
            }
        }

        if (bestCandidate == null) {
            log.info("{} 주소 일치 후보 없음 address={}",
                    searchType, expectedAddress);
            return null;
        }

        if (ambiguous) {
            log.warn("{} 좌표 후보가 모호하여 저장 보류 address={}",
                    searchType, expectedAddress);
            return null;
        }

        log.info("{} 최종 장소 선택 name={}, address={}, lat={}, lng={}",
                searchType,
                bestCandidate.placeName(),
                bestCandidate.roadAddress(),
                bestCandidate.latitude(),
                bestCandidate.longitude());

        return new CoordinateDto(
                bestCandidate.latitude(),
                bestCandidate.longitude()
        );
    }

    /**
     * 주소 일치도 점수 계산
     */
    private int getAddressScore(
            String expectedAddress,
            String actualAddress) {

        if (actualAddress == null || actualAddress.isBlank()) {
            return 0;
        }

        String expected = normalizeAddress(expectedAddress);
        String actual = normalizeAddress(actualAddress);

        if (expected.isBlank() || actual.isBlank()) {
            return 0;
        }

        if (expected.equals(actual)) {
            return 100;
        }

        if (expected.endsWith(actual) || actual.endsWith(expected)) {
            return 80;
        }

        return 0;
    }

    /**
     * 주소 비교를 위한 문자열 정규화
     */
    private String normalizeAddress(String address) {

        if (address == null) {
            return "";
        }

        String normalized = address
                .replaceAll("\\([^)]*\\)", " ")
                .replaceAll("\\s+\\d+층.*$", "")
                .replaceAll("\\s+\\d+호.*$", "")
                .replaceAll("[,\\s]", "");

        return normalized.trim();
    }

    /**
     * 두 후보의 좌표가 동일한지 확인
     */
    private boolean isSameCoordinate(
            CoordinateCandidate first,
            CoordinateCandidate second) {

        return Math.abs(first.latitude() - second.latitude()) < 0.00001
                && Math.abs(first.longitude() - second.longitude()) < 0.00001;
    }

    private String getString(Map<?, ?> document, String key) {

        Object value = document.get(key);

        return value == null ? "" : value.toString();
    }

    private String getNestedAddress(Map<?, ?> document, String key) {

        Object value = document.get(key);

        if (value instanceof Map<?, ?> addressMap) {
            return getString(addressMap, "address_name");
        }

        return "";
    }

    /**
     * 검색어 URL 인코딩
     */
    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
