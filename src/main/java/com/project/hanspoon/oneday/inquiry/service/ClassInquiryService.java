package com.project.hanspoon.oneday.inquiry.service;

import com.project.hanspoon.common.exception.BusinessException;
import com.project.hanspoon.common.user.repository.UserRepository;
import com.project.hanspoon.oneday.clazz.repository.ClassProductRepository;
import com.project.hanspoon.oneday.inquiry.domain.Visibility;
import com.project.hanspoon.oneday.inquiry.dto.ClassInquiryAnswerRequest;
import com.project.hanspoon.oneday.inquiry.dto.ClassInquiryCreateRequest;
import com.project.hanspoon.oneday.inquiry.dto.ClassInquiryResponse;
import com.project.hanspoon.oneday.inquiry.entity.ClassInquiry;
import com.project.hanspoon.oneday.inquiry.repository.ClassInquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassInquiryService {

    private final ClassInquiryRepository classInquiryRepository;
    private final ClassProductRepository classProductRepository;
    private final UserRepository userRepository;

    public ClassInquiryResponse create(Long userId, ClassInquiryCreateRequest req) {
        // 1) 요청값 검증
        validateCreate(userId, req);

        Long classProductId = req.classProductId();
        Visibility visibility = Boolean.TRUE.equals(req.secret()) ? Visibility.PRIVATE : Visibility.PUBLIC;
        boolean hasAttachment = Boolean.TRUE.equals(req.hasAttachment());

        ClassInquiry saved = classInquiryRepository.save(
                ClassInquiry.create(
                        classProductRepository.getReferenceById(classProductId),
                        userId,
                        req.category().trim(),
                        req.title().trim(),
                        req.content().trim(),
                        visibility,
                        hasAttachment
                )
        );

        String writerName = resolveWriterName(userId);
        return toResponse(saved, writerName, true, true);
    }

    @Transactional(readOnly = true)
    public List<ClassInquiryResponse> listAll(Long viewerUserId, boolean isAdmin) {
        // 2) 모든 문의를 한 번에 조회하고, 조회자 권한에 따라 마스킹 여부를 결정합니다.
        List<ClassInquiry> inquiries = classInquiryRepository.findAllByOrderByCreatedAtDesc();
        Map<Long, String> names = userRepository.findAllById(
                        inquiries.stream().map(ClassInquiry::getUserId).distinct().toList()
                ).stream()
                .collect(Collectors.toMap(u -> u.getUserId(), u -> u.getUserName(), (a, b) -> a));

        return inquiries.stream()
                .map(inquiry -> {
                    boolean canViewContent = canView(inquiry, viewerUserId, isAdmin);
                    boolean canAnswer = canAnswer(inquiry, viewerUserId, isAdmin);
                    String writerName = names.getOrDefault(inquiry.getUserId(), "이름 없음");
                    return toResponse(inquiry, writerName, canViewContent, canAnswer);
                })
                .toList();
    }

    public ClassInquiryResponse answer(Long inquiryId, Long actorUserId, boolean isAdmin, ClassInquiryAnswerRequest req) {
        if (actorUserId == null || actorUserId <= 0) {
            throw new BusinessException("로그인 정보가 필요합니다.");
        }
        if (req == null || req.answerContent() == null || req.answerContent().isBlank()) {
            throw new BusinessException("답글 내용(answerContent)은 필수입니다.");
        }

        ClassInquiry inquiry = classInquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new BusinessException("문의를 찾을 수 없습니다. id=" + inquiryId));

        if (!canAnswer(inquiry, actorUserId, isAdmin)) {
            throw new BusinessException("답글은 작성자 또는 관리자만 등록할 수 있습니다.");
        }

        String answer = req.answerContent().trim();
        if (answer.length() > 4000) {
            throw new BusinessException("답글 내용(answerContent)은 최대 4000자입니다.");
        }

        inquiry.answer(answer, actorUserId, LocalDateTime.now());

        String writerName = resolveWriterName(inquiry.getUserId());
        return toResponse(inquiry, writerName, true, canAnswer(inquiry, actorUserId, isAdmin));
    }

    private void validateCreate(Long userId, ClassInquiryCreateRequest req) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("로그인 정보가 필요합니다.");
        }
        if (req == null) {
            throw new BusinessException("문의 요청값이 없습니다.");
        }
        if (req.classProductId() == null || req.classProductId() <= 0) {
            throw new BusinessException("classProductId는 필수입니다.");
        }
        if (req.category() == null || req.category().isBlank()) {
            throw new BusinessException("분류(category)는 필수입니다.");
        }
        if (req.title() == null || req.title().isBlank()) {
            throw new BusinessException("제목(title)은 필수입니다.");
        }
        if (req.content() == null || req.content().isBlank()) {
            throw new BusinessException("내용(content)은 필수입니다.");
        }
        if (req.category().trim().length() > 30) {
            throw new BusinessException("분류(category)는 최대 30자입니다.");
        }
        if (req.title().trim().length() > 150) {
            throw new BusinessException("제목(title)은 최대 150자입니다.");
        }
        if (req.content().trim().length() > 4000) {
            throw new BusinessException("내용(content)은 최대 4000자입니다.");
        }

        boolean exists = classProductRepository.existsById(req.classProductId());
        if (!exists) {
            throw new BusinessException("존재하지 않는 클래스입니다. id=" + req.classProductId());
        }
    }

    private boolean canView(ClassInquiry inquiry, Long viewerUserId, boolean isAdmin) {
        // 공개글은 모두 조회 가능
        if (inquiry.getVisibility() == Visibility.PUBLIC) {
            return true;
        }
        // 비밀글은 작성자/관리자만 조회 가능
        if (viewerUserId == null || viewerUserId <= 0) {
            return false;
        }
        return isAdmin || inquiry.getUserId().equals(viewerUserId);
    }

    private boolean canAnswer(ClassInquiry inquiry, Long actorUserId, boolean isAdmin) {
        // 답글 권한: 작성자 또는 관리자
        if (actorUserId == null || actorUserId <= 0) {
            return false;
        }
        return isAdmin || inquiry.getUserId().equals(actorUserId);
    }

    private String resolveWriterName(Long userId) {
        return userRepository.findById(userId)
                .map(u -> u.getUserName())
                .orElse("이름 없음");
    }

    private ClassInquiryResponse toResponse(ClassInquiry inquiry, String writerName, boolean canViewContent, boolean canAnswer) {
        // 비밀글을 권한 없이 조회하면 제목/본문/답글을 마스킹합니다.
        String title = canViewContent ? inquiry.getTitle() : "비밀글입니다.";
        String content = canViewContent ? inquiry.getContent() : "비밀글입니다.";
        String answerContent = canViewContent ? inquiry.getAnswerContent() : null;

        return new ClassInquiryResponse(
                inquiry.getId(),
                inquiry.getClassProductId(),
                inquiry.getUserId(),
                writerName,
                inquiry.getCategory(),
                title,
                content,
                inquiry.getVisibility(),
                inquiry.isHasAttachment(),
                inquiry.isAnswered(),
                answerContent,
                inquiry.getAnsweredByUserId(),
                inquiry.getAnsweredAt(),
                canAnswer,
                inquiry.getCreatedAt()
        );
    }
}
