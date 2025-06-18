package com.sk.domain.shared.entity.master.tcm;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChartPredictSetting is a Querydsl query type for ChartPredictSetting
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChartPredictSetting extends EntityPathBase<ChartPredictSetting> {

    private static final long serialVersionUID = -1118581508L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QChartPredictSetting chartPredictSetting = new QChartPredictSetting("chartPredictSetting");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    public final NumberPath<Integer> chartPredictSettingNo = createNumber("chartPredictSettingNo", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final com.sk.domain.shared.entity.master.tmd.QCustomerCompany customerCompany;

    public final EnumPath<com.sk.domain.shared.enums.PeriodUnitSeparateCd> defaultPeriodUnitSeparateCd = createEnum("defaultPeriodUnitSeparateCd", com.sk.domain.shared.enums.PeriodUnitSeparateCd.class);

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public final NumberPath<Integer> predictChoiceDay = createNumber("predictChoiceDay", Integer.class);

    public final NumberPath<Integer> predictPeriodMonthsCnt = createNumber("predictPeriodMonthsCnt", Integer.class);

    public final EnumPath<com.sk.domain.shared.enums.PeriodUnitSeparateCd> predictPeriodUnitSeparateCd = createEnum("predictPeriodUnitSeparateCd", com.sk.domain.shared.enums.PeriodUnitSeparateCd.class);

    public final StringPath predictTargetSettingNm = createString("predictTargetSettingNm");

    public final EnumPath<com.sk.domain.shared.enums.PeriodUnitSeparateCd> scopePeriodUnitSeparateCd = createEnum("scopePeriodUnitSeparateCd", com.sk.domain.shared.enums.PeriodUnitSeparateCd.class);

    public QChartPredictSetting(String variable) {
        this(ChartPredictSetting.class, forVariable(variable), INITS);
    }

    public QChartPredictSetting(Path<? extends ChartPredictSetting> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QChartPredictSetting(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QChartPredictSetting(PathMetadata metadata, PathInits inits) {
        this(ChartPredictSetting.class, metadata, inits);
    }

    public QChartPredictSetting(Class<? extends ChartPredictSetting> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.customerCompany = inits.isInitialized("customerCompany") ? new com.sk.domain.shared.entity.master.tmd.QCustomerCompany(forProperty("customerCompany")) : null;
    }

}

