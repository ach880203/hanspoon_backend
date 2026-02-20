package com.project.hanspoon.recipe.entity;

import com.project.hanspoon.common.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name="recipe_ing")
@Getter
@Setter
public class RecipeIng { //문의글

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ing_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    @Lob
    private String content; //문의내용

    @Lob
    private String answer; //답글 내용

    @Column(columnDefinition = "boolean default false")
    private boolean isAnswered; //답글 여부

}
