package com.project.hanspoon.shop.product.service;

import com.project.hanspoon.shop.product.dto.ProductImageResponseDto;
import com.project.hanspoon.shop.product.entity.Product;
import com.project.hanspoon.shop.product.entity.ProductImage;
import com.project.hanspoon.shop.product.repository.ProductImageRepository;
import com.project.hanspoon.shop.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * ProductImageService
 * - 상품 이미지의 업로드/조회/삭제를 담당
 * - “파일 시스템 저장” + “DB(ProductImage) 저장”을 함께 처리한다.
 *
 * 설계 의도:
 * - readOnly=true로 기본 조회 최적화
 * - 업로드/삭제는 @Transactional로 쓰기 트랜잭션을 별도로 적용
 *
 * 주의:
 * - 파일 시스템 작업은 DB 트랜잭션 롤백과 완전히 동기화되지 않기 때문에
 *   예외 발생 시 “파일만 남는 문제” 같은 보상(정리) 전략이 필요할 수 있다(리뷰 참고)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductImageService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    @Value("${itemImgLocation}")
    private String itemImgLocation; // 예: c:/hanspoon/img

    /**
     * 이미지 업로드 (여러 장)
     * - 기본: 첫 번째 파일을 대표(rep)로 지정
     * - repIndex 지정 시: 해당 인덱스 파일을 대표로 지정(0부터)
     *
     * 반환:
     * - 업로드/저장된 이미지들의 응답 DTO 리스트
     */
    @Transactional
    public List<ProductImageResponseDto> upload(Long productId, List<MultipartFile> files, Integer repIndex) {

        // 1) 상품 존재 확인 (없으면 404)
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "상품이 없습니다. id=" + productId));

        // 2) 파일 리스트가 null/empty면 업로드할 것이 없으므로 빈 리스트 반환
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        // 3) 대표 이미지 인덱스 결정
        // - repIndex가 null이면 0(첫 번째)
        // - 범위를 벗어나면 0으로 보정
        int rep = (repIndex == null) ? 0 : repIndex;
        if (rep < 0 || rep >= files.size()) rep = 0;

        // 4) 기존 대표 이미지가 있으면 대표 해제(repYn=false)
        // - “상품당 대표 이미지는 최대 1장” 규칙을 유지하기 위한 처리
        // - findFirstBy...는 정렬이 없으면 대표가 여러 장일 때 어떤 것이 선택될지 불명확(리뷰 참고)
        productImageRepository.findFirstByProduct_IdAndRepYnTrue(productId)
                .ifPresent(img -> img.setRepYn(false));
        // ※ 영속 상태라면 dirty checking으로 커밋 시 반영됨

        // 5) 저장 디렉토리 존재 보장
        ensureDirExists(itemImgLocation);

        List<ProductImageResponseDto> result = new ArrayList<>();

        // 6) 파일을 순회하면서 (a) 파일 저장 (b) DB 저장을 수행
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);

            // null 또는 빈 파일은 건너뜀
            if (file == null || file.isEmpty()) continue;

            // 원본 파일명 (null 방어)
            String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("file");

            // 확장자 추출 (예: jpg, png)
            String ext = getExtension(originalName);

            // 저장 파일명(UUID 기반) 생성: 충돌 방지
            // ext가 있으면 ".ext"로 붙임
            String storedName = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);

            // 실제 저장 경로: itemImgLocation/storedName
            Path savePath = Paths.get(itemImgLocation).resolve(storedName);

            // 파일 저장
            // - transferTo 성공 시 파일 시스템에 저장됨
            // - 실패 시 RuntimeException으로 감싸서 트랜잭션 롤백 유도(단, 파일은 이미 생겼을 수 있음)
            try {
                file.transferTo(savePath);
            } catch (IOException e) {
                throw new RuntimeException("파일 저장 실패: " + originalName, e);
            }

            // 노출 URL 구성 (정적 리소스 매핑 기준)
            // 예: /images/{storedName}
            String imgUrl = "/images/" + storedName;

            // DB 저장: product_id FK + 파일 메타 + 대표 여부(repYn)
            ProductImage saved = productImageRepository.save(
                    ProductImage.builder()
                            .product(product)
                            .originalName(originalName)
                            .storedName(storedName)
                            .imgUrl(imgUrl)
                            .repYn(i == rep) // repIndex에 해당하는 “리스트 위치”의 파일을 대표로 지정
                            .build()
            );

            // 응답 DTO로 변환해서 결과에 추가
            result.add(toDto(saved));
        }

        return result;
    }

    /**
     * 특정 상품의 이미지 목록 조회
     * - 정렬: 대표(repYn=true) 먼저, 그 다음 id 오름차순
     */
    public List<ProductImageResponseDto> list(Long productId) {

        // (선택) 상품 존재 여부 체크
        // - 이미지가 없는 상품과 “상품이 없는 경우”를 구분하기 위해 추가 체크를 한 것으로 보임
        productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "상품이 없습니다. id=" + productId));

        // 이미지 엔티티 조회 -> DTO 변환
        return productImageRepository.findByProduct_IdOrderByRepYnDescIdAsc(productId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * 이미지 삭제
     * - 파일 시스템에서 실제 파일 삭제
     * - DB에서 ProductImage 레코드 삭제
     * - 삭제된 이미지가 대표였다면, 남은 이미지 중 첫 번째를 대표로 승격
     */
    @Transactional
    public void delete(Long productId, Long imageId) {

        // 1) 이미지 레코드 조회 (없으면 404)
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "이미지가 없습니다. id=" + imageId));

        // 2) URL 조작/타상품 삭제 방지: productId 일치 검증
        if (!image.getProduct().getId().equals(productId)) {
            throw new IllegalArgumentException("해당 상품의 이미지가 아닙니다.");
        }

        // 3) 파일 삭제 (실제 저장된 파일 제거)
        Path path = Paths.get(itemImgLocation).resolve(image.getStoredName());
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // 정책: 파일 삭제 실패 시 DB 삭제도 막고 예외 던짐
            // - 장점: DB와 파일 불일치(레코드는 사라졌는데 파일만 남는 상황)를 방지
            // - 단점: 파일이 잠겨 있거나 권한 이슈면 삭제가 계속 실패해 기능이 막힐 수 있음(리뷰 참고)
            throw new RuntimeException("파일 삭제 실패: " + image.getStoredName(), e);
        }

        // 4) 대표 이미지였는지 기록해두고 DB에서 삭제
        boolean wasRep = image.isRepYn();
        productImageRepository.delete(image);

        // 5) 대표 이미지였고, 다른 이미지가 남아있으면 첫 번째를 대표로 승격
        if (wasRep) {
            List<ProductImage> remain = productImageRepository.findByProduct_IdOrderByRepYnDescIdAsc(productId);
            if (!remain.isEmpty()) {
                remain.get(0).setRepYn(true); // dirty checking으로 반영
            }
        }
    }

    // 엔티티 -> 응답 DTO 변환
    private ProductImageResponseDto toDto(ProductImage img) {
        return ProductImageResponseDto.builder()
                .id(img.getId())
                .originalName(img.getOriginalName())
                .imgUrl(img.getImgUrl())
                .repYn(img.isRepYn())
                .build();
    }

    // 저장 디렉토리 생성(존재 보장)
    private void ensureDirExists(String dir) {
        try {
            Files.createDirectories(Paths.get(dir));
        } catch (IOException e) {
            throw new RuntimeException("디렉토리 생성 실패: " + dir, e);
        }
    }

    // 파일명에서 확장자 추출 (마지막 '.' 기준)
    private String getExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) return "";
        return filename.substring(idx + 1);
    }
}
