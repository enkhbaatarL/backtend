package com.sk.domain.shared.entity.base;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QRegisterBaseEntity is a Querydsl query type for RegisterBaseEntity
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultSupertypeSerializer")
public class QRegisterBaseEntity extends EntityPathBase<RegisterBaseEntity> {

    private static final long serialVersionUID = -1454227247L;

    public static final QRegisterBaseEntity registerBaseEntity = new QRegisterBaseEntity("registerBaseEntity");

    public final DateTimePath<java.time.LocalDateTime> createDt = createDateTime("createDt", java.time.LocalDateTime.class);

    public final StringPath createUserId = createString("createUserId");

    public QRegisterBaseEntity(String variable) {
        super(RegisterBaseEntity.class, forVariable(variable));
    }

    public QRegisterBaseEntity(Path<? extends RegisterBaseEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QRegisterBaseEntity(PathMetadata metadata) {
        super(RegisterBaseEntity.class, metadata);
    }

}

