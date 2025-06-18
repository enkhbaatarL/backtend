package com.sk.domain.shared.entity.master.tmm;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMarketIndexDataDetail is a Querydsl query type for MarketIndexDataDetail
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMarketIndexDataDetail extends EntityPathBase<MarketIndexDataDetail> {

    private static final long serialVersionUID = -928169874L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMarketIndexDataDetail marketIndexDataDetail = new QMarketIndexDataDetail("marketIndexDataDetail");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final StringPath dataCollectionBeginDt = createString("dataCollectionBeginDt");

    public final com.sk.domain.shared.entity.compositeKey.tmm.QMarketIndexDataDetailId id;

    public final StringPath instrumentCategoryNm = createString("instrumentCategoryNm");

    public final StringPath instrumentCollectionTableNm = createString("instrumentCollectionTableNm");

    public final StringPath instrumentNm = createString("instrumentNm");

    public final StringPath instrumentScreenCategoryNm = createString("instrumentScreenCategoryNm");

    public final StringPath instrumentScreenDetailCategoryNm = createString("instrumentScreenDetailCategoryNm");

    public final BooleanPath isbzTarget = createBoolean("isbzTarget");

    public final BooleanPath isDataModelUsed = createBoolean("isDataModelUsed");

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final QMarketIndexData marketIndexData;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public QMarketIndexDataDetail(String variable) {
        this(MarketIndexDataDetail.class, forVariable(variable), INITS);
    }

    public QMarketIndexDataDetail(Path<? extends MarketIndexDataDetail> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMarketIndexDataDetail(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMarketIndexDataDetail(PathMetadata metadata, PathInits inits) {
        this(MarketIndexDataDetail.class, metadata, inits);
    }

    public QMarketIndexDataDetail(Class<? extends MarketIndexDataDetail> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.id = inits.isInitialized("id") ? new com.sk.domain.shared.entity.compositeKey.tmm.QMarketIndexDataDetailId(forProperty("id")) : null;
        this.marketIndexData = inits.isInitialized("marketIndexData") ? new QMarketIndexData(forProperty("marketIndexData")) : null;
    }

}

