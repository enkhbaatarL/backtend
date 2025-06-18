package com.sk.domain.shared.entity.master.tmd;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUnitConvert is a Querydsl query type for UnitConvert
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUnitConvert extends EntityPathBase<UnitConvert> {

    private static final long serialVersionUID = 350717571L;

    public static final QUnitConvert unitConvert = new QUnitConvert("unitConvert");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    public final StringPath calculateFormulaServiceNm = createString("calculateFormulaServiceNm");

    public final StringPath convertAfterUnitNm = createString("convertAfterUnitNm");

    public final NumberPath<java.math.BigDecimal> convertCoef = createNumber("convertCoef", java.math.BigDecimal.class);

    public final StringPath convertPreviousUnitNm = createString("convertPreviousUnitNm");

    public final StringPath convertTargetCategoryNm = createString("convertTargetCategoryNm");

    public final StringPath convertTargetDetailTypeNm = createString("convertTargetDetailTypeNm");

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

    public final StringPath unitConvertExpn = createString("unitConvertExpn");

    public final StringPath unitConvNm = createString("unitConvNm");

    public final NumberPath<Integer> unitConvNo = createNumber("unitConvNo", Integer.class);

    public QUnitConvert(String variable) {
        super(UnitConvert.class, forVariable(variable));
    }

    public QUnitConvert(Path<? extends UnitConvert> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUnitConvert(PathMetadata metadata) {
        super(UnitConvert.class, metadata);
    }

}

