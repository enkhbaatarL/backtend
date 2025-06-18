package com.sk.domain.shared.entity.master.tcm;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QChatbotMajorQuestion is a Querydsl query type for ChatbotMajorQuestion
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChatbotMajorQuestion extends EntityPathBase<ChatbotMajorQuestion> {

    private static final long serialVersionUID = 687111209L;

    public static final QChatbotMajorQuestion chatbotMajorQuestion = new QChatbotMajorQuestion("chatbotMajorQuestion");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    public final NumberPath<Integer> chatbotMajorQuestionNo = createNumber("chatbotMajorQuestionNo", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final StringPath majorQuestionCntt = createString("majorQuestionCntt");

    public final NumberPath<Integer> majorQuestionRnk = createNumber("majorQuestionRnk", Integer.class);

    public final StringPath marketIndexAnalysisTargetCd = createString("marketIndexAnalysisTargetCd");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public QChatbotMajorQuestion(String variable) {
        super(ChatbotMajorQuestion.class, forVariable(variable));
    }

    public QChatbotMajorQuestion(Path<? extends ChatbotMajorQuestion> path) {
        super(path.getType(), path.getMetadata());
    }

    public QChatbotMajorQuestion(PathMetadata metadata) {
        super(ChatbotMajorQuestion.class, metadata);
    }

}

