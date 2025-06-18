package com.sk.domain.shared.entity.master.tcm;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QPolicyTactics is a Querydsl query type for PolicyTactics
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPolicyTactics extends EntityPathBase<PolicyTactics> {

    private static final long serialVersionUID = -2066532304L;

    public static final QPolicyTactics policyTactics = new QPolicyTactics("policyTactics");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final BooleanPath isUsed = createBoolean("isUsed");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public final StringPath poltacCntt = createString("poltacCntt");

    public final DateTimePath<java.time.LocalDateTime> poltacEnforcementDt = createDateTime("poltacEnforcementDt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> poltacNo = createNumber("poltacNo", Integer.class);

    public final EnumPath<com.sk.domain.shared.enums.PolicyTacticsSeparateCd> poltacSeparateCd = createEnum("poltacSeparateCd", com.sk.domain.shared.enums.PolicyTacticsSeparateCd.class);

    public final StringPath poltacVersionNm = createString("poltacVersionNm");

    public QPolicyTactics(String variable) {
        super(PolicyTactics.class, forVariable(variable));
    }

    public QPolicyTactics(Path<? extends PolicyTactics> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPolicyTactics(PathMetadata metadata) {
        super(PolicyTactics.class, metadata);
    }

}

