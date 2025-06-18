package com.sk.domain.shared.entity.master.tcm;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPersonalizedChartSetting is a Querydsl query type for PersonalizedChartSetting
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPersonalizedChartSetting extends EntityPathBase<PersonalizedChartSetting> {

    private static final long serialVersionUID = 655365787L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPersonalizedChartSetting personalizedChartSetting = new QPersonalizedChartSetting("personalizedChartSetting");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    public final EnumPath<com.sk.domain.shared.enums.chart.ChartSettingConditionCd> chartSettingConditionCd = createEnum("chartSettingConditionCd", com.sk.domain.shared.enums.chart.ChartSettingConditionCd.class);

    public final StringPath chartSettingConditionVal = createString("chartSettingConditionVal");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final com.sk.domain.shared.entity.master.tmd.QGoodsDataSourceDetail goodsDataSourceDetail;

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public final NumberPath<Integer> personalizedChartSettingNo = createNumber("personalizedChartSettingNo", Integer.class);

    public final StringPath userLoginId = createString("userLoginId");

    public QPersonalizedChartSetting(String variable) {
        this(PersonalizedChartSetting.class, forVariable(variable), INITS);
    }

    public QPersonalizedChartSetting(Path<? extends PersonalizedChartSetting> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPersonalizedChartSetting(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPersonalizedChartSetting(PathMetadata metadata, PathInits inits) {
        this(PersonalizedChartSetting.class, metadata, inits);
    }

    public QPersonalizedChartSetting(Class<? extends PersonalizedChartSetting> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.goodsDataSourceDetail = inits.isInitialized("goodsDataSourceDetail") ? new com.sk.domain.shared.entity.master.tmd.QGoodsDataSourceDetail(forProperty("goodsDataSourceDetail"), inits.get("goodsDataSourceDetail")) : null;
    }

}

