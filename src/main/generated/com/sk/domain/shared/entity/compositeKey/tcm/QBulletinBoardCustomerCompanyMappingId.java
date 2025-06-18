package com.sk.domain.shared.entity.compositeKey.tcm;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QBulletinBoardCustomerCompanyMappingId is a Querydsl query type for BulletinBoardCustomerCompanyMappingId
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QBulletinBoardCustomerCompanyMappingId extends BeanPath<BulletinBoardCustomerCompanyMappingId> {

    private static final long serialVersionUID = 2077595622L;

    public static final QBulletinBoardCustomerCompanyMappingId bulletinBoardCustomerCompanyMappingId = new QBulletinBoardCustomerCompanyMappingId("bulletinBoardCustomerCompanyMappingId");

    public final NumberPath<Integer> bbmNo = createNumber("bbmNo", Integer.class);

    public final NumberPath<Integer> customerCompanyNo = createNumber("customerCompanyNo", Integer.class);

    public QBulletinBoardCustomerCompanyMappingId(String variable) {
        super(BulletinBoardCustomerCompanyMappingId.class, forVariable(variable));
    }

    public QBulletinBoardCustomerCompanyMappingId(Path<? extends BulletinBoardCustomerCompanyMappingId> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBulletinBoardCustomerCompanyMappingId(PathMetadata metadata) {
        super(BulletinBoardCustomerCompanyMappingId.class, metadata);
    }

}

