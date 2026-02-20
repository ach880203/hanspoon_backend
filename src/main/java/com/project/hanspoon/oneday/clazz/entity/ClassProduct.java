package com.project.hanspoon.oneday.clazz.entity;

import com.project.hanspoon.common.entity.BaseTimeEntity;
import com.project.hanspoon.oneday.clazz.domain.Level;
import com.project.hanspoon.oneday.clazz.domain.RecipeCategory;
import com.project.hanspoon.oneday.clazz.domain.RunType;
import com.project.hanspoon.oneday.instructor.entity.Instructor;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "class_product")
public class ClassProduct extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Level level;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RunType runType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecipeCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instructor_id", foreignKey =
                    @ForeignKey(name = "fk_class_product_instructor"))
    private Instructor instructor;

    @OneToMany(mappedBy = "classProduct", cascade = CascadeType.ALL
                                , orphanRemoval = true)
    private List<ClassSession> session = new ArrayList<>();

    @Builder
    public ClassProduct(String title, String description, Level level,
                        RunType runType, RecipeCategory category,
                        Instructor instructor){
        this.title = title;
        this.description = description;
        this.level = level;
        this.runType = runType;
        this.category = category;
        this.instructor = instructor;

    }
}
