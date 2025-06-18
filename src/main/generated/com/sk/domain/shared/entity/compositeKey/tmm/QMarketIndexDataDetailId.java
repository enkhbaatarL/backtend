package com.sk.domain.shared.entity.compositeKey.tmm;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QMarketIndexDataDetailId is a Querydsl query type for MarketIndexDataDetailId
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QMarketIndexDataDetailId extends BeanPath<MarketIndexDataDetailId> {

    private static final long serialVersionUID = -1855687969L;

    public static final QMarketIndexDataDetailId marketIndexDataDetailId = new QMarketIndexDataDetailId("marketIndexDataDetailId");

    public final StringPath instrumentDataSeparateCd = createString("instrumentDataSeparateCd");

    public final StringPath marketIndexAnalysisTargetCd = createString("marketIndexAnalysisTargetCd");

    public final NumberPath<Integer> marketIndexDataNo = createNumber("marketIndexDataNo", Integer.class);

    public QMarketIndexDataDetailId(String variable) {
        super(MarketIndexDataDetailId.class, forVariable(variable));
    }

    public QMarketIndexDataDetailId(Path<? extends MarketIndexDataDetailId> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMarketIndexDataDetailId(PathMetadata metadata) {
        super(MarketIndexDataDetailId.class, metadata);
    }

}

