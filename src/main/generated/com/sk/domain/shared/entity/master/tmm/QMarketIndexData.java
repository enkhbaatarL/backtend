package com.sk.domain.shared.entity.master.tmm;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QMarketIndexData is a Querydsl query type for MarketIndexData
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMarketIndexData extends EntityPathBase<MarketIndexData> {

    private static final long serialVersionUID = -247054915L;

    public static final QMarketIndexData marketIndexData = new QMarketIndexData("marketIndexData");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final StringPath currencyCd = createString("currencyCd");

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final StringPath isinCd = createString("isinCd");

    public final NumberPath<Integer> marketIndexDataNo = createNumber("marketIndexDataNo", Integer.class);

    public final StringPath mnemonicCd = createString("mnemonicCd");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public final StringPath nationCd = createString("nationCd");

    public final StringPath ricCd = createString("ricCd");

    public QMarketIndexData(String variable) {
        super(MarketIndexData.class, forVariable(variable));
    }

    public QMarketIndexData(Path<? extends MarketIndexData> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMarketIndexData(PathMetadata metadata) {
        super(MarketIndexData.class, metadata);
    }

}

