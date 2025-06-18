package com.sk.domain.shared.entity.master.tif;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QClientAccessInfo is a Querydsl query type for ClientAccessInfo
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QClientAccessInfo extends EntityPathBase<ClientAccessInfo> {

    private static final long serialVersionUID = -69478109L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QClientAccessInfo clientAccessInfo = new QClientAccessInfo("clientAccessInfo");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    public final StringPath certMethodNm = createString("certMethodNm");

    public final StringPath certTypeNm = createString("certTypeNm");

    public final NumberPath<Integer> clientAccessRegNo = createNumber("clientAccessRegNo", Integer.class);

    public final StringPath clientCertId = createString("clientCertId");

    public final StringPath clientSecretKey = createString("clientSecretKey");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final com.sk.domain.shared.entity.master.tmd.QCustomerCompany customerCompany;

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public final StringPath permissionAuthScopeNm = createString("permissionAuthScopeNm");

    public QClientAccessInfo(String variable) {
        this(ClientAccessInfo.class, forVariable(variable), INITS);
    }

    public QClientAccessInfo(Path<? extends ClientAccessInfo> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QClientAccessInfo(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QClientAccessInfo(PathMetadata metadata, PathInits inits) {
        this(ClientAccessInfo.class, metadata, inits);
    }

    public QClientAccessInfo(Class<? extends ClientAccessInfo> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.customerCompany = inits.isInitialized("customerCompany") ? new com.sk.domain.shared.entity.master.tmd.QCustomerCompany(forProperty("customerCompany")) : null;
    }

}

