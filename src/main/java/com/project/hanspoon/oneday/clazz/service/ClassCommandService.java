package com.project.hanspoon.oneday.clazz.service;

import com.project.hanspoon.common.exception.BusinessException;
import com.project.hanspoon.oneday.clazz.dto.ClassCreateRequest;
import com.project.hanspoon.oneday.clazz.dto.ClassCreateResponse;
import com.project.hanspoon.oneday.clazz.dto.ClassSessionCreateRequest;
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
 * 원데이 클래스 "등록"처럼 데이터를 변경하는 기능을 담당하는 서비스입니다.
 * 초보자 참고:
 * - QueryService(조회)와 CommandService(등록/수정/삭제)를 분리하면
 *   코드를 읽을 때 책임이 명확해지고, 테스트도 쉬워집니다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ClassCommandService {

    private final ClassProductRepository classProductRepository;
    private final ClassSessionRepository classSessionRepository;
    private final InstructorRepository instructorRepository;

    /**
     * 클래스 + 세션을 한 번에 등록합니다.
     *
     * @param actorUserId 로그인 사용자 ID
     * @param isAdmin     관리자 여부 (현재 요구사항: 관리자만 등록 가능)
     * @param req         등록 요청값
     * @return 생성된 클래스/세션 요약
     */
    public ClassCreateResponse createClass(Long actorUserId, boolean isAdmin, ClassCreateRequest req) {
        validateActor(actorUserId, isAdmin);
        validateRequest(req);

        Instructor instructor = instructorRepository.findById(req.instructorId())
                .orElseThrow(() -> new BusinessException("강사를 찾을 수 없습니다. instructorId=" + req.instructorId()));

        ClassProduct savedClass = classProductRepository.save(
                ClassProduct.builder()
                        .title(req.title().trim())
                        .description(req.description() == null ? "" : req.description().trim())
                        .level(req.level())
                        .runType(req.runType())
                        .category(req.category())
                        .instructor(instructor)
                        .build()
        );

        // 초보자 참고:
        // 세션은 개별 엔티티이므로, 요청 배열을 순회하면서 하나씩 생성합니다.
        List<Long> createdSessionIds = new ArrayList<>();
        for (ClassSessionCreateRequest sessionReq : req.sessions()) {
            ClassSession session = classSessionRepository.save(
                    ClassSession.builder()
                            .classProduct(savedClass)
                            .startAt(sessionReq.startAt())
                            .slot(sessionReq.slot())
                            .capacity(sessionReq.capacity())
                            .price(sessionReq.price())
                            .build()
            );
            createdSessionIds.add(session.getId());
        }

        return new ClassCreateResponse(
                savedClass.getId(),
                savedClass.getTitle(),
                createdSessionIds.size(),
                createdSessionIds
        );
    }

    private void validateActor(Long actorUserId, boolean isAdmin) {
        if (actorUserId == null || actorUserId <= 0) {
            throw new BusinessException("로그인 정보가 필요합니다.");
        }
        if (!isAdmin) {
            throw new BusinessException("원데이 클래스 등록은 관리자만 가능합니다.");
        }
    }

    private void validateRequest(ClassCreateRequest req) {
        if (req == null) {
            throw new BusinessException("등록 요청값이 없습니다.");
        }
        if (req.title() == null || req.title().isBlank()) {
            throw new BusinessException("title은 필수입니다.");
        }
        if (req.title().trim().length() > 80) {
            throw new BusinessException("title은 최대 80자입니다.");
        }
        if (req.level() == null) {
            throw new BusinessException("level은 필수입니다.");
        }
        if (req.runType() == null) {
            throw new BusinessException("runType은 필수입니다.");
        }
        if (req.category() == null) {
            throw new BusinessException("category는 필수입니다.");
        }
        if (req.instructorId() == null || req.instructorId() <= 0) {
            throw new BusinessException("instructorId는 필수입니다.");
        }
        if (req.sessions() == null || req.sessions().isEmpty()) {
            throw new BusinessException("sessions는 최소 1건 이상 필요합니다.");
        }
        if (req.sessions().size() > 20) {
            throw new BusinessException("sessions는 최대 20건까지 등록할 수 있습니다.");
        }

        for (int i = 0; i < req.sessions().size(); i++) {
            ClassSessionCreateRequest session = req.sessions().get(i);
            String prefix = "sessions[" + i + "] ";

            if (session == null) {
                throw new BusinessException(prefix + "값이 비어 있습니다.");
            }
            if (session.startAt() == null) {
                throw new BusinessException(prefix + "startAt은 필수입니다.");
            }
            if (session.startAt().isBefore(LocalDateTime.now())) {
                throw new BusinessException(prefix + "startAt은 현재 시각 이후여야 합니다.");
            }
            if (session.slot() == null) {
                throw new BusinessException(prefix + "slot은 필수입니다.");
            }
            if (session.capacity() == null || session.capacity() <= 0) {
                throw new BusinessException(prefix + "capacity는 1 이상이어야 합니다.");
            }
            if (session.price() == null || session.price() < 0) {
                throw new BusinessException(prefix + "price는 0 이상이어야 합니다.");
            }
        }
    }
}
