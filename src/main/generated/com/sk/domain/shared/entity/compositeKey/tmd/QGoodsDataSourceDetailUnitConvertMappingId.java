package com.sk.domain.shared.entity.compositeKey.tmd;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QGoodsDataSourceDetailUnitConvertMappingId is a Querydsl query type for GoodsDataSourceDetailUnitConvertMappingId
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QGoodsDataSourceDetailUnitConvertMappingId extends BeanPath<GoodsDataSourceDetailUnitConvertMappingId> {

    private static final long serialVersionUID = 950807952L;

    public static final QGoodsDataSourceDetailUnitConvertMappingId goodsDataSourceDetailUnitConvertMappingId = new QGoodsDataSourceDetailUnitConvertMappingId("goodsDataSourceDetailUnitConvertMappingId");

    public final NumberPath<Integer> goodsDataSourceNo = createNumber("goodsDataSourceNo", Integer.class);

    public final NumberPath<Integer> unitConvNo = createNumber("unitConvNo", Integer.class);

    public QGoodsDataSourceDetailUnitConvertMappingId(String variable) {
        super(GoodsDataSourceDetailUnitConvertMappingId.class, forVariable(variable));
    }

    public QGoodsDataSourceDetailUnitConvertMappingId(Path<? extends GoodsDataSourceDetailUnitConvertMappingId> path) {
        super(path.getType(), path.getMetadata());
    }

    public QGoodsDataSourceDetailUnitConvertMappingId(PathMetadata metadata) {
        super(GoodsDataSourceDetailUnitConvertMappingId.class, metadata);
    }

}

