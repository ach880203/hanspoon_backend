package com.project.hanspoon.oneday;

import com.project.hanspoon.common.user.entity.User;
import com.project.hanspoon.common.user.repository.UserRepository;
import com.project.hanspoon.oneday.instructor.entity.Instructor;
import com.project.hanspoon.oneday.instructor.repository.InstructorRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
//@Transactional
class InstructorCreationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    @Test
    @DisplayName("강사 생성: 사용자 생성 후 강사 등록 및 조회")
    void createInstructor() {
        String seed = String.valueOf(System.nanoTime());

        User user = userRepository.save(
                User.builder()
                        .email("instructor-test-" + seed + "@example.com")
                        .password("encoded-test-password")
                        .userName("강사테스트" + seed)
                        .phone("010-0000-0000")
                        .address("Seoul")
                        .build()
        );

        Instructor saved = instructorRepository.save(
                Instructor.builder()
                        .user(user)
                        .bio("원데이 테스트 강사 소개")
                        .build()
        );

        Assertions.assertNotNull(saved.getId(), "강사 ID가 생성되어야 합니다.");

        Instructor found = instructorRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new AssertionError("userId로 강사를 조회하지 못했습니다."));

        Assertions.assertEquals(saved.getId(), found.getId(), "저장된 강사와 조회된 강사 ID가 같아야 합니다.");
        Assertions.assertEquals("원데이 테스트 강사 소개", found.getBio(), "강사 소개(bio)가 일치해야 합니다.");
        Assertions.assertEquals(user.getUserId(), found.getUser().getUserId(), "강사-사용자 연결이 유지되어야 합니다.");
    }
}
