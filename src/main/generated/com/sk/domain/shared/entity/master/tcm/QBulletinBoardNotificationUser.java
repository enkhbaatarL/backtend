package com.sk.domain.shared.entity.master.tcm;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBulletinBoardNotificationUser is a Querydsl query type for BulletinBoardNotificationUser
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBulletinBoardNotificationUser extends EntityPathBase<BulletinBoardNotificationUser> {

    private static final long serialVersionUID = -1682148228L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBulletinBoardNotificationUser bulletinBoardNotificationUser = new QBulletinBoardNotificationUser("bulletinBoardNotificationUser");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    public final QBulletinBoard bulletinBoard;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final com.sk.domain.shared.entity.compositeKey.tcm.QBulletinBoardUserMappingId id;

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final BooleanPath isRead = createBoolean("isRead");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public final com.sk.domain.shared.entity.master.tmd.QUser user;

    public QBulletinBoardNotificationUser(String variable) {
        this(BulletinBoardNotificationUser.class, forVariable(variable), INITS);
    }

    public QBulletinBoardNotificationUser(Path<? extends BulletinBoardNotificationUser> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBulletinBoardNotificationUser(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBulletinBoardNotificationUser(PathMetadata metadata, PathInits inits) {
        this(BulletinBoardNotificationUser.class, metadata, inits);
    }

    public QBulletinBoardNotificationUser(Class<? extends BulletinBoardNotificationUser> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.bulletinBoard = inits.isInitialized("bulletinBoard") ? new QBulletinBoard(forProperty("bulletinBoard")) : null;
        this.id = inits.isInitialized("id") ? new com.sk.domain.shared.entity.compositeKey.tcm.QBulletinBoardUserMappingId(forProperty("id")) : null;
        this.user = inits.isInitialized("user") ? new com.sk.domain.shared.entity.master.tmd.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

