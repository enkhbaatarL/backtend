package com.sk.domain.shared.entity.master.tlm;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QApiAccessLog is a Querydsl query type for ApiAccessLog
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QApiAccessLog extends EntityPathBase<ApiAccessLog> {

    private static final long serialVersionUID = -1759833560L;

    public static final QApiAccessLog apiAccessLog = new QApiAccessLog("apiAccessLog");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    public final StringPath accessIpAddress = createString("accessIpAddress");

    public final NumberPath<Integer> apiAccessSn = createNumber("apiAccessSn", Integer.class);

    public final StringPath apiEndpointUrl = createString("apiEndpointUrl");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final StringPath errorMessageCntt = createString("errorMessageCntt");

    public final StringPath httpMethodNm = createString("httpMethodNm");

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public final StringPath requestDataCntt = createString("requestDataCntt");

    public final StringPath userLoginId = createString("userLoginId");

    public final StringPath userRoleCd = createString("userRoleCd");

    public QApiAccessLog(String variable) {
        super(ApiAccessLog.class, forVariable(variable));
    }

    public QApiAccessLog(Path<? extends ApiAccessLog> path) {
        super(path.getType(), path.getMetadata());
    }

    public QApiAccessLog(PathMetadata metadata) {
        super(ApiAccessLog.class, metadata);
    }

}

