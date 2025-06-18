package com.sk.domain.shared.entity.master.tlm;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUserAccessLog is a Querydsl query type for UserAccessLog
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserAccessLog extends EntityPathBase<UserAccessLog> {

    private static final long serialVersionUID = 1096723475L;

    public static final QUserAccessLog userAccessLog = new QUserAccessLog("userAccessLog");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public final NumberPath<Integer> userAccessSequence = createNumber("userAccessSequence", Integer.class);

    public final StringPath userLoginId = createString("userLoginId");

    public QUserAccessLog(String variable) {
        super(UserAccessLog.class, forVariable(variable));
    }

    public QUserAccessLog(Path<? extends UserAccessLog> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserAccessLog(PathMetadata metadata) {
        super(UserAccessLog.class, metadata);
    }

}

