package com.sk.domain.shared.entity.master.tam;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCustomerCompanyProviderMapping is a Querydsl query type for CustomerCompanyProviderMapping
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCustomerCompanyProviderMapping extends EntityPathBase<CustomerCompanyProviderMapping> {

    private static final long serialVersionUID = -1755002827L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCustomerCompanyProviderMapping customerCompanyProviderMapping = new QCustomerCompanyProviderMapping("customerCompanyProviderMapping");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final com.sk.domain.shared.entity.master.tmd.QCustomerCompany customerCompany;

    public final com.sk.domain.shared.entity.compositeKey.tam.QCustomerCompanyProviderMappingId id;

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final BooleanPath isUsed = createBoolean("isUsed");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public final com.sk.domain.shared.entity.master.tmd.QProvider provider;

    public QCustomerCompanyProviderMapping(String variable) {
        this(CustomerCompanyProviderMapping.class, forVariable(variable), INITS);
    }

    public QCustomerCompanyProviderMapping(Path<? extends CustomerCompanyProviderMapping> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCustomerCompanyProviderMapping(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCustomerCompanyProviderMapping(PathMetadata metadata, PathInits inits) {
        this(CustomerCompanyProviderMapping.class, metadata, inits);
    }

    public QCustomerCompanyProviderMapping(Class<? extends CustomerCompanyProviderMapping> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.customerCompany = inits.isInitialized("customerCompany") ? new com.sk.domain.shared.entity.master.tmd.QCustomerCompany(forProperty("customerCompany")) : null;
        this.id = inits.isInitialized("id") ? new com.sk.domain.shared.entity.compositeKey.tam.QCustomerCompanyProviderMappingId(forProperty("id")) : null;
        this.provider = inits.isInitialized("provider") ? new com.sk.domain.shared.entity.master.tmd.QProvider(forProperty("provider")) : null;
    }

}

