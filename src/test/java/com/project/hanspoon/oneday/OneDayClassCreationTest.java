package com.project.hanspoon.oneday;

import com.project.hanspoon.oneday.clazz.domain.Level;
import com.project.hanspoon.oneday.clazz.domain.RecipeCategory;
import com.project.hanspoon.oneday.clazz.domain.RunType;
import com.project.hanspoon.oneday.clazz.domain.SessionSlot;
import com.project.hanspoon.oneday.clazz.entity.ClassProduct;
import com.project.hanspoon.oneday.clazz.entity.ClassSession;
import com.project.hanspoon.oneday.clazz.repository.ClassProductRepository;
import com.project.hanspoon.oneday.clazz.repository.ClassSessionRepository;
import com.project.hanspoon.oneday.instructor.entity.Instructor;
import com.project.hanspoon.oneday.instructor.repository.InstructorRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@SpringBootTest
class OneDayClassCreationTest {

    @Autowired
    private InstructorRepository instructorRepository;


    @Autowired
    private ClassProductRepository classProductRepository;

    @Autowired
    private ClassSessionRepository classSessionRepository;

        @Test
    @DisplayName("원데이 클래스 생성: 난이도 3종 + 오전/오후 세션 + 이벤트 1개")
    void createOneDayClassesWithSessionsAndEvent() {
        // @SpringBootTest를 사용하는 이유:
        // JPA 엔티티, 리포지토리, 트랜잭션 설정까지 실제 애플리케이션과 같은 방식으로 검증하기 위함입니다.
        //
        // @Transactional을 붙인 이유:
        // 테스트가 끝나면 기본적으로 롤백되어, 테스트 데이터가 DB에 누적되지 않도록 하기 위함입니다.
        Instructor instructor = instructorRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("테스트에 사용할 강사 데이터가 필요합니다."));

        // 난이도 3개(초급/중급/고급) 상시 클래스 생성
        createClassWithTwoSessions(
                "초급 한식 클래스",
                "초급자를 위한 한식 기본기 수업",
                Level.BEGINNER,
                RunType.ALWAYS,
                RecipeCategory.KOREAN,
                instructor,
                LocalDateTime.now().plusDays(3)
        );

        createClassWithTwoSessions(
                "중급 베이킹 클래스",
                "중급자를 위한 베이킹 실습 수업",
                Level.INTERMEDIATE,
                RunType.ALWAYS,
                RecipeCategory.BAKERY,
                instructor,
                LocalDateTime.now().plusDays(4)
        );

        createClassWithTwoSessions(
                "고급 한식 클래스",
                "고급자를 위한 플레이팅 심화 수업",
                Level.ADVANCED,
                RunType.ALWAYS,
                RecipeCategory.KOREAN,
                instructor,
                LocalDateTime.now().plusDays(5)
        );

        // 이벤트 클래스 1개 생성
        // RunType.EVENT를 명시해서 홈의 이벤트 클래스 영역에 포함될 수 있게 합니다.
        createClassWithTwoSessions(
                "이벤트 베이킹 클래스",
                "이벤트 전용 한정 클래스",
                Level.BEGINNER,
                RunType.EVENT,
                RecipeCategory.BAKERY,
                instructor,
                LocalDateTime.now().plusDays(2)
        );

        long createdClassCount = classProductRepository.count();
        long createdSessionCount = classSessionRepository.count();

        // 총 클래스 4개, 각 클래스당 AM/PM 2세션이므로 총 8세션이 생성되어야 합니다.
        Assertions.assertTrue(createdClassCount >= 4, "원데이 클래스가 최소 4개 이상 생성되어야 합니다.");
        Assertions.assertTrue(createdSessionCount >= 8, "원데이 세션이 최소 8개 이상 생성되어야 합니다.");
    }

    private void createClassWithTwoSessions(
            String title,
            String description,
            Level level,
            RunType runType,
            RecipeCategory category,
            Instructor instructor,
            LocalDateTime baseDateTime
    ) {
        ClassProduct classProduct = classProductRepository.save(
                ClassProduct.builder()
                        .title(title)
                        .description(description)
                        .level(level)
                        .runType(runType)
                        .category(category)
                        .instructor(instructor)
                        .build()
        );

        // 오전 세션(AM)
        classSessionRepository.save(
                ClassSession.builder()
                        .classProduct(classProduct)
                        .startAt(baseDateTime.withHour(10).withMinute(0).withSecond(0).withNano(0))
                        .slot(SessionSlot.AM)
                        .capacity(10)
                        .price(50000)
                        .build()
        );

        // 오후 세션(PM)
        classSessionRepository.save(
                ClassSession.builder()
                        .classProduct(classProduct)
                        .startAt(baseDateTime.withHour(15).withMinute(0).withSecond(0).withNano(0))
                        .slot(SessionSlot.PM)
                        .capacity(10)
                        .price(55000)
                        .build()
        );
    }
}
