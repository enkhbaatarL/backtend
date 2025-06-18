package com.sk.domain.shared.entity.master.tcm;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCommonCodeDetail is a Querydsl query type for CommonCodeDetail
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCommonCodeDetail extends EntityPathBase<CommonCodeDetail> {

    private static final long serialVersionUID = 269835266L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCommonCodeDetail commonCodeDetail = new QCommonCodeDetail("commonCodeDetail");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    public final StringPath codeExpn = createString("codeExpn");

    public final StringPath codeNm = createString("codeNm");

    public final NumberPath<Short> codeRnk = createNumber("codeRnk", Short.class);

    public final QCommonCode commonCd;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final StringPath highCodeGroupId = createString("highCodeGroupId");

    public final StringPath highCodeId = createString("highCodeId");

    public final com.sk.domain.shared.entity.compositeKey.tcm.QCommonCodeDetailId id;

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final BooleanPath isUsed = createBoolean("isUsed");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public final StringPath useBeginDt = createString("useBeginDt");

    public final StringPath useEndDt = createString("useEndDt");

    public QCommonCodeDetail(String variable) {
        this(CommonCodeDetail.class, forVariable(variable), INITS);
    }

    public QCommonCodeDetail(Path<? extends CommonCodeDetail> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCommonCodeDetail(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCommonCodeDetail(PathMetadata metadata, PathInits inits) {
        this(CommonCodeDetail.class, metadata, inits);
    }

    public QCommonCodeDetail(Class<? extends CommonCodeDetail> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.commonCd = inits.isInitialized("commonCd") ? new QCommonCode(forProperty("commonCd")) : null;
        this.id = inits.isInitialized("id") ? new com.sk.domain.shared.entity.compositeKey.tcm.QCommonCodeDetailId(forProperty("id")) : null;
    }

}

