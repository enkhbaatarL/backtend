package com.sk.domain.shared.entity.master.tcm;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChartPredictSettingDetail is a Querydsl query type for ChartPredictSettingDetail
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChartPredictSettingDetail extends EntityPathBase<ChartPredictSettingDetail> {

    private static final long serialVersionUID = 918299757L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QChartPredictSettingDetail chartPredictSettingDetail = new QChartPredictSettingDetail("chartPredictSettingDetail");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    public final com.sk.domain.shared.entity.master.tmd.QGoodsDataSourceDetail associatedGoodsDataSourceDetail;

    public final EnumPath<com.sk.domain.shared.enums.chart.ChartPredictLineType> chartPredictLineSeparateCd = createEnum("chartPredictLineSeparateCd", com.sk.domain.shared.enums.chart.ChartPredictLineType.class);

    public final QChartPredictSetting chartPredictSetting;

    public final NumberPath<Integer> chartPredictSettingDetailNo = createNumber("chartPredictSettingDetailNo", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final com.sk.domain.shared.entity.master.tmd.QGoodsDataSourceDetail goodsDataSourceDetail;

    public final BooleanPath isChartLinePredictVal = createBoolean("isChartLinePredictVal");

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final BooleanPath isFuture = createBoolean("isFuture");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public final StringPath predictChartDetailExpn = createString("predictChartDetailExpn");

    public QChartPredictSettingDetail(String variable) {
        this(ChartPredictSettingDetail.class, forVariable(variable), INITS);
    }

    public QChartPredictSettingDetail(Path<? extends ChartPredictSettingDetail> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QChartPredictSettingDetail(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QChartPredictSettingDetail(PathMetadata metadata, PathInits inits) {
        this(ChartPredictSettingDetail.class, metadata, inits);
    }

    public QChartPredictSettingDetail(Class<? extends ChartPredictSettingDetail> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.associatedGoodsDataSourceDetail = inits.isInitialized("associatedGoodsDataSourceDetail") ? new com.sk.domain.shared.entity.master.tmd.QGoodsDataSourceDetail(forProperty("associatedGoodsDataSourceDetail"), inits.get("associatedGoodsDataSourceDetail")) : null;
        this.chartPredictSetting = inits.isInitialized("chartPredictSetting") ? new QChartPredictSetting(forProperty("chartPredictSetting"), inits.get("chartPredictSetting")) : null;
        this.goodsDataSourceDetail = inits.isInitialized("goodsDataSourceDetail") ? new com.sk.domain.shared.entity.master.tmd.QGoodsDataSourceDetail(forProperty("goodsDataSourceDetail"), inits.get("goodsDataSourceDetail")) : null;
    }

}

