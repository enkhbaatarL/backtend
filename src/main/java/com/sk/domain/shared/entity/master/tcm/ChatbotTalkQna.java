package com.sk.domain.shared.entity.master.tcm;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.common.utils.JsonUtil;
import com.sk.domain.mi.dto.request.agent.QnaAssessmentReq;
import com.sk.domain.mi.dto.request.agent.QnaReq;
import com.sk.domain.mi.dto.response.agent.QnaRes;
import com.sk.domain.mi.dto.response.agent.talk.AnswerRes;
import com.sk.domain.shared.client.dto.ModelAgentRes;
import com.sk.domain.shared.converter.StringToAnswerStatusCdConverter;
import com.sk.domain.shared.converter.StringToAssessmentFeedbackCdConverter;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.agent.AnswerStatusCd;
import com.sk.domain.shared.enums.agent.AssessmentFeedbackCd;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Getter
@Entity
@Table(name = "tcm_chatbtalkqnam")
@Comment("TCM_챗봇대화문답상세")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatbotTalkQna extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chatb_talk_qna_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("챗봇대화문답번호")
    private Integer chatbotTalkQnaNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatb_talk_uuid", referencedColumnName = "chatb_talk_uuid", nullable = false)
    @Comment("챗봇대화UUID")
    private ChatbotTalk chatbotTalk;

    @Column(name = "usr_ques_cntt", length = 250, nullable = false)
    @Comment("사용자질문내용")
    private String userQuestionCntt;

    @Column(name = "chatb_ans_cntt", columnDefinition = "text")
    @Comment("챗봇답변내용")
    private String chatbotAnswerCntt;

    @Column(name = "ans_recm_yn", length = 1, columnDefinition = "char")
    @Comment("답변추천여부")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isRecommended;

    @Column(name = "ans_decm_yn", length = 1, columnDefinition = "char")
    @Comment("답변비추천여부")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isNotRecommended;

    @Column(name = "talk_qna_stat_cd", length = 2, columnDefinition = "varchar(2)")
    @Comment("대화문답상태코드")
    @Convert(converter = StringToAnswerStatusCdConverter.class)
    private AnswerStatusCd answerStatusCd;

    @Column(name = "chatb_asmt_fedb_cd", length = 2)
    @Comment("챗봇평가피드백코드")
    @Convert(converter = StringToAssessmentFeedbackCdConverter.class)
    private AssessmentFeedbackCd assessmentFeedbackCd;

    @Column(name = "chatb_asmt_fedb_cntt", length = 500)
    @Comment("챗봇평가피드백내용")
    private String assessmentFeedbackCntt;

    @Column(name = "inpt_tokn_cnt", nullable = false, columnDefinition = "mediumint unsigned")
    @Comment("입력토큰수")
    private Integer inputTokenCnt;

    @Column(name = "outp_tokn_cnt", nullable = false, columnDefinition = "mediumint unsigned")
    @Comment("출력토큰수")
    private Integer outputTokenCnt;

    @Builder
    public ChatbotTalkQna(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId, Boolean isDeleted,
            Integer chatbotTalkQnaNo, ChatbotTalk chatbotTalk, String userQuestionCntt, String chatbotAnswerCntt, Boolean isRecommended,
            Boolean isNotRecommended, AnswerStatusCd answerStatusCd, AssessmentFeedbackCd assessmentFeedbackCd, String assessmentFeedbackCntt,
            Integer inputTokenCnt, Integer outputTokenCnt) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.chatbotTalkQnaNo = chatbotTalkQnaNo;
        this.chatbotTalk = chatbotTalk;
        this.userQuestionCntt = userQuestionCntt;
        this.chatbotAnswerCntt = chatbotAnswerCntt;
        this.isRecommended = isRecommended;
        this.isNotRecommended = isNotRecommended;
        this.answerStatusCd = answerStatusCd;
        this.assessmentFeedbackCd = assessmentFeedbackCd;
        this.assessmentFeedbackCntt = assessmentFeedbackCntt;
        this.inputTokenCnt = inputTokenCnt;
        this.outputTokenCnt = outputTokenCnt;
    }

    public static ChatbotTalkQna of(QnaReq.Insert talkReq, ChatbotTalk talk) {
        return ChatbotTalkQna.builder()
                .chatbotTalk(talk)
                .userQuestionCntt(talkReq.question().substring(0, Math.min(talkReq.question().length(), 250)))
                .isRecommended(Boolean.FALSE)
                .isNotRecommended(Boolean.FALSE)
                .answerStatusCd(AnswerStatusCd.PENDING) // default
                .build();
    }

    public static ChatbotTalkQna ofFail(QnaReq.Insert talkReq, ChatbotTalk talk, AnswerStatusCd answerStatusCd) {
        return ChatbotTalkQna.builder()
                .chatbotTalk(talk)
                .userQuestionCntt(talkReq.question().substring(0, Math.min(talkReq.question().length(), 250)))
                .answerStatusCd(answerStatusCd)
                .build();
    }

    public void update(QnaAssessmentReq dto) {

        if (ObjectUtils.isNotEmpty(dto.isRecommended())) {
            this.isRecommended = dto.isRecommended();
            this.isNotRecommended = !dto.isRecommended();
        }

        if (ObjectUtils.isNotEmpty(dto.feedbackCd())) {
            this.assessmentFeedbackCd = dto.feedbackCd();
        }

        if (ObjectUtils.isNotEmpty(dto.feedbackComment()) && AssessmentFeedbackCd.OTHER == dto.feedbackCd()) {
            this.assessmentFeedbackCntt = dto.feedbackComment();
        }

    }

    public void updateCompleteResult(ModelAgentRes modelAgentRes, AnswerRes answerRes) {
        this.answerStatusCd = AnswerStatusCd.COMPLETE;
        this.chatbotAnswerCntt = Optional.ofNullable(answerRes).map(JsonUtil::toJson).orElse(null);
        this.inputTokenCnt = Optional.ofNullable(modelAgentRes)
                .map(ModelAgentRes::usage)
                .map(ModelAgentRes.Usage::promptTokens)
                .orElse(0);
        this.outputTokenCnt = Optional.ofNullable(modelAgentRes)
                .map(ModelAgentRes::usage)
                .map(ModelAgentRes.Usage::completionTokens)
                .orElse(0);
    }

    public void updateFailResult() {
        this.answerStatusCd = AnswerStatusCd.FAIL;
    }

    public void updateFailResult(AnswerStatusCd validationStatus) {
        this.answerStatusCd = validationStatus;
    }

}
