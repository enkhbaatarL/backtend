package com.sk.domain.shared.entity.master.tmd;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QGoodsDataCategory is a Querydsl query type for GoodsDataCategory
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QGoodsDataCategory extends EntityPathBase<GoodsDataCategory> {

    private static final long serialVersionUID = 113770802L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QGoodsDataCategory goodsDataCategory = new QGoodsDataCategory("goodsDataCategory");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final NumberPath<Long> goodsCategoryRegistrationNo = createNumber("goodsCategoryRegistrationNo", Long.class);

    public final QGoodsData goodsData;

    public final StringPath goodsDataCategoryCd = createString("goodsDataCategoryCd");

    public final StringPath goodsDataKeywordNm = createString("goodsDataKeywordNm");

    public final NumberPath<Long> goodsDataManagementNo = createNumber("goodsDataManagementNo", Long.class);

    public final StringPath goodsDataManagementSeparateCd = createString("goodsDataManagementSeparateCd");

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public QGoodsDataCategory(String variable) {
        this(GoodsDataCategory.class, forVariable(variable), INITS);
    }

    public QGoodsDataCategory(Path<? extends GoodsDataCategory> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QGoodsDataCategory(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QGoodsDataCategory(PathMetadata metadata, PathInits inits) {
        this(GoodsDataCategory.class, metadata, inits);
    }

    public QGoodsDataCategory(Class<? extends GoodsDataCategory> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.goodsData = inits.isInitialized("goodsData") ? new QGoodsData(forProperty("goodsData"), inits.get("goodsData")) : null;
    }

}

