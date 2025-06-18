package com.sk.domain.shared.entity.master.tmm;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBizTargetPredictionExplanatoryInfo is a Querydsl query type for BizTargetPredictionExplanatoryInfo
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBizTargetPredictionExplanatoryInfo extends EntityPathBase<BizTargetPredictionExplanatoryInfo> {

    private static final long serialVersionUID = -1222377595L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBizTargetPredictionExplanatoryInfo bizTargetPredictionExplanatoryInfo = new QBizTargetPredictionExplanatoryInfo("bizTargetPredictionExplanatoryInfo");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final StringPath explanatoryDirectionSymbolNm = createString("explanatoryDirectionSymbolNm");

    public final NumberPath<java.math.BigDecimal> explanatoryNm = createNumber("explanatoryNm", java.math.BigDecimal.class);

    public final NumberPath<Integer> explanatoryRnk = createNumber("explanatoryRnk", Integer.class);

    public final com.sk.domain.shared.entity.compositeKey.tmm.QBizTargetPredictionExplanatoryInfoId id;

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public final StringPath predictionInputMnemonicDetailCd = createString("predictionInputMnemonicDetailCd");

    public QBizTargetPredictionExplanatoryInfo(String variable) {
        this(BizTargetPredictionExplanatoryInfo.class, forVariable(variable), INITS);
    }

    public QBizTargetPredictionExplanatoryInfo(Path<? extends BizTargetPredictionExplanatoryInfo> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBizTargetPredictionExplanatoryInfo(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBizTargetPredictionExplanatoryInfo(PathMetadata metadata, PathInits inits) {
        this(BizTargetPredictionExplanatoryInfo.class, metadata, inits);
    }

    public QBizTargetPredictionExplanatoryInfo(Class<? extends BizTargetPredictionExplanatoryInfo> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.id = inits.isInitialized("id") ? new com.sk.domain.shared.entity.compositeKey.tmm.QBizTargetPredictionExplanatoryInfoId(forProperty("id")) : null;
    }

}

