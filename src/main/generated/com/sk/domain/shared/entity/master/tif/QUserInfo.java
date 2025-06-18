package com.sk.domain.shared.entity.master.tif;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUserInfo is a Querydsl query type for UserInfo
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserInfo extends EntityPathBase<UserInfo> {

    private static final long serialVersionUID = 1206311903L;

    public static final QUserInfo userInfo = new QUserInfo("userInfo");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    public final StringPath companyCd = createString("companyCd");

    public final StringPath companyNm = createString("companyNm");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final StringPath departmentCd = createString("departmentCd");

    public final StringPath departmentNm = createString("departmentNm");

    public final StringPath emailAddress = createString("emailAddress");

    public final StringPath employeeNo = createString("employeeNo");

    public final StringPath imCd = createString("imCd");

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final StringPath jobGradeCd = createString("jobGradeCd");

    public final StringPath jobGradeNm = createString("jobGradeNm");

    public final StringPath loginId = createString("loginId");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public final StringPath onDutyCd = createString("onDutyCd");

    public final StringPath onDutyNm = createString("onDutyNm");

    public final StringPath positionCd = createString("positionCd");

    public final StringPath positionNm = createString("positionNm");

    public final StringPath userEnglishNm = createString("userEnglishNm");

    public final StringPath userNm = createString("userNm");

    public QUserInfo(String variable) {
        super(UserInfo.class, forVariable(variable));
    }

    public QUserInfo(Path<? extends UserInfo> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserInfo(PathMetadata metadata) {
        super(UserInfo.class, metadata);
    }

}

