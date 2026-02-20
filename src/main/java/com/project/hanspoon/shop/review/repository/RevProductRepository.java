package com.project.hanspoon.shop.review.repository;

import com.project.hanspoon.shop.review.entity.RevProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RevProductRepository extends JpaRepository<RevProduct, Long> {

    Page<RevProduct> findByProduct_IdOrderByIdDesc(Long productId, Pageable pageable);

    Page<RevProduct> findByUser_UserIdOrderByIdDesc(Long userId, Pageable pageable);

    Optional<RevProduct> findByIdAndUser_UserId(Long revId, Long userId);
}
