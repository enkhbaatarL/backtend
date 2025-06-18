package com.sk.domain.shared.entity.master.tmd;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCustomerCompany is a Querydsl query type for CustomerCompany
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCustomerCompany extends EntityPathBase<CustomerCompany> {

    private static final long serialVersionUID = -1432720461L;

    public static final QCustomerCompany customerCompany = new QCustomerCompany("customerCompany");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final StringPath customerCompanyCd = createString("customerCompanyCd");

    public final StringPath customerCompanyMipUrl = createString("customerCompanyMipUrl");

    public final StringPath customerCompanyNm = createString("customerCompanyNm");

    public final NumberPath<Integer> customerCompanyNo = createNumber("customerCompanyNo", Integer.class);

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final BooleanPath isMipManagementCompany = createBoolean("isMipManagementCompany");

    public final BooleanPath isUsed = createBoolean("isUsed");

    public final StringPath marketIndexAnalysisTargetCd = createString("marketIndexAnalysisTargetCd");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public final StringPath useBeginDt = createString("useBeginDt");

    public final StringPath useEndDt = createString("useEndDt");

    public final ListPath<User, QUser> users = this.<User, QUser>createList("users", User.class, QUser.class, PathInits.DIRECT2);

    public QCustomerCompany(String variable) {
        super(CustomerCompany.class, forVariable(variable));
    }

    public QCustomerCompany(Path<? extends CustomerCompany> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCustomerCompany(PathMetadata metadata) {
        super(CustomerCompany.class, metadata);
    }

}

