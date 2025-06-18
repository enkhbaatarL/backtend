package com.sk.domain.shared.entity.compositeKey.tlm;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QGoodsDataInquiryUserId is a Querydsl query type for GoodsDataInquiryUserId
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QGoodsDataInquiryUserId extends BeanPath<GoodsDataInquiryUserId> {

    private static final long serialVersionUID = -1054217063L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QGoodsDataInquiryUserId goodsDataInquiryUserId = new QGoodsDataInquiryUserId("goodsDataInquiryUserId");

    public final com.sk.domain.shared.entity.master.tmd.QGoodsData goodsData;

    public final DateTimePath<java.time.LocalDateTime> inquiryDtm = createDateTime("inquiryDtm", java.time.LocalDateTime.class);

    public final StringPath userLoginId = createString("userLoginId");

    public QGoodsDataInquiryUserId(String variable) {
        this(GoodsDataInquiryUserId.class, forVariable(variable), INITS);
    }

    public QGoodsDataInquiryUserId(Path<? extends GoodsDataInquiryUserId> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QGoodsDataInquiryUserId(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QGoodsDataInquiryUserId(PathMetadata metadata, PathInits inits) {
        this(GoodsDataInquiryUserId.class, metadata, inits);
    }

    public QGoodsDataInquiryUserId(Class<? extends GoodsDataInquiryUserId> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.goodsData = inits.isInitialized("goodsData") ? new com.sk.domain.shared.entity.master.tmd.QGoodsData(forProperty("goodsData"), inits.get("goodsData")) : null;
    }

}

