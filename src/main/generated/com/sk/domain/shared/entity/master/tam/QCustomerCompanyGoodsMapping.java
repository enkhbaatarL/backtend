package com.sk.domain.shared.entity.master.tam;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCustomerCompanyGoodsMapping is a Querydsl query type for CustomerCompanyGoodsMapping
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCustomerCompanyGoodsMapping extends EntityPathBase<CustomerCompanyGoodsMapping> {

    private static final long serialVersionUID = 1341396224L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCustomerCompanyGoodsMapping customerCompanyGoodsMapping = new QCustomerCompanyGoodsMapping("customerCompanyGoodsMapping");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final com.sk.domain.shared.entity.master.tmd.QCustomerCompany customerCompany;

    public final com.sk.domain.shared.entity.master.tmd.QGoods goods;

    public final com.sk.domain.shared.entity.compositeKey.tam.QCustomerCompanyGoodsMappingId id;

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final BooleanPath isUsed = createBoolean("isUsed");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public QCustomerCompanyGoodsMapping(String variable) {
        this(CustomerCompanyGoodsMapping.class, forVariable(variable), INITS);
    }

    public QCustomerCompanyGoodsMapping(Path<? extends CustomerCompanyGoodsMapping> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCustomerCompanyGoodsMapping(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCustomerCompanyGoodsMapping(PathMetadata metadata, PathInits inits) {
        this(CustomerCompanyGoodsMapping.class, metadata, inits);
    }

    public QCustomerCompanyGoodsMapping(Class<? extends CustomerCompanyGoodsMapping> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.customerCompany = inits.isInitialized("customerCompany") ? new com.sk.domain.shared.entity.master.tmd.QCustomerCompany(forProperty("customerCompany")) : null;
        this.goods = inits.isInitialized("goods") ? new com.sk.domain.shared.entity.master.tmd.QGoods(forProperty("goods"), inits.get("goods")) : null;
        this.id = inits.isInitialized("id") ? new com.sk.domain.shared.entity.compositeKey.tam.QCustomerCompanyGoodsMappingId(forProperty("id")) : null;
    }

}

