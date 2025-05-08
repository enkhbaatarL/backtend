package com.sk.domain.shared.repository.master.tcm.custom;

import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.response.agent.QQnaRes;
import com.sk.domain.mi.dto.response.agent.QnaRes;
import com.sk.domain.shared.entity.master.tcm.ChatbotTalk;
import com.sk.domain.shared.entity.master.tmd.User;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;
import java.util.Optional;

import static com.sk.domain.shared.entity.master.tcm.QChatbotTalk.chatbotTalk;
import static com.sk.domain.shared.entity.master.tcm.QChatbotTalkQna.chatbotTalkQna;

@RequiredArgsConstructor
public class CustomChatbotTalkQnaRepositoryImpl implements CustomChatbotTalkQnaRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<QnaRes> findQnaByConditions(int page, int size, String talkId, User user) {

        Optional<ChatbotTalk> talk = Optional.ofNullable(
            queryFactory
                .selectFrom(chatbotTalk)
                .where(chatbotTalk.chatbotTalkUuid.eq(talkId))
                .fetchOne()
        );

        if (talk.isEmpty()) {
            return PageableExecutionUtils.getPage(List.of(), PageRequest.of(page - 1, size), () -> 0L);
        }

        if (BooleanUtils.isFalse(talk.get().getUser().getUserNo().equals(user.getUserNo()))) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "you do not have permission to access other users talk.");
        }


        Pageable pageable = PageRequest.of(page - 1, size);

        List<QnaRes> content = queryFactory
            .select(new QQnaRes(chatbotTalkQna))
            .from(chatbotTalkQna)
            .leftJoin(chatbotTalkQna.chatbotTalk, chatbotTalk).fetchJoin()
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .where(chatbotTalk.isDeleted.isFalse(),
                chatbotTalk.chatbotTalkUuid.eq(talkId),
                chatbotTalkQna.isDeleted.isFalse()
            )
            .orderBy(chatbotTalkQna.createDt.desc())
            .fetch();

        JPQLQuery<Long> countQuery = queryFactory
            .select(chatbotTalkQna.count())
            .from(chatbotTalkQna)
            .leftJoin(chatbotTalkQna.chatbotTalk, chatbotTalk)
            .where(chatbotTalk.isDeleted.isFalse(),
                chatbotTalk.chatbotTalkUuid.eq(talkId),
                chatbotTalkQna.isDeleted.isFalse()
            );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);

    }

}
