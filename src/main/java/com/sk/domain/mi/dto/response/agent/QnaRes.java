package com.sk.domain.mi.dto.response.agent;

import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.dto.AuditInfo;
import com.sk.domain.shared.entity.master.tcm.ChatbotTalkQna;
import com.sk.domain.shared.enums.agent.AnswerStatusCd;
import com.sk.domain.shared.enums.agent.AssessmentFeedbackCd;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record QnaRes(

    @Schema(description = "질의응답 ID", example = "1004")
    Integer qnaNo,

    @Schema(description = "질문", example = "삼성전자 주가는?")
    String question,

    @Schema(description = "AI답변", example = "?")
    String answer,

    @Schema(description = "추천 여부", example = "true")
    Boolean isRecommended,

    @Schema(description = "비추천 여부", example = "false")
    Boolean isNotRecommended,

    @Schema(description = "채팅 상태 코드", example = "COMPLETE", allowableValues = {"COMPLETE", "PENDING", "FAIL"})
    AnswerStatusCd statusCd,

    @Schema(description = "채팅 상태 명", example = "완료", allowableValues = {"완료", "대기", "실패"})
    String statusNm,

    @Schema(description = "피드백 코드", example = "OTHER", allowableValues = {"BUG_ISSUE", "NO_POSSIBLE_RESPONSE", "NOT_FOLLOW_REQUEST", "INACCURATE_INFORMATION", "INCOMPLETE_RESPONSE", "OTHER"})
    AssessmentFeedbackCd feedbackCd,

    @Schema(description = "피드백 명", example = "기타", allowableValues = {"버그문제", "가능한 응답이 없음", "요청에 제대로 따르지 않음", "정보가 정확하지 않음", "응답이 완전하지 않음", "기타"})
    String feedbackNm,

    @Schema(description = "피드백 내용", example = "너무 느림")
    String feedbackComment,

    AuditInfo auditInfo

) {

    @QueryProjection
    public QnaRes(ChatbotTalkQna entity) {
        this(
            entity.getChatbotTalkQnaNo(),
            entity.getUserQuestionCntt(),
            entity.getChatbotAnswerCntt(),
            entity.getIsRecommended(),
            entity.getIsNotRecommended(),
            entity.getAnswerStatusCd(),
            entity.getAnswerStatusCd().getDescription(),
            entity.getAssessmentFeedbackCd(),
            entity.getAssessmentFeedbackCd().getDescription(),
            entity.getAssessmentFeedbackCntt(),
            AuditInfo.ofSimple(
                entity.getCreateUserId(),
                entity.getCreateDt(),
                entity.getModifyUserId(),
                entity.getModifyDt()
            )
        );
    }


    @Builder
    public QnaRes(Integer qnaNo, String question, String answer, Boolean isRecommended, Boolean isNotRecommended, AnswerStatusCd statusCd, String statusNm, AssessmentFeedbackCd feedbackCd, String feedbackNm, String feedbackComment, AuditInfo auditInfo) {
        this.qnaNo = qnaNo;
        this.question = question;
        this.answer = answer;
        this.isRecommended = isRecommended;
        this.isNotRecommended = isNotRecommended;
        this.statusCd = statusCd;
        this.statusNm = statusNm;
        this.feedbackCd = feedbackCd;
        this.feedbackNm = feedbackNm;
        this.feedbackComment = feedbackComment;
        this.auditInfo = auditInfo;
    }

}
