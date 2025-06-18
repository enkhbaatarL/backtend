package com.sk.domain.shared.entity.master.tif;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QCompanyInfo is a Querydsl query type for CompanyInfo
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCompanyInfo extends EntityPathBase<CompanyInfo> {

    private static final long serialVersionUID = -905634747L;

    public static final QCompanyInfo companyInfo = new QCompanyInfo("companyInfo");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    public final StringPath companyCd = createString("companyCd");

    public final StringPath companyEngNm = createString("companyEngNm");

    public final StringPath companyNm = createString("companyNm");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public QCompanyInfo(String variable) {
        super(CompanyInfo.class, forVariable(variable));
    }

    public QCompanyInfo(Path<? extends CompanyInfo> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCompanyInfo(PathMetadata metadata) {
        super(CompanyInfo.class, metadata);
    }

}

