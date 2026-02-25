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
    private static final int MAX_DETAIL_IMAGE_DATA_LENGTH = 72_000_000; // Base64(DataURL) 기준, 약 50MB 원본 이미지 허용

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

        // ??⑤㈇?????닿뎄 ????嶺뚯솘????筌먲퐣議???戮?맋?????곌랙?х뙴???됀???⑤벡夷???㉱?洹먮뿫????덈펲.
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
                .orElseThrow(() -> new BusinessException("??????? 嶺뚢돦堉??????怨룸????덈펲. id=" + classId));

        if (classSessionRepository.existsByClassProductIdAndReservedCountGreaterThan(classId, 0)) {
            throw new BusinessException("???고뒎 ?????????덈츎 ???????노츎 ??瑜곸젧??????怨룸????덈펲.");
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
            throw new BusinessException("???고뒎 ?????????덈츎 ???????노츎 ?????????怨룸????덈펲.");
        }

        ClassProduct target = classProductRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("??????? 嶺뚢돦堉??????怨룸????덈펲. id=" + classId));

        classSessionRepository.deleteByClassProductId(classId);
        classProductRepository.delete(target);
    }

    private Instructor loadInstructor(Long instructorId) {
        return instructorRepository.findById(instructorId)
                .orElseThrow(() -> new BusinessException("?띠룆踰→쾮?㏓ご?嶺뚢돦堉??????怨룸????덈펲. instructorId=" + instructorId));
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
            throw new BusinessException("?β돦裕????筌먲퐢沅뽪뤆?쎛 ?熬곣뫗???紐껊퉵??");
        }
        if (!isAdmin) {
            throw new BusinessException("?????????????노츎 ??㉱?洹먮봿?썹춯???㉱?洹먮뿫留??????곕????덈펲.");
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
            throw new BusinessException("??븐슙???띠룆??????닷젆????곕????덈펲.");
        }

        if (title == null || title.isBlank()) {
            throw new BusinessException("??類쏄콬?? ?熬곣뫖????낅퉵??");
        }
        if (title.trim().length() > 80) {
            throw new BusinessException("??類쏄콬?? 嶺뚣끉裕? 80???肉???덈펲.");
        }

        if (description != null && description.trim().length() > 4000) {
            throw new BusinessException("???닿뎄?? 嶺뚣끉裕? 4000???肉???덈펲.");
        }
        if (detailDescription != null && detailDescription.trim().length() > 12000) {
            throw new BusinessException("??⑤㈇?????닿뎄?? 嶺뚣끉裕? 12000???肉???덈펲.");
        }

        List<String> normalizedDetailImages = normalizeDetailImages(detailImageData, detailImageDataList);
        if (normalizedDetailImages.size() > 10) {
            throw new BusinessException("??⑤㈇??????嶺뚯솘???嶺뚣끉裕? 10?鰲??먯?? ?繹먮굞夷???????곕????덈펲.");
        }
        for (String imageData : normalizedDetailImages) {
            if (imageData.length() > MAX_DETAIL_IMAGE_DATA_LENGTH) {
                throw new BusinessException("상세 이미지 데이터가 너무 큽니다. 50MB 이하 이미지를 사용해 주세요.");
            }
        }

        if (level == null) {
            throw new BusinessException("???뉖낵?? ?熬곣뫖????낅퉵??");
        }
        if (runType == null) {
            throw new BusinessException("嶺뚯쉳?듸쭛??꾩렮維??? ?熬곣뫖????낅퉵??");
        }
        if (category == null) {
            throw new BusinessException("?釉뚯뫊筌???熬곣뫖????낅퉵??");
        }
        if (instructorId == null || instructorId <= 0) {
            throw new BusinessException("?띠룆踰→쾮?ID???熬곣뫖????낅퉵??");
        }

        if (sessions == null || sessions.isEmpty()) {
            throw new BusinessException("??琉우뵜 ??源놁젧?? 嶺뚣끉裕??1濾???怨대쭜 ?熬곣뫗???紐껊퉵??");
        }
        if (sessions.size() > 20) {
            throw new BusinessException("??琉우뵜 ??源놁젧?? 嶺뚣끉裕? 20濾곌쑬???먯?? ?繹먮굞夷???????곕????덈펲.");
        }

        for (int i = 0; i < sessions.size(); i++) {
            ClassSessionCreateRequest session = sessions.get(i);
            String prefix = "sessions[" + i + "] ";

            if (session == null) {
                throw new BusinessException(prefix + "?띠룆??????닷젆????곕????덈펲.");
            }
            if (session.startAt() == null) {
                throw new BusinessException(prefix + "??戮곗굚 ??蹂?뜜?? ?熬곣뫖????낅퉵??");
            }
            if (session.startAt().isBefore(LocalDateTime.now())) {
                throw new BusinessException(prefix + "??戮곗굚 ??蹂?뜜?? ?熬곣뫗????蹂?뜜 ??袁⑸쐩??????紐껊퉵??");
            }
            if (session.slot() == null) {
                throw new BusinessException(prefix + "??蹂?뜟?????熬곣뫖????낅퉵??");
            }
            if (session.capacity() == null || session.capacity() <= 0) {
                throw new BusinessException(prefix + "?筌먦끉??? 1 ??怨대쭜??怨룹꽑????紐껊퉵??");
            }
            if (session.price() == null || session.price() < 0) {
                throw new BusinessException(prefix + "?띠럾??롪봇維? 0 ??怨대쭜??怨룹꽑????紐껊퉵??");
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




