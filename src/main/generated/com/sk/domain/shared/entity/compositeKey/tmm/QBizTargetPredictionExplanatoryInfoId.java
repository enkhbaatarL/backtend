package com.sk.domain.shared.entity.compositeKey.tmm;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QBizTargetPredictionExplanatoryInfoId is a Querydsl query type for BizTargetPredictionExplanatoryInfoId
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QBizTargetPredictionExplanatoryInfoId extends BeanPath<BizTargetPredictionExplanatoryInfoId> {

    private static final long serialVersionUID = 1831700106L;

    public static final QBizTargetPredictionExplanatoryInfoId bizTargetPredictionExplanatoryInfoId = new QBizTargetPredictionExplanatoryInfoId("bizTargetPredictionExplanatoryInfoId");

    public final StringPath explanatoryLevelDetailNm = createString("explanatoryLevelDetailNm");

    public final StringPath explanatoryLevelNm = createString("explanatoryLevelNm");

    public final StringPath marketIndexAnalysisTargetCd = createString("marketIndexAnalysisTargetCd");

    public final StringPath mnemonicCd = createString("mnemonicCd");

    public final StringPath predictionFulfillDt = createString("predictionFulfillDt");

    public final StringPath predictionInputMnemonicCd = createString("predictionInputMnemonicCd");

    public QBizTargetPredictionExplanatoryInfoId(String variable) {
        super(BizTargetPredictionExplanatoryInfoId.class, forVariable(variable));
    }

    public QBizTargetPredictionExplanatoryInfoId(Path<? extends BizTargetPredictionExplanatoryInfoId> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBizTargetPredictionExplanatoryInfoId(PathMetadata metadata) {
        super(BizTargetPredictionExplanatoryInfoId.class, metadata);
    }

}

