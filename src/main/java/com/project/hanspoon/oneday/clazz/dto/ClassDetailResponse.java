package com.project.hanspoon.oneday.clazz.dto;

import com.project.hanspoon.oneday.clazz.domain.Level;
import com.project.hanspoon.oneday.clazz.domain.RecipeCategory;
import com.project.hanspoon.oneday.clazz.domain.RunType;
import com.project.hanspoon.oneday.clazz.entity.ClassProduct;

public record ClassDetailResponse(
        Long id,
        String title,
        String description,
        String detailDescription,
        String detailImageData,
        Level level,
        RunType runType,
        RecipeCategory category,
        Long instructorId,
        String instructorBio
) {
    public static ClassDetailResponse from(ClassProduct p) {
        Long instructorId = (p.getInstructor() != null) ? p.getInstructor().getId() : null;
        String instructorBio = (p.getInstructor() != null) ? p.getInstructor().getBio() : null;

        return new ClassDetailResponse(
                p.getId(),
                p.getTitle(),
                p.getDescription(),
                p.getDetailDescription(),
                p.getDetailImageData(),
                p.getLevel(),
                p.getRunType(),
                p.getCategory(),
                instructorId,
                instructorBio
        );
    }
}
