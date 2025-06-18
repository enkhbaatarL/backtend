package com.sk.domain.shared.entity.master.tcm;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChatbotTalkQna is a Querydsl query type for ChatbotTalkQna
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChatbotTalkQna extends EntityPathBase<ChatbotTalkQna> {

    private static final long serialVersionUID = -1299054462L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QChatbotTalkQna chatbotTalkQna = new QChatbotTalkQna("chatbotTalkQna");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    public final EnumPath<com.sk.domain.shared.enums.agent.AnswerStatusCd> answerStatusCd = createEnum("answerStatusCd", com.sk.domain.shared.enums.agent.AnswerStatusCd.class);

    public final EnumPath<com.sk.domain.shared.enums.agent.AssessmentFeedbackCd> assessmentFeedbackCd = createEnum("assessmentFeedbackCd", com.sk.domain.shared.enums.agent.AssessmentFeedbackCd.class);

    public final StringPath assessmentFeedbackCntt = createString("assessmentFeedbackCntt");

    public final StringPath chatbotAnswerCntt = createString("chatbotAnswerCntt");

    public final QChatbotTalk chatbotTalk;

    public final NumberPath<Integer> chatbotTalkQnaNo = createNumber("chatbotTalkQnaNo", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final NumberPath<Integer> inputTokenCnt = createNumber("inputTokenCnt", Integer.class);

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    public final BooleanPath isNotRecommended = createBoolean("isNotRecommended");

    public final BooleanPath isRecommended = createBoolean("isRecommended");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public final NumberPath<Integer> outputTokenCnt = createNumber("outputTokenCnt", Integer.class);

    public final StringPath userQuestionCntt = createString("userQuestionCntt");

    public QChatbotTalkQna(String variable) {
        this(ChatbotTalkQna.class, forVariable(variable), INITS);
    }

    public QChatbotTalkQna(Path<? extends ChatbotTalkQna> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QChatbotTalkQna(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QChatbotTalkQna(PathMetadata metadata, PathInits inits) {
        this(ChatbotTalkQna.class, metadata, inits);
    }

    public QChatbotTalkQna(Class<? extends ChatbotTalkQna> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.chatbotTalk = inits.isInitialized("chatbotTalk") ? new QChatbotTalk(forProperty("chatbotTalk"), inits.get("chatbotTalk")) : null;
    }

}

