package com.project.hanspoon.oneday.inquiry.entity;

import com.project.hanspoon.common.entity.BaseTimeEntity;
import com.project.hanspoon.oneday.clazz.entity.ClassProduct;
import com.project.hanspoon.oneday.inquiry.domain.Visibility;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "class_inquiry",
        indexes = {
                @Index(name = "idx_class_inquiry_class", columnList = "class_product_id"),
                @Index(name = "idx_class_inquiry_user", columnList = "user_id"),
                @Index(name = "idx_class_inquiry_created", columnList = "createdAt")
        }
)
public class ClassInquiry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_product_id", nullable = false)
    private ClassProduct classProduct;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 4000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Visibility visibility;

    @Column(name = "has_attachment", nullable = false)
    private boolean hasAttachment;

    @Column(name = "answer_content", length = 4000)
    private String answerContent;

    @Column(name = "answered_by_user_id")
    private Long answeredByUserId;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    private ClassInquiry(
            ClassProduct classProduct,
            Long userId,
            String category,
            String title,
            String content,
            Visibility visibility,
            boolean hasAttachment
    ) {
        this.classProduct = classProduct;
        this.userId = userId;
        this.category = category;
        this.title = title;
        this.content = content;
        this.visibility = visibility;
        this.hasAttachment = hasAttachment;
    }

    public static ClassInquiry create(
            ClassProduct classProduct,
            Long userId,
            String category,
            String title,
            String content,
            Visibility visibility,
            boolean hasAttachment
    ) {
        return new ClassInquiry(classProduct, userId, category, title, content, visibility, hasAttachment);
    }

    // 기존 서비스 코드(ClassInquiryService) 하위호환 팩토리 메서드
    public static ClassInquiry of(
            Long userId,
            Long classProductId,
            String category,
            String title,
            String content,
            Visibility visibility,
            boolean hasAttachment
    ) {
        throw new UnsupportedOperationException(
                "Deprecated factory method. Use ClassInquiry.create(ClassProduct, ...) instead."
        );
    }

    // 기존 DTO 매핑 코드 하위호환: classProduct.id 직접 접근 메서드
    public Long getClassProductId() {
        return this.classProduct != null ? this.classProduct.getId() : null;
    }

    public boolean isAnswered() {
        return answerContent != null && !answerContent.isBlank();
    }

    public void answer(String answerContent, Long answeredByUserId, LocalDateTime answeredAt) {
        this.answerContent = answerContent;
        this.answeredByUserId = answeredByUserId;
        this.answeredAt = answeredAt;
    }
}