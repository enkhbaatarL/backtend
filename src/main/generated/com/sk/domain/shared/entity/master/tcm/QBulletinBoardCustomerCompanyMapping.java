package com.sk.domain.shared.entity.master.tcm;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBulletinBoardCustomerCompanyMapping is a Querydsl query type for BulletinBoardCustomerCompanyMapping
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBulletinBoardCustomerCompanyMapping extends EntityPathBase<BulletinBoardCustomerCompanyMapping> {

    private static final long serialVersionUID = 1383403957L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBulletinBoardCustomerCompanyMapping bulletinBoardCustomerCompanyMapping = new QBulletinBoardCustomerCompanyMapping("bulletinBoardCustomerCompanyMapping");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    public final QBulletinBoard bulletinBoard;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final com.sk.domain.shared.entity.master.tmd.QCustomerCompany customerCompany;

    public final com.sk.domain.shared.entity.compositeKey.tcm.QBulletinBoardCustomerCompanyMappingId id;

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public QBulletinBoardCustomerCompanyMapping(String variable) {
        this(BulletinBoardCustomerCompanyMapping.class, forVariable(variable), INITS);
    }

    public QBulletinBoardCustomerCompanyMapping(Path<? extends BulletinBoardCustomerCompanyMapping> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBulletinBoardCustomerCompanyMapping(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBulletinBoardCustomerCompanyMapping(PathMetadata metadata, PathInits inits) {
        this(BulletinBoardCustomerCompanyMapping.class, metadata, inits);
    }

    public QBulletinBoardCustomerCompanyMapping(Class<? extends BulletinBoardCustomerCompanyMapping> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.bulletinBoard = inits.isInitialized("bulletinBoard") ? new QBulletinBoard(forProperty("bulletinBoard")) : null;
        this.customerCompany = inits.isInitialized("customerCompany") ? new com.sk.domain.shared.entity.master.tmd.QCustomerCompany(forProperty("customerCompany")) : null;
        this.id = inits.isInitialized("id") ? new com.sk.domain.shared.entity.compositeKey.tcm.QBulletinBoardCustomerCompanyMappingId(forProperty("id")) : null;
    }

}

