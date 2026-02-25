package com.project.hanspoon.oneday.clazz.service;

import com.project.hanspoon.common.exception.BusinessException;
import com.project.hanspoon.oneday.clazz.dto.ClassCreateRequest;
import com.project.hanspoon.oneday.clazz.dto.ClassCreateResponse;
import com.project.hanspoon.oneday.clazz.dto.ClassDetailResponse;
import com.project.hanspoon.oneday.clazz.dto.ClassSessionCreateRequest;
import com.project.hanspoon.oneday.clazz.dto.ClassUpdateRequest;
import com.project.hanspoon.oneday.clazz.entity.ClassProduct;
import com.project.hanspoon.oneday.clazz.entity.ClassSession;
import com.project.hanspoon.oneday.clazz.repository.ClassProductRepository;
import com.project.hanspoon.oneday.clazz.repository.ClassSessionRepository;
import com.project.hanspoon.oneday.instructor.entity.Instructor;
import com.project.hanspoon.oneday.instructor.repository.InstructorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassCommandService {

    private final ClassProductRepository classProductRepository;
    private final ClassSessionRepository classSessionRepository;
    private final InstructorRepository instructorRepository;

    public ClassCreateResponse createClass(Long actorUserId, boolean isAdmin, ClassCreateRequest req) {
        validateActor(actorUserId, isAdmin);
        validateCreateRequest(req);

        List<String> detailImages = normalizeDetailImages(req.detailImageData(), req.detailImageDataList());
        Instructor instructor = loadInstructor(req.instructorId());

        ClassProduct savedClass = classProductRepository.save(
                ClassProduct.builder()
                        .title(req.title().trim())
                        .description(trimOrEmpty(req.description()))
                        .detailDescription(trimOrEmpty(req.detailDescription()))
                        .detailImageData(detailImages.isEmpty() ? "" : detailImages.get(0))
                        .level(req.level())
                        .runType(req.runType())
                        .category(req.category())
                        .instructor(instructor)
                        .build()
        );

        // 상세 설명 이미지는 정렬 순서대로 별도 엔티티로 관리합니다.
        savedClass.replaceDetailImages(detailImages);

        List<Long> createdSessionIds = replaceSessions(savedClass, req.sessions());

        return new ClassCreateResponse(
                savedClass.getId(),
                savedClass.getTitle(),
                createdSessionIds.size(),
                createdSessionIds
        );
    }

    public ClassDetailResponse updateClass(Long actorUserId, boolean isAdmin, Long classId, ClassUpdateRequest req) {
        validateActor(actorUserId, isAdmin);
        validateUpdateRequest(req);

        List<String> detailImages = normalizeDetailImages(req.detailImageData(), req.detailImageDataList());

        ClassProduct target = classProductRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("클래스를 찾을 수 없습니다. id=" + classId));

        if (classSessionRepository.existsByClassProductIdAndReservedCountGreaterThan(classId, 0)) {
            throw new BusinessException("예약 이력이 있는 클래스는 수정할 수 없습니다.");
        }

        Instructor instructor = loadInstructor(req.instructorId());

        target.updateInfo(
                req.title().trim(),
                trimOrEmpty(req.description()),
                trimOrEmpty(req.detailDescription()),
                detailImages.isEmpty() ? "" : detailImages.get(0),
                req.level(),
                req.runType(),
                req.category(),
                instructor
        );

        target.replaceDetailImages(detailImages);

        classSessionRepository.deleteByClassProductId(classId);
        replaceSessions(target, req.sessions());

        return ClassDetailResponse.from(target);
    }

    public void deleteClass(Long actorUserId, boolean isAdmin, Long classId) {
        validateActor(actorUserId, isAdmin);

        if (classSessionRepository.existsByClassProductIdAndReservedCountGreaterThan(classId, 0)) {
            throw new BusinessException("예약 이력이 있는 클래스는 삭제할 수 없습니다.");
        }

        ClassProduct target = classProductRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("클래스를 찾을 수 없습니다. id=" + classId));

        classSessionRepository.deleteByClassProductId(classId);
        classProductRepository.delete(target);
    }

    private Instructor loadInstructor(Long instructorId) {
        return instructorRepository.findById(instructorId)
                .orElseThrow(() -> new BusinessException("강사를 찾을 수 없습니다. instructorId=" + instructorId));
    }

    private List<Long> replaceSessions(ClassProduct classProduct, List<ClassSessionCreateRequest> sessionRequests) {
        List<Long> createdSessionIds = new ArrayList<>();

        for (ClassSessionCreateRequest sessionReq : sessionRequests) {
            ClassSession session = classSessionRepository.save(
                    ClassSession.builder()
                            .classProduct(classProduct)
                            .startAt(sessionReq.startAt())
                            .slot(sessionReq.slot())
                            .capacity(sessionReq.capacity())
                            .price(sessionReq.price())
                            .build()
            );
            createdSessionIds.add(session.getId());
        }

        return createdSessionIds;
    }

    private void validateActor(Long actorUserId, boolean isAdmin) {
        if (actorUserId == null || actorUserId <= 0) {
            throw new BusinessException("로그인 정보가 필요합니다.");
        }
        if (!isAdmin) {
            throw new BusinessException("원데이 클래스는 관리자만 관리할 수 있습니다.");
        }
    }

    private void validateCreateRequest(ClassCreateRequest req) {
        validateCommon(
                req == null ? null : req.title(),
                req == null ? null : req.description(),
                req == null ? null : req.detailDescription(),
                req == null ? null : req.detailImageData(),
                req == null ? null : req.detailImageDataList(),
                req == null ? null : req.level(),
                req == null ? null : req.runType(),
                req == null ? null : req.category(),
                req == null ? null : req.instructorId(),
                req == null ? null : req.sessions()
        );
    }

    private void validateUpdateRequest(ClassUpdateRequest req) {
        validateCommon(
                req == null ? null : req.title(),
                req == null ? null : req.description(),
                req == null ? null : req.detailDescription(),
                req == null ? null : req.detailImageData(),
                req == null ? null : req.detailImageDataList(),
                req == null ? null : req.level(),
                req == null ? null : req.runType(),
                req == null ? null : req.category(),
                req == null ? null : req.instructorId(),
                req == null ? null : req.sessions()
        );
    }

    private void validateCommon(
            String title,
            String description,
            String detailDescription,
            String detailImageData,
            List<String> detailImageDataList,
            com.project.hanspoon.oneday.clazz.domain.Level level,
            com.project.hanspoon.oneday.clazz.domain.RunType runType,
            com.project.hanspoon.oneday.clazz.domain.RecipeCategory category,
            Long instructorId,
            List<ClassSessionCreateRequest> sessions
    ) {
        if (title == null && description == null && detailDescription == null
                && detailImageData == null && detailImageDataList == null && level == null && runType == null
                && category == null && instructorId == null && sessions == null) {
            throw new BusinessException("요청 값이 비어 있습니다.");
        }

        if (title == null || title.isBlank()) {
            throw new BusinessException("제목은 필수입니다.");
        }
        if (title.trim().length() > 80) {
            throw new BusinessException("제목은 최대 80자입니다.");
        }

        if (description != null && description.trim().length() > 4000) {
            throw new BusinessException("설명은 최대 4000자입니다.");
        }
        if (detailDescription != null && detailDescription.trim().length() > 12000) {
            throw new BusinessException("상세 설명은 최대 12000자입니다.");
        }

        List<String> normalizedDetailImages = normalizeDetailImages(detailImageData, detailImageDataList);
        if (normalizedDetailImages.size() > 10) {
            throw new BusinessException("상세 이미지는 최대 10장까지 등록할 수 있습니다.");
        }
        for (String imageData : normalizedDetailImages) {
            if (imageData.length() > 4_000_000) {
                throw new BusinessException("상세 이미지 데이터가 너무 큽니다. 2MB 이하 이미지를 사용해 주세요.");
            }
        }

        if (level == null) {
            throw new BusinessException("레벨은 필수입니다.");
        }
        if (runType == null) {
            throw new BusinessException("진행 방식은 필수입니다.");
        }
        if (category == null) {
            throw new BusinessException("분류는 필수입니다.");
        }
        if (instructorId == null || instructorId <= 0) {
            throw new BusinessException("강사 ID는 필수입니다.");
        }

        if (sessions == null || sessions.isEmpty()) {
            throw new BusinessException("수업 일정은 최소 1건 이상 필요합니다.");
        }
        if (sessions.size() > 20) {
            throw new BusinessException("수업 일정은 최대 20건까지 등록할 수 있습니다.");
        }

        for (int i = 0; i < sessions.size(); i++) {
            ClassSessionCreateRequest session = sessions.get(i);
            String prefix = "sessions[" + i + "] ";

            if (session == null) {
                throw new BusinessException(prefix + "값이 비어 있습니다.");
            }
            if (session.startAt() == null) {
                throw new BusinessException(prefix + "시작 시각은 필수입니다.");
            }
            if (session.startAt().isBefore(LocalDateTime.now())) {
                throw new BusinessException(prefix + "시작 시각은 현재 시각 이후여야 합니다.");
            }
            if (session.slot() == null) {
                throw new BusinessException(prefix + "시간대는 필수입니다.");
            }
            if (session.capacity() == null || session.capacity() <= 0) {
                throw new BusinessException(prefix + "정원은 1 이상이어야 합니다.");
            }
            if (session.price() == null || session.price() < 0) {
                throw new BusinessException(prefix + "가격은 0 이상이어야 합니다.");
            }
        }
    }

    private String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private List<String> normalizeDetailImages(String detailImageData, List<String> detailImageDataList) {
        List<String> result = new ArrayList<>();
        if (detailImageDataList != null) {
            for (String imageData : detailImageDataList) {
                String normalized = trimOrEmpty(imageData);
                if (!normalized.isEmpty()) {
                    result.add(normalized);
                }
            }
        }

        if (result.isEmpty()) {
            String single = trimOrEmpty(detailImageData);
            if (!single.isEmpty()) {
                result.add(single);
            }
        }

        return result;
    }
}
