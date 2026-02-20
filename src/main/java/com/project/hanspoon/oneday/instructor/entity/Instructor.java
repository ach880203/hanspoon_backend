package com.project.hanspoon.oneday.instructor.entity;

import com.project.hanspoon.common.entity.BaseTimeEntity;
import com.project.hanspoon.common.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "instructor")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Instructor extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;


    @Column(nullable = false, length = 1000)
    private String bio;

    @Builder
    private Instructor(User user, String bio) {
        this.user = user;
        this.bio = bio;
    }

    
}
