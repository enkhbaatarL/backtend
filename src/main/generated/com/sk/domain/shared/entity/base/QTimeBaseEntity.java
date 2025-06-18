package com.sk.domain.shared.entity.base;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QTimeBaseEntity is a Querydsl query type for TimeBaseEntity
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultSupertypeSerializer")
public class QTimeBaseEntity extends EntityPathBase<TimeBaseEntity> {

    private static final long serialVersionUID = 587572603L;

    public static final QTimeBaseEntity timeBaseEntity = new QTimeBaseEntity("timeBaseEntity");

    public final DateTimePath<java.time.LocalDateTime> createDt = createDateTime("createDt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> modifyDt = createDateTime("modifyDt", java.time.LocalDateTime.class);

    public QTimeBaseEntity(String variable) {
        super(TimeBaseEntity.class, forVariable(variable));
    }

    public QTimeBaseEntity(Path<? extends TimeBaseEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QTimeBaseEntity(PathMetadata metadata) {
        super(TimeBaseEntity.class, metadata);
    }

}

