package com.sk.domain.shared.entity.master.tmd;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProviderDataColumn is a Querydsl query type for ProviderDataColumn
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProviderDataColumn extends EntityPathBase<ProviderDataColumn> {

    private static final long serialVersionUID = 875958013L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QProviderDataColumn providerDataColumn = new QProviderDataColumn("providerDataColumn");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final StringPath dataColumnExpn = createString("dataColumnExpn");

    public final com.sk.domain.shared.entity.compositeKey.tmd.QProviderProviderDataColumnMappingId id;

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public final QProvider provider;

    public QProviderDataColumn(String variable) {
        this(ProviderDataColumn.class, forVariable(variable), INITS);
    }

    public QProviderDataColumn(Path<? extends ProviderDataColumn> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QProviderDataColumn(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QProviderDataColumn(PathMetadata metadata, PathInits inits) {
        this(ProviderDataColumn.class, metadata, inits);
    }

    public QProviderDataColumn(Class<? extends ProviderDataColumn> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.id = inits.isInitialized("id") ? new com.sk.domain.shared.entity.compositeKey.tmd.QProviderProviderDataColumnMappingId(forProperty("id")) : null;
        this.provider = inits.isInitialized("provider") ? new QProvider(forProperty("provider")) : null;
    }

}

