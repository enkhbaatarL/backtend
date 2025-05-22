package com.sk.domain.shared.entity.master.tcm;

import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "tcm_chatbmajquesm")
@Comment("TCM_챗봇주요질문기본")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatbotMajorQuestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chatb_maj_ques_no", nullable = false, columnDefinition = "mediumint unsigned")
    @Comment("챗봇주요질문번호")
    private Integer chatbotMajorQuestionNo;

    @Column(name = "mrkt_idx_anly_tgt_cd", length = 15, nullable = false)
    @Comment("시장지수분석대상코드")
    private String marketIndexAnalysisTargetCd;

    @Column(name = "maj_ques_cntt", length = 250, nullable = false)
    @Comment("주요질문내용")
    private String majorQuestionCntt;

    @Column(name = "maj_ques_rnk", columnDefinition = "smallint unsigned")
    @Comment("주요질문순위")
    private Integer majorQuestionRnk;

    @Builder
    public ChatbotMajorQuestion(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId, Boolean isDeleted, Integer chatbotMajorQuestionNo, String marketIndexAnalysisTargetCd, String majorQuestionCntt, Integer majorQuestionRnk) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.chatbotMajorQuestionNo = chatbotMajorQuestionNo;
        this.marketIndexAnalysisTargetCd = marketIndexAnalysisTargetCd;
        this.majorQuestionCntt = majorQuestionCntt;
        this.majorQuestionRnk = majorQuestionRnk;
    }

}
