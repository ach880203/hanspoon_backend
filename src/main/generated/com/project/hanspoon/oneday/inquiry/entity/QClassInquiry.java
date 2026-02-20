package com.project.hanspoon.oneday.inquiry.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QClassInquiry is a Querydsl query type for ClassInquiry
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QClassInquiry extends EntityPathBase<ClassInquiry> {

    private static final long serialVersionUID = 396087979L;

    public static final QClassInquiry classInquiry = new QClassInquiry("classInquiry");

    public final com.project.hanspoon.common.entity.QBaseTimeEntity _super = new com.project.hanspoon.common.entity.QBaseTimeEntity(this);

    public final StringPath answerContent = createString("answerContent");

    public final BooleanPath answered = createBoolean("answered");

    public final DateTimePath<java.time.LocalDateTime> answeredAt = createDateTime("answeredAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> answeredByUserId = createNumber("answeredByUserId", Long.class);

    public final StringPath category = createString("category");

    public final NumberPath<Long> classId = createNumber("classId", Long.class);

    public final NumberPath<Long> classProductId = createNumber("classProductId", Long.class);

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final BooleanPath hasAttachment = createBoolean("hasAttachment");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath secret = createBoolean("secret");

    public final EnumPath<com.project.hanspoon.oneday.inquiry.domain.InquiryStatus> status = createEnum("status", com.project.hanspoon.oneday.inquiry.domain.InquiryStatus.class);

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public final EnumPath<com.project.hanspoon.oneday.inquiry.domain.Visibility> visibility = createEnum("visibility", com.project.hanspoon.oneday.inquiry.domain.Visibility.class);

    public QClassInquiry(String variable) {
        super(ClassInquiry.class, forVariable(variable));
    }

    public QClassInquiry(Path<? extends ClassInquiry> path) {
        super(path.getType(), path.getMetadata());
    }

    public QClassInquiry(PathMetadata metadata) {
        super(ClassInquiry.class, metadata);
    }

}

