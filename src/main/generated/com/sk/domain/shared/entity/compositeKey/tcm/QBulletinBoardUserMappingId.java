package com.sk.domain.shared.entity.compositeKey.tcm;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QBulletinBoardUserMappingId is a Querydsl query type for BulletinBoardUserMappingId
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QBulletinBoardUserMappingId extends BeanPath<BulletinBoardUserMappingId> {

    private static final long serialVersionUID = 696657698L;

    public static final QBulletinBoardUserMappingId bulletinBoardUserMappingId = new QBulletinBoardUserMappingId("bulletinBoardUserMappingId");

    public final NumberPath<Integer> bbmNo = createNumber("bbmNo", Integer.class);

    public final NumberPath<Integer> userNo = createNumber("userNo", Integer.class);

    public QBulletinBoardUserMappingId(String variable) {
        super(BulletinBoardUserMappingId.class, forVariable(variable));
    }

    public QBulletinBoardUserMappingId(Path<? extends BulletinBoardUserMappingId> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBulletinBoardUserMappingId(PathMetadata metadata) {
        super(BulletinBoardUserMappingId.class, metadata);
    }

}

