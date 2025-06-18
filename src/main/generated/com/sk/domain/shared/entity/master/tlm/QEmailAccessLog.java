package com.sk.domain.shared.entity.master.tlm;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QEmailAccessLog is a Querydsl query type for EmailAccessLog
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEmailAccessLog extends EntityPathBase<EmailAccessLog> {

    private static final long serialVersionUID = -1336450458L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QEmailAccessLog emailAccessLog = new QEmailAccessLog("emailAccessLog");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final StringPath emailCntt = createString("emailCntt");

    public final StringPath emailReceiveUserLoginId = createString("emailReceiveUserLoginId");

    public final DateTimePath<java.time.LocalDateTime> emailSendDtm = createDateTime("emailSendDtm", java.time.LocalDateTime.class);

    public final StringPath emailSendErrCntt = createString("emailSendErrCntt");

    public final NumberPath<Integer> emailSendSn = createNumber("emailSendSn", Integer.class);

    public final EnumPath<com.sk.domain.shared.enums.EmailSendStatusName> emailSendStatusNm = createEnum("emailSendStatusNm", com.sk.domain.shared.enums.EmailSendStatusName.class);

    public final StringPath emailTitleNm = createString("emailTitleNm");

    public final com.sk.domain.shared.entity.master.tmd.QGoodsCriticalValueSetting goodsCriticalValueSetting;

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final BooleanPath isEmailInquiry = createBoolean("isEmailInquiry");

    public final BooleanPath isSendSuccess = createBoolean("isSendSuccess");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public QEmailAccessLog(String variable) {
        this(EmailAccessLog.class, forVariable(variable), INITS);
    }

    public QEmailAccessLog(Path<? extends EmailAccessLog> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QEmailAccessLog(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QEmailAccessLog(PathMetadata metadata, PathInits inits) {
        this(EmailAccessLog.class, metadata, inits);
    }

    public QEmailAccessLog(Class<? extends EmailAccessLog> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.goodsCriticalValueSetting = inits.isInitialized("goodsCriticalValueSetting") ? new com.sk.domain.shared.entity.master.tmd.QGoodsCriticalValueSetting(forProperty("goodsCriticalValueSetting"), inits.get("goodsCriticalValueSetting")) : null;
    }

}

