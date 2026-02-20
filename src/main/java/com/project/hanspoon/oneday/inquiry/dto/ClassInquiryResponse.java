package com.project.hanspoon.oneday.inquiry.dto;

import com.project.hanspoon.oneday.inquiry.domain.Visibility;

import java.time.LocalDateTime;

public record ClassInquiryResponse(
        Long inquiryId,
        Long userId,
        String writerName,
        Long classProductId,
        String category,
        String title,
        String content,
        Visibility visibility,
        boolean hasAttachment,
        boolean answered,
        String answerContent,
        Long answeredByUserId,
        LocalDateTime answeredAt,
        boolean canViewContent,
        boolean canAnswer,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
