package com.sk.domain.shared.entity.compositeKey.tmd;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QProviderProviderDataColumnMappingId is a Querydsl query type for ProviderProviderDataColumnMappingId
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QProviderProviderDataColumnMappingId extends BeanPath<ProviderProviderDataColumnMappingId> {

    private static final long serialVersionUID = 1418096401L;

    public static final QProviderProviderDataColumnMappingId providerProviderDataColumnMappingId = new QProviderProviderDataColumnMappingId("providerProviderDataColumnMappingId");

    public final StringPath providerDataColumnNm = createString("providerDataColumnNm");

    public final NumberPath<Integer> providerNo = createNumber("providerNo", Integer.class);

    public QProviderProviderDataColumnMappingId(String variable) {
        super(ProviderProviderDataColumnMappingId.class, forVariable(variable));
    }

    public QProviderProviderDataColumnMappingId(Path<? extends ProviderProviderDataColumnMappingId> path) {
        super(path.getType(), path.getMetadata());
    }

    public QProviderProviderDataColumnMappingId(PathMetadata metadata) {
        super(ProviderProviderDataColumnMappingId.class, metadata);
    }

}

