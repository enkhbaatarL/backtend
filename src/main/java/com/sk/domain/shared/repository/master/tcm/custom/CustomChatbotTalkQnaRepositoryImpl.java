package com.sk.domain.shared.repository.master.tcm.custom;

import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.admin.dto.response.company.CompanyRes;
import com.sk.domain.admin.dto.response.company.QCompanyRes;
import com.sk.domain.mi.dto.response.agent.QQnaRes;
import com.sk.domain.mi.dto.response.agent.QnaRes;
import com.sk.domain.shared.dto.AuditInfo;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.UserRoleCd;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

import static com.sk.domain.shared.entity.master.tcm.QChatbotTalk.chatbotTalk;
import static com.sk.domain.shared.entity.master.tcm.QChatbotTalkQna.chatbotTalkQna;
import static com.sk.domain.shared.entity.master.tmd.QCustomerCompany.customerCompany;

@RequiredArgsConstructor
public class CustomChatbotTalkQnaRepositoryImpl implements CustomChatbotTalkQnaRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<QnaRes> findQnaByConditions(int page, int size, String talkId, User user) {

        Pageable pageable = PageRequest.of(page - 1, size);

        List<QnaRes> content = queryFactory
            .select(new QQnaRes(chatbotTalkQna))
            .from(chatbotTalkQna)
            .leftJoin(chatbotTalkQna.chatbotTalk, chatbotTalk).fetchJoin()
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .where(chatbotTalk.isDeleted.isFalse(),
                chatbotTalk.chatbotTalkUuid.eq(talkId),
                chatbotTalk.user.eq(user), // todo 다르면 에러 터트리는게 낫겠다.
                chatbotTalkQna.isDeleted.isFalse()
            )
            .orderBy(chatbotTalkQna.createDt.desc())
            .fetch();

        JPQLQuery<Long> countQuery = queryFactory
            .select(customerCompany.count())
            .from(chatbotTalkQna)
            .leftJoin(chatbotTalkQna.chatbotTalk, chatbotTalk).fetchJoin()
            .where(chatbotTalk.isDeleted.isFalse(),
                chatbotTalk.chatbotTalkUuid.eq(talkId),
                chatbotTalk.user.eq(user), // todo 다르면 에러 터트리는게 낫겠다.
                chatbotTalkQna.isDeleted.isFalse()
            );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);

    }

}
