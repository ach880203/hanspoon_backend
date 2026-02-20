package com.project.hanspoon.shop.review.service;

import com.project.hanspoon.common.user.entity.User;
import com.project.hanspoon.shop.product.entity.Product;
import com.project.hanspoon.shop.review.entity.RevProduct;
import com.project.hanspoon.shop.product.repository.ProductRepository;
import com.project.hanspoon.shop.review.dto.ReviewCreateRequestDto;
import com.project.hanspoon.shop.review.dto.ReviewResponseDto;
import com.project.hanspoon.shop.review.dto.ReviewUpdateRequestDto;
import com.project.hanspoon.shop.review.repository.RevProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewProductService {

    private final RevProductRepository revProductRepository;
    private final ProductRepository productRepository;

    @PersistenceContext
    private EntityManager em;

    // ✅ 상품별 후기 목록
    public Page<ReviewResponseDto> listByProduct(Long productId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return revProductRepository.findByProduct_IdOrderByIdDesc(productId, pageable)
                .map(this::toDto);
    }

    // ✅ 내 후기 목록
    public Page<ReviewResponseDto> listMyReviews(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return revProductRepository.findByUser_UserIdOrderByIdDesc(userId, pageable)
                .map(this::toDto);
    }

    // ✅ 후기 등록 (내 계정으로)
    @Transactional
    public ReviewResponseDto create(Long userId, Long productId, ReviewCreateRequestDto req) {
        if (!StringUtils.hasText(req.getContent())) {
            throw new ResponseStatusException(BAD_REQUEST, "content는 필수입니다.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "상품이 없습니다. id=" + productId));

        User userRef = em.getReference(User.class, userId);

        RevProduct saved = revProductRepository.save(
                RevProduct.builder()
                        .user(userRef)
                        .product(product)
                        .content(req.getContent().trim())
                        .rating(req.getRating())
                        .build()
        );

        return toDto(saved);
    }

    // ✅ 후기 수정 (내 후기만)
    @Transactional
    public ReviewResponseDto update(Long userId, Long revId, ReviewUpdateRequestDto req) {
        RevProduct review = revProductRepository.findByIdAndUser_UserId(revId, userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "후기가 없습니다. revId=" + revId));

        if (req.getContent() != null) {
            String c = req.getContent().trim();
            if (!c.isEmpty()) review.setContent(c);
        }
        if (req.getRating() != null) {
            review.setRating(req.getRating());
        }

        return toDto(review);
    }

    // ✅ 후기 삭제 (내 후기만)
    @Transactional
    public void delete(Long userId, Long revId) {
        RevProduct review = revProductRepository.findByIdAndUser_UserId(revId, userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "후기가 없습니다. revId=" + revId));
        revProductRepository.delete(review);
    }

    private ReviewResponseDto toDto(RevProduct r) {
        return ReviewResponseDto.builder()
                .revId(r.getId())
                .productId(r.getProduct().getId())
                .userId(r.getUser().getUserId())
                .content(r.getContent())
                .rating(r.getRating())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
