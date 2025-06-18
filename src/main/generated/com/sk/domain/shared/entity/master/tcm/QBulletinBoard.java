package com.sk.domain.shared.entity.master.tcm;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QBulletinBoard is a Querydsl query type for BulletinBoard
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBulletinBoard extends EntityPathBase<BulletinBoard> {

    private static final long serialVersionUID = 286294278L;

    public static final QBulletinBoard bulletinBoard = new QBulletinBoard("bulletinBoard");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    public final StringPath bulletinCategorySeparateCd = createString("bulletinCategorySeparateCd");

    public final StringPath bulletinCntt = createString("bulletinCntt");

    public final NumberPath<Integer> bulletinNo = createNumber("bulletinNo", Integer.class);

    public final StringPath bulletinTitleNm = createString("bulletinTitleNm");

    public final StringPath bulletinTypeCd = createString("bulletinTypeCd");

    public final StringPath bulletinValidBeginDt = createString("bulletinValidBeginDt");

    public final StringPath bulletinValidEndDt = createString("bulletinValidEndDt");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final BooleanPath isPopupUse = createBoolean("isPopupUse");

    public final BooleanPath isTopFixed = createBoolean("isTopFixed");

    public final BooleanPath isValidPerioUnrestrict = createBoolean("isValidPerioUnrestrict");

    public final BooleanPath isWholePost = createBoolean("isWholePost");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public final StringPath popupPostBeginDt = createString("popupPostBeginDt");

    public final StringPath popupPostEndDt = createString("popupPostEndDt");

    public QBulletinBoard(String variable) {
        super(BulletinBoard.class, forVariable(variable));
    }

    public QBulletinBoard(Path<? extends BulletinBoard> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBulletinBoard(PathMetadata metadata) {
        super(BulletinBoard.class, metadata);
    }

}

