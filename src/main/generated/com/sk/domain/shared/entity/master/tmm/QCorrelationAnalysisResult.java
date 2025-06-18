package com.sk.domain.shared.entity.master.tmm;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCorrelationAnalysisResult is a Querydsl query type for CorrelationAnalysisResult
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCorrelationAnalysisResult extends EntityPathBase<CorrelationAnalysisResult> {

    private static final long serialVersionUID = 477752056L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCorrelationAnalysisResult correlationAnalysisResult = new QCorrelationAnalysisResult("correlationAnalysisResult");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    public final NumberPath<java.math.BigDecimal> correlationCalculationVal = createNumber("correlationCalculationVal", java.math.BigDecimal.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final com.sk.domain.shared.entity.compositeKey.tmm.QCorrelationAnalysisResultId id;

    public final StringPath instrumentNm = createString("instrumentNm");

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public QCorrelationAnalysisResult(String variable) {
        this(CorrelationAnalysisResult.class, forVariable(variable), INITS);
    }

    public QCorrelationAnalysisResult(Path<? extends CorrelationAnalysisResult> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCorrelationAnalysisResult(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCorrelationAnalysisResult(PathMetadata metadata, PathInits inits) {
        this(CorrelationAnalysisResult.class, metadata, inits);
    }

    public QCorrelationAnalysisResult(Class<? extends CorrelationAnalysisResult> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.id = inits.isInitialized("id") ? new com.sk.domain.shared.entity.compositeKey.tmm.QCorrelationAnalysisResultId(forProperty("id")) : null;
    }

}

