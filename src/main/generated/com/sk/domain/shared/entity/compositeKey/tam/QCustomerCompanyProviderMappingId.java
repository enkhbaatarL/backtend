package com.sk.domain.shared.entity.compositeKey.tam;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QCustomerCompanyProviderMappingId is a Querydsl query type for CustomerCompanyProviderMappingId
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QCustomerCompanyProviderMappingId extends BeanPath<CustomerCompanyProviderMappingId> {

    private static final long serialVersionUID = -33425350L;

    public static final QCustomerCompanyProviderMappingId customerCompanyProviderMappingId = new QCustomerCompanyProviderMappingId("customerCompanyProviderMappingId");

    public final NumberPath<Integer> customerCompanyNo = createNumber("customerCompanyNo", Integer.class);

    public final NumberPath<Integer> providerNo = createNumber("providerNo", Integer.class);

    public QCustomerCompanyProviderMappingId(String variable) {
        super(CustomerCompanyProviderMappingId.class, forVariable(variable));
    }

    public QCustomerCompanyProviderMappingId(Path<? extends CustomerCompanyProviderMappingId> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCustomerCompanyProviderMappingId(PathMetadata metadata) {
        super(CustomerCompanyProviderMappingId.class, metadata);
    }

}

