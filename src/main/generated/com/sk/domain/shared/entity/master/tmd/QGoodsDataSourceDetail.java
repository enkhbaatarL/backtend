package com.sk.domain.shared.entity.master.tmd;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QGoodsDataSourceDetail is a Querydsl query type for GoodsDataSourceDetail
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QGoodsDataSourceDetail extends EntityPathBase<GoodsDataSourceDetail> {

    private static final long serialVersionUID = -1894140288L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QGoodsDataSourceDetail goodsDataSourceDetail = new QGoodsDataSourceDetail("goodsDataSourceDetail");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    public final com.sk.domain.shared.entity.master.tcm.QComponentInfo componentInfo;

    public final StringPath convertTargetDetailTypeNm = createString("convertTargetDetailTypeNm");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final QGoodsData goodsData;

    public final StringPath goodsDataSourceCurrencyUnitNm = createString("goodsDataSourceCurrencyUnitNm");

    public final StringPath goodsDataSourceExpn = createString("goodsDataSourceExpn");

    public final StringPath goodsDataSourceNm = createString("goodsDataSourceNm");

    public final NumberPath<Integer> goodsDataSourceNo = createNumber("goodsDataSourceNo", Integer.class);

    public final StringPath goodsDataSourceRmk = createString("goodsDataSourceRmk");

    public final StringPath goodsDataSourceUnitNm = createString("goodsDataSourceUnitNm");

    public final StringPath goodsDataUseManagementCd = createString("goodsDataUseManagementCd");

    public final BooleanPath isCandleChartPossible = createBoolean("isCandleChartPossible");

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public final EnumPath<com.sk.domain.shared.enums.PeriodUnitSeparateCd> periodUnitSeparateCd = createEnum("periodUnitSeparateCd", com.sk.domain.shared.enums.PeriodUnitSeparateCd.class);

    public final StringPath queryConditionCntt = createString("queryConditionCntt");

    public final StringPath tableColumnListNm = createString("tableColumnListNm");

    public final StringPath targetDbNm = createString("targetDbNm");

    public final StringPath targetTableNm = createString("targetTableNm");

    public final StringPath tradeVolumeColumnNm = createString("tradeVolumeColumnNm");

    public QGoodsDataSourceDetail(String variable) {
        this(GoodsDataSourceDetail.class, forVariable(variable), INITS);
    }

    public QGoodsDataSourceDetail(Path<? extends GoodsDataSourceDetail> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QGoodsDataSourceDetail(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QGoodsDataSourceDetail(PathMetadata metadata, PathInits inits) {
        this(GoodsDataSourceDetail.class, metadata, inits);
    }

    public QGoodsDataSourceDetail(Class<? extends GoodsDataSourceDetail> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.componentInfo = inits.isInitialized("componentInfo") ? new com.sk.domain.shared.entity.master.tcm.QComponentInfo(forProperty("componentInfo")) : null;
        this.goodsData = inits.isInitialized("goodsData") ? new QGoodsData(forProperty("goodsData"), inits.get("goodsData")) : null;
    }

}

