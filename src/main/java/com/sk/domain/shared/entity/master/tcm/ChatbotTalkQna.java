package com.sk.domain.shared.entity.master.tcm;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.mi.dto.request.agent.QnaAssessmentReq;
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

    @Column(name = "talk_qna_stat_cd", length = 1, columnDefinition = "varchar(1)")
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

    @Builder
    public ChatbotTalkQna(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId, Boolean isDeleted, Integer chatbotTalkQnaNo, ChatbotTalk chatbotTalk, String userQuestionCntt, String chatbotAnswerCntt, Boolean isRecommended, Boolean isNotRecommended, AnswerStatusCd answerStatusCd, AssessmentFeedbackCd assessmentFeedbackCd, String assessmentFeedbackCntt) {
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

}

