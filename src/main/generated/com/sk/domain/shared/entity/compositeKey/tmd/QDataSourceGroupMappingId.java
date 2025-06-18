package com.sk.domain.shared.entity.compositeKey.tmd;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QDataSourceGroupMappingId is a Querydsl query type for DataSourceGroupMappingId
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QDataSourceGroupMappingId extends BeanPath<DataSourceGroupMappingId> {

    private static final long serialVersionUID = -404581851L;

    public static final QDataSourceGroupMappingId dataSourceGroupMappingId = new QDataSourceGroupMappingId("dataSourceGroupMappingId");

    public final NumberPath<Integer> compareGoodsDataSourceNo = createNumber("compareGoodsDataSourceNo", Integer.class);

    public final NumberPath<Integer> mainGoodsDataSourceNo = createNumber("mainGoodsDataSourceNo", Integer.class);

    public QDataSourceGroupMappingId(String variable) {
        super(DataSourceGroupMappingId.class, forVariable(variable));
    }

    public QDataSourceGroupMappingId(Path<? extends DataSourceGroupMappingId> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDataSourceGroupMappingId(PathMetadata metadata) {
        super(DataSourceGroupMappingId.class, metadata);
    }

}

