package com.sk.domain.shared.entity.master.tmd;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QGoodsCriticalValueSetting is a Querydsl query type for GoodsCriticalValueSetting
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QGoodsCriticalValueSetting extends EntityPathBase<GoodsCriticalValueSetting> {

    private static final long serialVersionUID = 1483557192L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QGoodsCriticalValueSetting goodsCriticalValueSetting = new QGoodsCriticalValueSetting("goodsCriticalValueSetting");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final StringPath criticalValueSettingUnitNm = createString("criticalValueSettingUnitNm");

    public final NumberPath<java.math.BigDecimal> criticalValueSettingVol = createNumber("criticalValueSettingVol", java.math.BigDecimal.class);

    public final QGoodsDataSourceDetail goodsDataSourceDetail;

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final BooleanPath isPredictData = createBoolean("isPredictData");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public final EnumPath<com.sk.domain.shared.enums.NotiCrtvlSptCd> notificationCriticalValueSeparateCd = createEnum("notificationCriticalValueSeparateCd", com.sk.domain.shared.enums.NotiCrtvlSptCd.class);

    public final QUser user;

    public final NumberPath<Integer> userGoodsNotificationSettingNo = createNumber("userGoodsNotificationSettingNo", Integer.class);

    public QGoodsCriticalValueSetting(String variable) {
        this(GoodsCriticalValueSetting.class, forVariable(variable), INITS);
    }

    public QGoodsCriticalValueSetting(Path<? extends GoodsCriticalValueSetting> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QGoodsCriticalValueSetting(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QGoodsCriticalValueSetting(PathMetadata metadata, PathInits inits) {
        this(GoodsCriticalValueSetting.class, metadata, inits);
    }

    public QGoodsCriticalValueSetting(Class<? extends GoodsCriticalValueSetting> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.goodsDataSourceDetail = inits.isInitialized("goodsDataSourceDetail") ? new QGoodsDataSourceDetail(forProperty("goodsDataSourceDetail"), inits.get("goodsDataSourceDetail")) : null;
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user"), inits.get("user")) : null;
    }

}

