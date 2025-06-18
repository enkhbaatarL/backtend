package com.sk.domain.shared.entity.master.tcm;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChatbotTalk is a Querydsl query type for ChatbotTalk
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChatbotTalk extends EntityPathBase<ChatbotTalk> {

    private static final long serialVersionUID = 957244930L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QChatbotTalk chatbotTalk = new QChatbotTalk("chatbotTalk");

    public final com.sk.domain.shared.entity.base.QBaseEntity _super = new com.sk.domain.shared.entity.base.QBaseEntity(this);

    public final StringPath chatbotTalkUuid = createString("chatbotTalkUuid");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDt = _super.createDt;

    //inherited
    public final StringPath createUserId = _super.createUserId;

    public final StringPath finalTalkDt = createString("finalTalkDt");

    //inherited
    public final BooleanPath isDeleted = _super.isDeleted;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDt = _super.modifyDt;

    //inherited
    public final StringPath modifyUserId = _super.modifyUserId;

    public final StringPath talkBeginDt = createString("talkBeginDt");

    public final StringPath talkFirstQuestionCntt = createString("talkFirstQuestionCntt");

    public final StringPath talkTitleNm = createString("talkTitleNm");

    public final com.sk.domain.shared.entity.master.tmd.QUser user;

    public QChatbotTalk(String variable) {
        this(ChatbotTalk.class, forVariable(variable), INITS);
    }

    public QChatbotTalk(Path<? extends ChatbotTalk> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QChatbotTalk(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QChatbotTalk(PathMetadata metadata, PathInits inits) {
        this(ChatbotTalk.class, metadata, inits);
    }

    public QChatbotTalk(Class<? extends ChatbotTalk> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new com.sk.domain.shared.entity.master.tmd.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

