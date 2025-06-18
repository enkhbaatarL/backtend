package com.sk.domain.shared.entity.compositeKey.tmd;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUserHistoryId is a Querydsl query type for UserHistoryId
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QUserHistoryId extends BeanPath<UserHistoryId> {

    private static final long serialVersionUID = 1029305198L;

    public static final QUserHistoryId userHistoryId = new QUserHistoryId("userHistoryId");

    public final DateTimePath<java.time.LocalDateTime> applyDt = createDateTime("applyDt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> userNo = createNumber("userNo", Integer.class);

    public QUserHistoryId(String variable) {
        super(UserHistoryId.class, forVariable(variable));
    }

    public QUserHistoryId(Path<? extends UserHistoryId> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserHistoryId(PathMetadata metadata) {
        super(UserHistoryId.class, metadata);
    }

}

