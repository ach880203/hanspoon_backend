package com.project.hanspoon.oneday.inquiry.dto;

public record ClassInquiryCreateRequest(
        Long classProductId,
        String category,
        String title,
        String content,
        Boolean secret,
        Boolean hasAttachment
) {}
