package com.sk.domain.shared.entity.master.tcm;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMarketDataChartSetting is a Querydsl query type for MarketDataChartSetting
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMarketDataChartSetting extends EntityPathBase<MarketDataChartSetting> {

    private static final long serialVersionUID = -1912600047L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMarketDataChartSetting marketDataChartSetting = new QMarketDataChartSetting("marketDataChartSetting");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    public final NumberPath<Short> chartMarkRank = createNumber("chartMarkRank", Short.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final com.sk.domain.shared.entity.master.tmd.QCustomerCompany customerCompany;

    public final com.sk.domain.shared.entity.master.tmd.QGoodsDataSourceDetail goodsDataSourceDetail;

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final NumberPath<Integer> marketDataSettingNo = createNumber("marketDataSettingNo", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public QMarketDataChartSetting(String variable) {
        this(MarketDataChartSetting.class, forVariable(variable), INITS);
    }

    public QMarketDataChartSetting(Path<? extends MarketDataChartSetting> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMarketDataChartSetting(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMarketDataChartSetting(PathMetadata metadata, PathInits inits) {
        this(MarketDataChartSetting.class, metadata, inits);
    }

    public QMarketDataChartSetting(Class<? extends MarketDataChartSetting> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.customerCompany = inits.isInitialized("customerCompany") ? new com.sk.domain.shared.entity.master.tmd.QCustomerCompany(forProperty("customerCompany")) : null;
        this.goodsDataSourceDetail = inits.isInitialized("goodsDataSourceDetail") ? new com.sk.domain.shared.entity.master.tmd.QGoodsDataSourceDetail(forProperty("goodsDataSourceDetail"), inits.get("goodsDataSourceDetail")) : null;
    }

}

