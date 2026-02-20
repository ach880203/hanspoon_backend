package com.project.hanspoon.oneday.inquiry.entity;

import com.project.hanspoon.common.entity.BaseTimeEntity;
import com.project.hanspoon.oneday.inquiry.domain.InquiryStatus;
import com.project.hanspoon.oneday.inquiry.domain.Visibility;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
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
                @Index(name = "idx_class_inquiry_user", columnList = "user_id"),
                @Index(name = "idx_class_inquiry_class", columnList = "class_product_id"),
                @Index(name = "idx_class_inquiry_created_at", columnList = "created_at")
        }
)
public class ClassInquiry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "class_product_id")
    private Long classProductId;

    // 구 스키마(class_id)와의 호환을 위해 같은 값을 함께 저장합니다.
    // 운영 DB에 남아 있는 NOT NULL 제약으로 인한 INSERT 실패를 방지하기 위한 필드입니다.
    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 4000)
    private String content;

    // 구 스키마(secret)와의 호환 필드입니다.
    @Column(nullable = false, columnDefinition = "bit(1)")
    private boolean secret;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Visibility visibility;

    // 구 스키마(status)와의 호환 필드입니다.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryStatus status;

    @Column(name = "has_attachment", nullable = false, columnDefinition = "tinyint(1) default 0")
    private boolean hasAttachment;

    @Column(name = "answered", nullable = false, columnDefinition = "tinyint(1) default 0")
    private boolean answered;

    @Column(name = "answer_content", length = 4000)
    private String answerContent;

    @Column(name = "answered_by_user_id")
    private Long answeredByUserId;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    private ClassInquiry(
            Long userId,
            Long classProductId,
            String category,
            String title,
            String content,
            Visibility visibility,
            boolean hasAttachment
    ) {
        this.userId = userId;
        this.classProductId = classProductId;
        this.classId = classProductId;
        this.category = category;
        this.title = title;
        this.content = content;
        this.secret = visibility == Visibility.PRIVATE;
        this.visibility = visibility;
        this.status = InquiryStatus.OPEN;
        this.hasAttachment = hasAttachment;
        this.answered = false;
    }

    public static ClassInquiry of(
            Long userId,
            Long classProductId,
            String category,
            String title,
            String content,
            Visibility visibility,
            boolean hasAttachment
    ) {
        return new ClassInquiry(userId, classProductId, category, title, content, visibility, hasAttachment);
    }

    public void answer(String answerContent, Long answeredByUserId, LocalDateTime answeredAt) {
        this.answered = true;
        this.answerContent = answerContent;
        this.answeredByUserId = answeredByUserId;
        this.answeredAt = answeredAt;
        this.status = InquiryStatus.ANSWERED;
    }
}
