package com.project.hanspoon.oneday.api.location;

import com.project.hanspoon.common.exception.BusinessException;
import com.project.hanspoon.oneday.api.location.dto.GeocodeResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * 카카오 로컬 API 호출 서비스
 * - REST 키는 반드시 서버에서만 관리(프론트 노출 금지)
 */
@Service
public class KakaoLocationService {

    @Value("${kakao.rest-api-key:}")
    private String kakaoRestApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 주소 -> 좌표 변환(지오코딩)
     * 카카오 로컬 주소 검색 API 사용
     *
     * @param query 사용자가 입력한 주소 문자열
     * @return address, lat, lng
     */
    public GeocodeResponseDTO geocode(String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.isEmpty()) {
            throw new BusinessException("주소를 입력해 주세요.");
        }

        // REST API 키는 지도 JavaScript 키와 다릅니다.
        // 키 누락/오입력 시 앱 전체 기동은 유지하고, 주소검색 시점에 원인을 안내합니다.
        String restApiKey = kakaoRestApiKey == null ? "" : kakaoRestApiKey.trim();
        if (restApiKey.isEmpty()) {
            throw new BusinessException("카카오 주소검색 REST API 키가 설정되지 않았습니다. 백엔드 KAKAO_REST_API_KEY를 확인해 주세요.");
        }

        String url = UriComponentsBuilder
                .fromHttpUrl("https://dapi.kakao.com/v2/local/search/address.json")
                .queryParam("query", normalizedQuery)
                .build()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + restApiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response;
        try {
            response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            throw new BusinessException("카카오 주소검색 인증에 실패했습니다. REST API 키와 카카오 개발자 콘솔 설정을 확인해 주세요.");
        } catch (RestClientException e) {
            throw new BusinessException("카카오 주소검색 호출에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }

        Map body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("카카오 응답이 비어있습니다.");
        }

        List documents = (List) body.get("documents");
        if (documents == null || documents.isEmpty()) {
            // 프론트에서 이 메시지를 그대로 사용자 안내로 써도 됨(한글)
            throw new IllegalArgumentException("검색 결과가 없습니다. 주소를 더 구체적으로 입력해 주세요.");
        }

        Map first = (Map) documents.get(0);

        // 카카오 응답 기준: x=경도, y=위도
        String addressName = String.valueOf(first.get("address_name"));
        double lng = Double.parseDouble(String.valueOf(first.get("x")));
        double lat = Double.parseDouble(String.valueOf(first.get("y")));

        return new GeocodeResponseDTO(addressName, lat, lng);
    }
}
