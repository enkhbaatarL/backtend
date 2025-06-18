package com.sk.domain.shared.entity.master.tmd;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QGoodsData is a Querydsl query type for GoodsData
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QGoodsData extends EntityPathBase<GoodsData> {

    private static final long serialVersionUID = -368692204L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QGoodsData goodsData = new QGoodsData("goodsData");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final QCustomerCompany customerCompany;

    public final StringPath dataContributorNm = createString("dataContributorNm");

    public final QGoods goods;

    public final StringPath goodsDataDistinguishId = createString("goodsDataDistinguishId");

    public final StringPath goodsDataExpn = createString("goodsDataExpn");

    public final StringPath goodsDataNm = createString("goodsDataNm");

    public final NumberPath<Integer> goodsDataNo = createNumber("goodsDataNo", Integer.class);

    public final StringPath goodsDataSummaryCntt = createString("goodsDataSummaryCntt");

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final BooleanPath isGoodsDataMarked = createBoolean("isGoodsDataMarked");

    public final BooleanPath isMetaDataUploaded = createBoolean("isMetaDataUploaded");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public QGoodsData(String variable) {
        this(GoodsData.class, forVariable(variable), INITS);
    }

    public QGoodsData(Path<? extends GoodsData> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QGoodsData(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QGoodsData(PathMetadata metadata, PathInits inits) {
        this(GoodsData.class, metadata, inits);
    }

    public QGoodsData(Class<? extends GoodsData> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.customerCompany = inits.isInitialized("customerCompany") ? new QCustomerCompany(forProperty("customerCompany")) : null;
        this.goods = inits.isInitialized("goods") ? new QGoods(forProperty("goods"), inits.get("goods")) : null;
    }

}

