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
    private static final int MAX_DETAIL_IMAGE_DATA_LENGTH = 72_000_000; // Base64(DataURL) 湲곗?, ??50MB ?먮낯 ?대?吏 ?덉슜
    private static final int MAX_SESSION_COUNT = 120;

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

        // ???ㅳ늾??????용럡 ????癲ル슣?????嶺뚮㉡?ｈ????筌?留?????怨뚮옓????????????ㅻ깹鸚????굿?域밸Ŧ肉?????덊렡.
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
                .orElseThrow(() -> new BusinessException("??????? 癲ル슓??젆???????⑤８?????덊렡. id=" + classId));

        if (classSessionRepository.existsByClassProductIdAndReservedCountGreaterThan(classId, 0)) {
            throw new BusinessException("???怨좊뭿 ??????????덉툗 ????????몄툗 ???쒓낯????????⑤８?????덊렡.");
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
            throw new BusinessException("???怨좊뭿 ??????????덉툗 ????????몄툗 ??????????⑤８?????덊렡.");
        }

        ClassProduct target = classProductRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("??????? 癲ル슓??젆???????⑤８?????덊렡. id=" + classId));

        classSessionRepository.deleteByClassProductId(classId);
        classProductRepository.delete(target);
    }

    private Instructor loadInstructor(Long instructorId) {
        return instructorRepository.findById(instructorId)
                .orElseThrow(() -> new BusinessException("??좊즴甕겸넂苡??볝걫?癲ル슓??젆???????⑤８?????덊렡. instructorId=" + instructorId));
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
            throw new BusinessException("?棺??짆????嶺뚮㉡?€쾮戮る쨬??쎛 ??ш끽維???筌뤾퍓???");
        }
        if (!isAdmin) {
            throw new BusinessException("??????????????몄툗 ???굿?域밸Ŧ遊??뱀땡????굿?域밸Ŧ肉ワ쭕??????怨?????덊렡.");
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
            throw new BusinessException("??釉먯뒜????좊즴???????룹젂????怨?????덊렡.");
        }

        if (title == null || title.isBlank()) {
            throw new BusinessException("??筌먯룄肄?? ??ш끽維?????낇돲??");
        }
        if (title.trim().length() > 80) {
            throw new BusinessException("??筌먯룄肄?? 癲ル슔?됭짆? 80????????덊렡.");
        }

        if (description != null && description.trim().length() > 4000) {
            throw new BusinessException("????용럡?? 癲ル슔?됭짆? 4000????????덊렡.");
        }
        if (detailDescription != null && detailDescription.trim().length() > 12000) {
            throw new BusinessException("???ㅳ늾??????용럡?? 癲ル슔?됭짆? 12000????????덊렡.");
        }

        List<String> normalizedDetailImages = normalizeDetailImages(detailImageData, detailImageDataList);
        if (normalizedDetailImages.size() > 10) {
            throw new BusinessException("???ㅳ늾??????癲ル슣????癲ル슔?됭짆? 10?欲꼲??癒?? ?濚밸Ŧ援욃ㅇ???????怨?????덊렡.");
        }
        for (String imageData : normalizedDetailImages) {
            if (imageData.length() > MAX_DETAIL_IMAGE_DATA_LENGTH) {
                throw new BusinessException("?곸꽭 ?대?吏 ?곗씠?곌? ?덈Т ?쎈땲?? 50MB ?댄븯 ?대?吏瑜??ъ슜??二쇱꽭??");
            }
        }

        if (level == null) {
            throw new BusinessException("????뽯궢?? ??ш끽維?????낇돲??");
        }
        if (runType == null) {
            throw new BusinessException("癲ル슣???몄춿??袁⑸젻泳??? ??ш끽維?????낇돲??");
        }
        if (category == null) {
            throw new BusinessException("??됰슣維딁춯????ш끽維?????낇돲??");
        }
        if (instructorId == null || instructorId <= 0) {
            throw new BusinessException("??좊즴甕겸넂苡?ID????ш끽維?????낇돲??");
        }

        if (sessions == null || sessions.isEmpty()) {
            throw new BusinessException("??筌뚯슦逾???繹먮냱??? 癲ル슔?됭짆??1癲????⑤?彛???ш끽維???筌뤾퍓???");
        }
        if (sessions.size() > MAX_SESSION_COUNT) {
            throw new BusinessException("세션은 최대 120개까지 등록할 수 있습니다.");
        }

        for (int i = 0; i < sessions.size(); i++) {
            ClassSessionCreateRequest session = sessions.get(i);
            String prefix = "sessions[" + i + "] ";

            if (session == null) {
                throw new BusinessException(prefix + "??좊즴???????룹젂????怨?????덊렡.");
            }
            if (session.startAt() == null) {
                throw new BusinessException(prefix + "??筌믨퀣援???癰???? ??ш끽維?????낇돲??");
            }
            if (session.startAt().isBefore(LocalDateTime.now())) {
                throw new BusinessException(prefix + "??筌믨퀣援???癰???? ??ш끽維????癰?????熬곣뫖???????筌뤾퍓???");
            }
            if (session.slot() == null) {
                throw new BusinessException(prefix + "??癰????????ш끽維?????낇돲??");
            }
            if (session.capacity() == null || session.capacity() <= 0) {
                throw new BusinessException(prefix + "?嶺뚮Ĳ???? 1 ???⑤?彛???⑤９苑????筌뤾퍓???");
            }
            if (session.price() == null || session.price() < 0) {
                throw new BusinessException(prefix + "??좊읈??濡る큸泳? 0 ???⑤?彛???⑤９苑????筌뤾퍓???");
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




