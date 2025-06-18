package com.sk.domain.shared.entity.compositeKey.tam;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QAdminPermissionId is a Querydsl query type for AdminPermissionId
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QAdminPermissionId extends BeanPath<AdminPermissionId> {

    private static final long serialVersionUID = -584171336L;

    public static final QAdminPermissionId adminPermissionId = new QAdminPermissionId("adminPermissionId");

    public final DateTimePath<java.time.LocalDateTime> applyDt = createDateTime("applyDt", java.time.LocalDateTime.class);

    public final StringPath ipAddress = createString("ipAddress");

    public final NumberPath<Integer> userNo = createNumber("userNo", Integer.class);

    public QAdminPermissionId(String variable) {
        super(AdminPermissionId.class, forVariable(variable));
    }

    public QAdminPermissionId(Path<? extends AdminPermissionId> path) {
        super(path.getType(), path.getMetadata());
    }

    public QAdminPermissionId(PathMetadata metadata) {
        super(AdminPermissionId.class, metadata);
    }

}

