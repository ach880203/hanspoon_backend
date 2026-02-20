package com.project.hanspoon.recipe.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecipeRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "main_recipe_id")
    private Recipe mainRecipe;

    @ManyToOne
    @JoinColumn(name = "sub_recipe_id")
    private Recipe subRecipe;
}
