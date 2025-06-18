package com.sk.domain.shared.entity.compositeKey.tmm;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QCorrelationAnalysisResultId is a Querydsl query type for CorrelationAnalysisResultId
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QCorrelationAnalysisResultId extends BeanPath<CorrelationAnalysisResultId> {

    private static final long serialVersionUID = -653305879L;

    public static final QCorrelationAnalysisResultId correlationAnalysisResultId = new QCorrelationAnalysisResultId("correlationAnalysisResultId");

    public final StringPath analysisFulfilmentDt = createString("analysisFulfilmentDt");

    public final StringPath correlationCalculationCycleCd = createString("correlationCalculationCycleCd");

    public final StringPath marketIndexAnalysisTargetCd = createString("marketIndexAnalysisTargetCd");

    public final StringPath mnemonicCd = createString("mnemonicCd");

    public final StringPath predictionResultDt = createString("predictionResultDt");

    public QCorrelationAnalysisResultId(String variable) {
        super(CorrelationAnalysisResultId.class, forVariable(variable));
    }

    public QCorrelationAnalysisResultId(Path<? extends CorrelationAnalysisResultId> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCorrelationAnalysisResultId(PathMetadata metadata) {
        super(CorrelationAnalysisResultId.class, metadata);
    }

}

