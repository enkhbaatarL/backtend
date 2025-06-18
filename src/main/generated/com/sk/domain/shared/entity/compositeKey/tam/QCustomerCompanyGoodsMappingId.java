package com.sk.domain.shared.entity.compositeKey.tam;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QCustomerCompanyGoodsMappingId is a Querydsl query type for CustomerCompanyGoodsMappingId
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QCustomerCompanyGoodsMappingId extends BeanPath<CustomerCompanyGoodsMappingId> {

    private static final long serialVersionUID = -828536207L;

    public static final QCustomerCompanyGoodsMappingId customerCompanyGoodsMappingId = new QCustomerCompanyGoodsMappingId("customerCompanyGoodsMappingId");

    public final NumberPath<Integer> customerCompanyNo = createNumber("customerCompanyNo", Integer.class);

    public final EnumPath<com.sk.domain.shared.enums.GoodsAuthManagementSeparateCd> goodsAuthManagementSeparateCd = createEnum("goodsAuthManagementSeparateCd", com.sk.domain.shared.enums.GoodsAuthManagementSeparateCd.class);

    public final NumberPath<Integer> goodsNo = createNumber("goodsNo", Integer.class);

    public QCustomerCompanyGoodsMappingId(String variable) {
        super(CustomerCompanyGoodsMappingId.class, forVariable(variable));
    }

    public QCustomerCompanyGoodsMappingId(Path<? extends CustomerCompanyGoodsMappingId> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCustomerCompanyGoodsMappingId(PathMetadata metadata) {
        super(CustomerCompanyGoodsMappingId.class, metadata);
    }

}

