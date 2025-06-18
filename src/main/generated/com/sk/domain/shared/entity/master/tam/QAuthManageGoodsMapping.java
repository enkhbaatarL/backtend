package com.sk.domain.shared.entity.master.tam;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAuthManageGoodsMapping is a Querydsl query type for AuthManageGoodsMapping
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAuthManageGoodsMapping extends EntityPathBase<AuthManageGoodsMapping> {

    private static final long serialVersionUID = 939159740L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAuthManageGoodsMapping authManageGoodsMapping = new QAuthManageGoodsMapping("authManageGoodsMapping");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    public final NumberPath<Long> authManagerGoodsMappingSeq = createNumber("authManagerGoodsMappingSeq", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final QCustomerCompanyGoodsMapping customerCompanyGoodsMapping;

    public final EnumPath<com.sk.domain.shared.enums.GoodsAuthCd> goodsAuthCd = createEnum("goodsAuthCd", com.sk.domain.shared.enums.GoodsAuthCd.class);

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public final StringPath useBeginDate = createString("useBeginDate");

    public final StringPath useEndDate = createString("useEndDate");

    public final com.sk.domain.shared.entity.master.tmd.QUser user;

    public QAuthManageGoodsMapping(String variable) {
        this(AuthManageGoodsMapping.class, forVariable(variable), INITS);
    }

    public QAuthManageGoodsMapping(Path<? extends AuthManageGoodsMapping> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAuthManageGoodsMapping(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAuthManageGoodsMapping(PathMetadata metadata, PathInits inits) {
        this(AuthManageGoodsMapping.class, metadata, inits);
    }

    public QAuthManageGoodsMapping(Class<? extends AuthManageGoodsMapping> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.customerCompanyGoodsMapping = inits.isInitialized("customerCompanyGoodsMapping") ? new QCustomerCompanyGoodsMapping(forProperty("customerCompanyGoodsMapping"), inits.get("customerCompanyGoodsMapping")) : null;
        this.user = inits.isInitialized("user") ? new com.sk.domain.shared.entity.master.tmd.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

