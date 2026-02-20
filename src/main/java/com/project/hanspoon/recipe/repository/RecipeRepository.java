package com.project.hanspoon.recipe.repository;

import com.project.hanspoon.recipe.constant.Category;
import com.project.hanspoon.recipe.entity.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    Optional<Recipe> findByTitle(String title);

    Page<Recipe> findByTitleContainingAndDeletedFalse(String keyword, Pageable pageable);

    Page<Recipe> findByCategoryAndTitleContainingAndDeletedFalse
            (Category category, String keyword, Pageable pageable);

    Page<Recipe> findByDeletedFalse(Pageable pageable);

    List<Recipe> findByDeletedTrueAndCategory(Category category);

    List<Recipe> findByDeletedTrue();
}
