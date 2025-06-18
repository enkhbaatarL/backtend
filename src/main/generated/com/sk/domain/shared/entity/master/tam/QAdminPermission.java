package com.sk.domain.shared.entity.master.tam;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAdminPermission is a Querydsl query type for AdminPermission
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAdminPermission extends EntityPathBase<AdminPermission> {

    private static final long serialVersionUID = 222253063L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAdminPermission adminPermission = new QAdminPermission("adminPermission");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final com.sk.domain.shared.entity.compositeKey.tam.QAdminPermissionId id;

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public final com.sk.domain.shared.entity.master.tmd.QUser user;

    public QAdminPermission(String variable) {
        this(AdminPermission.class, forVariable(variable), INITS);
    }

    public QAdminPermission(Path<? extends AdminPermission> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAdminPermission(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAdminPermission(PathMetadata metadata, PathInits inits) {
        this(AdminPermission.class, metadata, inits);
    }

    public QAdminPermission(Class<? extends AdminPermission> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.id = inits.isInitialized("id") ? new com.sk.domain.shared.entity.compositeKey.tam.QAdminPermissionId(forProperty("id")) : null;
        this.user = inits.isInitialized("user") ? new com.sk.domain.shared.entity.master.tmd.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

