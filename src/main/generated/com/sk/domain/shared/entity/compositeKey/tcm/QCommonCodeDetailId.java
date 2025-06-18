package com.sk.domain.shared.entity.compositeKey.tcm;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QCommonCodeDetailId is a Querydsl query type for CommonCodeDetailId
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QCommonCodeDetailId extends BeanPath<CommonCodeDetailId> {

    private static final long serialVersionUID = -1912256953L;

    public static final QCommonCodeDetailId commonCodeDetailId = new QCommonCodeDetailId("commonCodeDetailId");

    public final StringPath codeGroupId = createString("codeGroupId");

    public final StringPath codeId = createString("codeId");

    public QCommonCodeDetailId(String variable) {
        super(CommonCodeDetailId.class, forVariable(variable));
    }

    public QCommonCodeDetailId(Path<? extends CommonCodeDetailId> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCommonCodeDetailId(PathMetadata metadata) {
        super(CommonCodeDetailId.class, metadata);
    }

}

