package com.sk.domain.shared.entity.master.tlm;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QGoodsDataInquiryUser is a Querydsl query type for GoodsDataInquiryUser
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QGoodsDataInquiryUser extends EntityPathBase<GoodsDataInquiryUser> {

    private static final long serialVersionUID = -1428126764L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QGoodsDataInquiryUser goodsDataInquiryUser = new QGoodsDataInquiryUser("goodsDataInquiryUser");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final com.sk.domain.shared.entity.compositeKey.tlm.QGoodsDataInquiryUserId id;

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public QGoodsDataInquiryUser(String variable) {
        this(GoodsDataInquiryUser.class, forVariable(variable), INITS);
    }

    public QGoodsDataInquiryUser(Path<? extends GoodsDataInquiryUser> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QGoodsDataInquiryUser(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QGoodsDataInquiryUser(PathMetadata metadata, PathInits inits) {
        this(GoodsDataInquiryUser.class, metadata, inits);
    }

    public QGoodsDataInquiryUser(Class<? extends GoodsDataInquiryUser> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.id = inits.isInitialized("id") ? new com.sk.domain.shared.entity.compositeKey.tlm.QGoodsDataInquiryUserId(forProperty("id"), inits.get("id")) : null;
    }

}

