package com.sk.domain.shared.entity.master.tmd;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProvider is a Querydsl query type for Provider
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProvider extends EntityPathBase<Provider> {

    private static final long serialVersionUID = -2014190723L;

    public static final QProvider provider = new QProvider("provider");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final BooleanPath isUsed = createBoolean("isUsed");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public final ListPath<ProviderDataColumn, QProviderDataColumn> providerDataColumns = this.<ProviderDataColumn, QProviderDataColumn>createList("providerDataColumns", ProviderDataColumn.class, QProviderDataColumn.class, PathInits.DIRECT2);

    public final StringPath providerExpn = createString("providerExpn");

    public final StringPath providerId = createString("providerId");

    public final StringPath providerNm = createString("providerNm");

    public final NumberPath<Integer> providerNo = createNumber("providerNo", Integer.class);

    public QProvider(String variable) {
        super(Provider.class, forVariable(variable));
    }

    public QProvider(Path<? extends Provider> path) {
        super(path.getType(), path.getMetadata());
    }

    public QProvider(PathMetadata metadata) {
        super(Provider.class, metadata);
    }

}

