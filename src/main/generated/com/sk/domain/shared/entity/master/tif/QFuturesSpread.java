package com.sk.domain.shared.entity.master.tif;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QFuturesSpread is a Querydsl query type for FuturesSpread
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QFuturesSpread extends EntityPathBase<FuturesSpread> {

    private static final long serialVersionUID = 872387485L;

    public static final QFuturesSpread futuresSpread = new QFuturesSpread("futuresSpread");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    public final StringPath code = createString("code");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final NumberPath<Integer> dataRegistrationSn = createNumber("dataRegistrationSn", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> date = createDateTime("date", java.time.LocalDateTime.class);

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public final NumberPath<Integer> oi = createNumber("oi", Integer.class);

    public final NumberPath<java.math.BigDecimal> ph = createNumber("ph", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> pl = createNumber("pl", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> po = createNumber("po", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> ps = createNumber("ps", java.math.BigDecimal.class);

    public final NumberPath<Integer> vm = createNumber("vm", Integer.class);

    public QFuturesSpread(String variable) {
        super(FuturesSpread.class, forVariable(variable));
    }

    public QFuturesSpread(Path<? extends FuturesSpread> path) {
        super(path.getType(), path.getMetadata());
    }

    public QFuturesSpread(PathMetadata metadata) {
        super(FuturesSpread.class, metadata);
    }

}

