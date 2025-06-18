package com.sk.domain.shared.entity.master.tmd;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QGoodsDataUnitConvert is a Querydsl query type for GoodsDataUnitConvert
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QGoodsDataUnitConvert extends EntityPathBase<GoodsDataUnitConvert> {

    private static final long serialVersionUID = 1262141147L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QGoodsDataUnitConvert goodsDataUnitConvert = new QGoodsDataUnitConvert("goodsDataUnitConvert");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final QGoodsDataSourceDetail goodsDataSourceDetail;

    public final com.sk.domain.shared.entity.compositeKey.tmd.QGoodsDataSourceDetailUnitConvertMappingId id;

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public final QUnitConvert unitConvert;

    public QGoodsDataUnitConvert(String variable) {
        this(GoodsDataUnitConvert.class, forVariable(variable), INITS);
    }

    public QGoodsDataUnitConvert(Path<? extends GoodsDataUnitConvert> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QGoodsDataUnitConvert(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QGoodsDataUnitConvert(PathMetadata metadata, PathInits inits) {
        this(GoodsDataUnitConvert.class, metadata, inits);
    }

    public QGoodsDataUnitConvert(Class<? extends GoodsDataUnitConvert> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.goodsDataSourceDetail = inits.isInitialized("goodsDataSourceDetail") ? new QGoodsDataSourceDetail(forProperty("goodsDataSourceDetail"), inits.get("goodsDataSourceDetail")) : null;
        this.id = inits.isInitialized("id") ? new com.sk.domain.shared.entity.compositeKey.tmd.QGoodsDataSourceDetailUnitConvertMappingId(forProperty("id")) : null;
        this.unitConvert = inits.isInitialized("unitConvert") ? new QUnitConvert(forProperty("unitConvert")) : null;
    }

}

