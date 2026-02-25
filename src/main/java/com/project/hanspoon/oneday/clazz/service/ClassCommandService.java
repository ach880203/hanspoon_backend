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

/**
 * 원데이 클래스의 생성/수정/삭제(명령) 책임을 가진 서비스입니다.
 *
 * 초보자 참고:
 * - QueryService는 "조회", CommandService는 "데이터 변경"을 담당하도록 분리하면
 *   코드 책임이 명확해지고, 테스트/리뷰가 쉬워집니다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ClassCommandService {

    private final ClassProductRepository classProductRepository;
    private final ClassSessionRepository classSessionRepository;
    private final InstructorRepository instructorRepository;

    /**
     * 클래스 생성
     */
    public ClassCreateResponse createClass(Long actorUserId, boolean isAdmin, ClassCreateRequest req) {
        validateActor(actorUserId, isAdmin);
        validateCreateRequest(req);

        Instructor instructor = loadInstructor(req.instructorId());

        ClassProduct savedClass = classProductRepository.save(
                ClassProduct.builder()
                        .title(req.title().trim())
                        .description(trimOrEmpty(req.description()))
                        .detailDescription(trimOrEmpty(req.detailDescription()))
                        .detailImageData(trimOrEmpty(req.detailImageData()))
                        .level(req.level())
                        .runType(req.runType())
                        .category(req.category())
                        .instructor(instructor)
                        .build()
        );

        List<Long> createdSessionIds = replaceSessions(savedClass, req.sessions());

        return new ClassCreateResponse(
                savedClass.getId(),
                savedClass.getTitle(),
                createdSessionIds.size(),
                createdSessionIds
        );
    }

    /**
     * 클래스 수정
     *
     * 세션은 "전체 교체" 방식으로 다룹니다.
     * 예약이 이미 잡혀 있는 세션이 있으면 교체 시 데이터 불일치가 생길 수 있으므로
     * 안전을 위해 수정(특히 세션 변경)을 막습니다.
     */
    public ClassDetailResponse updateClass(Long actorUserId, boolean isAdmin, Long classId, ClassUpdateRequest req) {
        validateActor(actorUserId, isAdmin);
        validateUpdateRequest(req);

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
                trimOrEmpty(req.detailImageData()),
                req.level(),
                req.runType(),
                req.category(),
                instructor
        );

        classSessionRepository.deleteByClassProductId(classId);
        replaceSessions(target, req.sessions());

        return ClassDetailResponse.from(target);
    }

    /**
     * 클래스 삭제
     *
     * 예약 이력이 있으면 삭제를 차단합니다.
     * (실운영 데이터 보호 목적)
     */
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

    /**
     * 세션 전체 교체 유틸
     *
     * 초보자 참고:
     * - 등록/수정 모두 같은 세션 저장 로직을 사용하도록 묶으면
     *   중복 코드가 줄고, 수정 포인트가 한 곳으로 모여 유지보수가 쉬워집니다.
     */
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
            com.project.hanspoon.oneday.clazz.domain.Level level,
            com.project.hanspoon.oneday.clazz.domain.RunType runType,
            com.project.hanspoon.oneday.clazz.domain.RecipeCategory category,
            Long instructorId,
            List<ClassSessionCreateRequest> sessions
    ) {
        if (title == null && description == null && detailDescription == null
                && detailImageData == null && level == null && runType == null
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
        if (detailImageData != null && detailImageData.length() > 4_000_000) {
            throw new BusinessException("상세 이미지 데이터가 너무 큽니다. 2MB 이하 이미지를 사용해 주세요.");
        }

        if (level == null) {
            throw new BusinessException("난이도는 필수입니다.");
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
                throw new BusinessException(prefix + "회차는 필수입니다.");
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
}
