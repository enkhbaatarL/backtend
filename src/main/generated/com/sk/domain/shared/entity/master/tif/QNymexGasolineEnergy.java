package com.sk.domain.shared.entity.master.tif;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QNymexGasolineEnergy is a Querydsl query type for NymexGasolineEnergy
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QNymexGasolineEnergy extends EntityPathBase<NymexGasolineEnergy> {

    private static final long serialVersionUID = -1648728095L;

    public static final QNymexGasolineEnergy nymexGasolineEnergy = new QNymexGasolineEnergy("nymexGasolineEnergy");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    public final NumberPath<Long> acvolUns = createNumber("acvolUns", Long.class);

    public final NumberPath<java.math.BigDecimal> ask = createNumber("ask", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> bid = createNumber("bid", java.math.BigDecimal.class);

    public final NumberPath<Integer> blkVolum = createNumber("blkVolum", Integer.class);

    public final StringPath code = createString("code");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final StringPath crtMnth = createString("crtMnth");

    public final NumberPath<Integer> dataRegistrationSn = createNumber("dataRegistrationSn", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> date = createDateTime("date", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> expirDate = createDateTime("expirDate", java.time.LocalDateTime.class);

    public final NumberPath<java.math.BigDecimal> high1 = createNumber("high1", java.math.BigDecimal.class);

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final NumberPath<java.math.BigDecimal> loLimit = createNumber("loLimit", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> low1 = createNumber("low1", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> midPrice = createNumber("midPrice", java.math.BigDecimal.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public final NumberPath<java.math.BigDecimal> numMoves = createNumber("numMoves", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> openPrc = createNumber("openPrc", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> opint1 = createNumber("opint1", java.math.BigDecimal.class);

    public final NumberPath<Integer> ordbkVol = createNumber("ordbkVol", Integer.class);

    public final NumberPath<Byte> sequence = createNumber("sequence", Byte.class);

    public final NumberPath<java.math.BigDecimal> setlNchng = createNumber("setlNchng", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> setlPchng = createNumber("setlPchng", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> settle = createNumber("settle", java.math.BigDecimal.class);

    public final NumberPath<Integer> totCntrOi = createNumber("totCntrOi", Integer.class);

    public final NumberPath<Integer> totCntrVol = createNumber("totCntrVol", Integer.class);

    public final NumberPath<java.math.BigDecimal> trdPrc1 = createNumber("trdPrc1", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> upLimit = createNumber("upLimit", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> volXPrc1 = createNumber("volXPrc1", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> vwap = createNumber("vwap", java.math.BigDecimal.class);

    public QNymexGasolineEnergy(String variable) {
        super(NymexGasolineEnergy.class, forVariable(variable));
    }

    public QNymexGasolineEnergy(Path<? extends NymexGasolineEnergy> path) {
        super(path.getType(), path.getMetadata());
    }

    public QNymexGasolineEnergy(PathMetadata metadata) {
        super(NymexGasolineEnergy.class, metadata);
    }

}

