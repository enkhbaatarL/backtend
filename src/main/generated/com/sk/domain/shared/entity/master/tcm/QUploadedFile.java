package com.sk.domain.shared.entity.master.tcm;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUploadedFile is a Querydsl query type for UploadedFile
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUploadedFile extends EntityPathBase<UploadedFile> {

    private static final long serialVersionUID = -307626091L;

    public static final QUploadedFile uploadedFile = new QUploadedFile("uploadedFile");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final StringPath fileExtNm = createString("fileExtNm");

    public final StringPath fileHashVal = createString("fileHashVal");

    public final NumberPath<Long> fileNo = createNumber("fileNo", Long.class);

    public final NumberPath<Integer> filePageCnt = createNumber("filePageCnt", Integer.class);

    public final NumberPath<Integer> fileRelationshipObjectDistinguishNo = createNumber("fileRelationshipObjectDistinguishNo", Integer.class);

    public final StringPath fileRelationshipObjectSeparateCd = createString("fileRelationshipObjectSeparateCd");

    public final NumberPath<Long> fileSize = createNumber("fileSize", Long.class);

    public final StringPath fileValidEndDt = createString("fileValidEndDt");

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final BooleanPath isImageConversionCompleted = createBoolean("isImageConversionCompleted");

    public final StringPath mimeTypeNm = createString("mimeTypeNm");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public final StringPath originalFileNm = createString("originalFileNm");

    public final StringPath storageFileNm = createString("storageFileNm");

    public final StringPath storagePathNm = createString("storagePathNm");

    public QUploadedFile(String variable) {
        super(UploadedFile.class, forVariable(variable));
    }

    public QUploadedFile(Path<? extends UploadedFile> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUploadedFile(PathMetadata metadata) {
        super(UploadedFile.class, metadata);
    }

}

