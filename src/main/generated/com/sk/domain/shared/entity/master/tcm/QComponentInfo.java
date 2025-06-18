package com.sk.domain.shared.entity.master.tcm;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QComponentInfo is a Querydsl query type for ComponentInfo
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QComponentInfo extends EntityPathBase<ComponentInfo> {

    private static final long serialVersionUID = -1041237294L;

    public static final QComponentInfo componentInfo = new QComponentInfo("componentInfo");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    public final StringPath componentTypeId = createString("componentTypeId");

    public final StringPath componentTypeNm = createString("componentTypeNm");

    public final StringPath convertTargetDetailTypeNm = createString("convertTargetDetailTypeNm");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final EnumPath<com.sk.domain.shared.enums.ComponentType> dataStructureConvertNm = createEnum("dataStructureConvertNm", com.sk.domain.shared.enums.ComponentType.class);

    public final StringPath excelColumnListNm = createString("excelColumnListNm");

    public final StringPath excelConditionCntt = createString("excelConditionCntt");

    public final StringPath excelTableColumnListNm = createString("excelTableColumnListNm");

    public final StringPath goodsDataSourceCurrencyUnitNm = createString("goodsDataSourceCurrencyUnitNm");

    public final StringPath goodsDataSourceNm = createString("goodsDataSourceNm");

    public final StringPath goodsDataSourceUnitNm = createString("goodsDataSourceUnitNm");

    public final BooleanPath isCandleChartPossible = createBoolean("isCandleChartPossible");

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public final StringPath queryConditionCntt = createString("queryConditionCntt");

    public final StringPath tableColumnListNm = createString("tableColumnListNm");

    public final StringPath targetDbNm = createString("targetDbNm");

    public final StringPath targetTableNm = createString("targetTableNm");

    public QComponentInfo(String variable) {
        super(ComponentInfo.class, forVariable(variable));
    }

    public QComponentInfo(Path<? extends ComponentInfo> path) {
        super(path.getType(), path.getMetadata());
    }

    public QComponentInfo(PathMetadata metadata) {
        super(ComponentInfo.class, metadata);
    }

}

