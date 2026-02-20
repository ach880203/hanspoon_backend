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

@Service
@RequiredArgsConstructor
@Transactional
public class ClassCommandService {

    private final ClassProductRepository classProductRepository;
    private final ClassSessionRepository classSessionRepository;
    private final InstructorRepository instructorRepository;

    // 관리자만 원데이 클래스를 등록할 수 있습니다.
    public ClassCreateResponse createClass(Long actorUserId, boolean isAdmin, ClassCreateRequest req) {
        if (actorUserId == null || actorUserId <= 0) {
            throw new BusinessException("로그인 정보가 필요합니다.");
        }
        if (!isAdmin) {
            throw new BusinessException("원데이 클래스 등록은 관리자만 가능합니다.");
        }
        validateRequest(req);

        Instructor instructor = instructorRepository.findById(req.instructorId())
                .orElseThrow(() -> new BusinessException("강사를 찾을 수 없습니다. id=" + req.instructorId()));

        ClassProduct product = classProductRepository.save(
                ClassProduct.builder()
                        .title(req.title().trim())
                        .description(safeTrim(req.description()))
                        .level(req.level())
                        .runType(req.runType())
                        .category(req.category())
                        .instructor(instructor)
                        .build()
        );

        List<Long> sessionIds = new ArrayList<>();
        for (ClassSessionCreateRequest s : req.sessions()) {
            validateSessionInput(s);

            ClassSession created = classSessionRepository.save(
                    ClassSession.builder()
                            .classProduct(product)
                            .startAt(s.startAt())
                            .slot(s.slot())
                            .capacity(s.capacity())
                            .price(s.price())
                            .build()
            );
            sessionIds.add(created.getId());
        }

        return new ClassCreateResponse(
                product.getId(),
                product.getTitle(),
                instructor.getId(),
                sessionIds.size(),
                sessionIds
        );
    }

    private void validateRequest(ClassCreateRequest req) {
        if (req == null) {
            throw new BusinessException("등록 요청 값이 없습니다.");
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
            throw new BusinessException("sessions는 1개 이상 필요합니다.");
        }
    }

    private void validateSessionInput(ClassSessionCreateRequest s) {
        if (s == null) {
            throw new BusinessException("세션 입력 값이 없습니다.");
        }
        if (s.startAt() == null) {
            throw new BusinessException("세션 startAt은 필수입니다.");
        }
        if (s.startAt().isBefore(LocalDateTime.now().minusMinutes(1))) {
            throw new BusinessException("세션 시작 시각은 현재 이후여야 합니다.");
        }
        if (s.slot() == null) {
            throw new BusinessException("세션 slot은 필수입니다.");
        }
        if (s.capacity() == null || s.capacity() <= 0) {
            throw new BusinessException("세션 capacity는 1 이상이어야 합니다.");
        }
        if (s.price() == null || s.price() < 0) {
            throw new BusinessException("세션 price는 0 이상이어야 합니다.");
        }
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
