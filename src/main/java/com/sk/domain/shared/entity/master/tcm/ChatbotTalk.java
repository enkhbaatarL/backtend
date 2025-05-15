package com.sk.domain.shared.entity.master.tcm;

import com.sk.common.utils.DateUtil;
import com.sk.domain.mi.dto.request.agent.TalkReq;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.master.tmd.User;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "tcm_chatbtalkm")
@Comment("TCM_챗봇대화기본")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatbotTalk extends BaseEntity {

    @Id
    @Column(name = "chatb_talk_uuid", length = 36, nullable = false, columnDefinition = "char(36)")
    @Comment("챗봇대화UUID")
    private String chatbotTalkUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatb_talk_usr_no", referencedColumnName = "usr_no", nullable = false)
    @Comment("챗봇대화사용자번호")
    private User user;

    @Column(name = "talk_titl_nm", length = 100, nullable = false)
    @Comment("대화제목명")
    private String talkTitleNm;

    @Column(name = "talk_tfst_ques_cntt", length = 250, nullable = false)
    @Comment("대화최초질문내용")
    private String talkFirstQuestionCntt;

    @Column(name = "talk_bgn_dt", length = 8, nullable = false)
    @Comment("대화시작일자")
    private String talkBeginDt;

    @Column(name = "tfin_talk_dt", length = 8, nullable = false)
    @Comment("최종대화일자")
    private String finalTalkDt;

    @Builder
    public ChatbotTalk(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId, Boolean isDeleted, String chatbotTalkUuid, User user, String talkTitleNm, String talkFirstQuestionCntt, String talkBeginDt, String finalTalkDt) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.chatbotTalkUuid = chatbotTalkUuid;
        this.user = user;
        this.talkTitleNm = talkTitleNm;
        this.talkFirstQuestionCntt = talkFirstQuestionCntt;
        this.talkBeginDt = talkBeginDt;
        this.finalTalkDt = finalTalkDt;
    }

    public static ChatbotTalk of(TalkReq.Insert talkReq, User user) {
        return ChatbotTalk.builder()
                .chatbotTalkUuid(talkReq.talkId().toString())
                .user(user)
                .talkTitleNm(talkReq.question())
                .talkFirstQuestionCntt(talkReq.question())
                .talkBeginDt(DateUtil.currentYmd())
                .finalTalkDt(DateUtil.currentYmd())
                .build();
    }

    public void update(TalkReq.Update talkReq) {

        if (ObjectUtils.isNotEmpty(talkReq)) {
            this.talkTitleNm = talkReq.title();
        }

    }

}

