package com.sk.domain.shared.entity.master.tmd;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QDataSourceGroupMapping is a Querydsl query type for DataSourceGroupMapping
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDataSourceGroupMapping extends EntityPathBase<DataSourceGroupMapping> {

    private static final long serialVersionUID = -2054637856L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QDataSourceGroupMapping dataSourceGroupMapping = new QDataSourceGroupMapping("dataSourceGroupMapping");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    public final QGoodsDataSourceDetail compareGoodsDataSourceDetail;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final com.sk.domain.shared.entity.compositeKey.tmd.QDataSourceGroupMappingId id;

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final QGoodsDataSourceDetail mainGoodsDataSourceDetail;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public QDataSourceGroupMapping(String variable) {
        this(DataSourceGroupMapping.class, forVariable(variable), INITS);
    }

    public QDataSourceGroupMapping(Path<? extends DataSourceGroupMapping> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QDataSourceGroupMapping(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QDataSourceGroupMapping(PathMetadata metadata, PathInits inits) {
        this(DataSourceGroupMapping.class, metadata, inits);
    }

    public QDataSourceGroupMapping(Class<? extends DataSourceGroupMapping> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.compareGoodsDataSourceDetail = inits.isInitialized("compareGoodsDataSourceDetail") ? new QGoodsDataSourceDetail(forProperty("compareGoodsDataSourceDetail"), inits.get("compareGoodsDataSourceDetail")) : null;
        this.id = inits.isInitialized("id") ? new com.sk.domain.shared.entity.compositeKey.tmd.QDataSourceGroupMappingId(forProperty("id")) : null;
        this.mainGoodsDataSourceDetail = inits.isInitialized("mainGoodsDataSourceDetail") ? new QGoodsDataSourceDetail(forProperty("mainGoodsDataSourceDetail"), inits.get("mainGoodsDataSourceDetail")) : null;
    }

}

