package com.sk.domain.shared.entity.master.tmd;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUser is a Querydsl query type for User
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUser extends EntityPathBase<User> {

    private static final long serialVersionUID = -565612713L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUser user = new QUser("user");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final QCustomerCompany customerCompany;

    public final StringPath departmentNm = createString("departmentNm");

    public final DateTimePath<java.time.LocalDateTime> individualInfoAgreementDtm = createDateTime("individualInfoAgreementDtm", java.time.LocalDateTime.class);

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final BooleanPath isIndividualInfoAgreement = createBoolean("isIndividualInfoAgreement");

    public final BooleanPath isOtherServiceAccessPossible = createBoolean("isOtherServiceAccessPossible");

    public final BooleanPath isUserNotificationSetting = createBoolean("isUserNotificationSetting");

    public final StringPath jobGradeNm = createString("jobGradeNm");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public final StringPath onDutyNm = createString("onDutyNm");

    public final StringPath positionNm = createString("positionNm");

    public final StringPath userLoginId = createString("userLoginId");

    public final StringPath userNm = createString("userNm");

    public final NumberPath<Integer> userNo = createNumber("userNo", Integer.class);

    public final EnumPath<com.sk.domain.shared.enums.UserRoleCd> userRoleCd = createEnum("userRoleCd", com.sk.domain.shared.enums.UserRoleCd.class);

    public QUser(String variable) {
        this(User.class, forVariable(variable), INITS);
    }

    public QUser(Path<? extends User> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUser(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUser(PathMetadata metadata, PathInits inits) {
        this(User.class, metadata, inits);
    }

    public QUser(Class<? extends User> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.customerCompany = inits.isInitialized("customerCompany") ? new QCustomerCompany(forProperty("customerCompany")) : null;
    }

}

