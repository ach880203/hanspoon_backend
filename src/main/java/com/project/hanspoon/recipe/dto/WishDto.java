package com.project.hanspoon.recipe.dto;

import com.project.hanspoon.recipe.entity.Recipe;
import com.project.hanspoon.recipe.entity.RecipeWish;
import lombok.*;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WishDto {
    private Long id;
    private Long userId;
    private Long wishId;
    private String title;
    private String mainImage;

    public WishDto(RecipeWish recipeWish, Recipe recipe) {
        this.id = recipe.getId();
        this.userId = recipeWish.getId();
        this.wishId = recipeWish.getUser().getUserId();
        this.title = recipe.getTitle();
        this.mainImage = recipe.getRecipeImg();
    }

}
