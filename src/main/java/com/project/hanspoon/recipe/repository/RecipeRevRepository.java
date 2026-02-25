package com.project.hanspoon.recipe.repository;

import com.project.hanspoon.recipe.entity.RecipeRev;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecipeRevRepository extends JpaRepository<RecipeRev, Long> {
    List<RecipeRev> findAllByUser_UserIdOrderByIdDesc(Long userId);
}

