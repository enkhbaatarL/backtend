package com.sk.domain.shared.entity.master.tcm;

import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

import com.sk.domain.shared.dto.request.common.UserFeedbackReq;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.master.tmd.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "tcm_usrfedbl")
@Comment("TCM_사용자피드백내역")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserFeedback extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usr_fedb_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("사용자피드백번호")
    private Long userFeedbackNo;

    @Column(name = "fedb_page_nm", length = 50)
    @Comment("피드백페이지명")
    private String feedbackPageNm;

    @Column(name = "fedb_sectn_nm", length = 50)
    @Comment("피드백섹션명")
    private String feedbackSectionNm;

    @Column(name = "fedb_type_cd", nullable = false, length = 20)
    @Comment("피드백유형코드")
    private String feedbackTypeCd;

    @Column(name = "fedb_cntt", length = 2000)
    @Comment("피드백내용")
    private String feedbackContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usr_no", referencedColumnName = "usr_no", nullable = false)
    @Comment("사용자번호")
    private User user;

    @Builder
    public UserFeedback(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId, Boolean isDeleted,
                        Long userFeedbackNo, String feedbackPageNm, String feedbackSectionNm, String feedbackTypeCd, String feedbackContent,
                        User user) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.userFeedbackNo = userFeedbackNo;
        this.feedbackPageNm = feedbackPageNm;
        this.feedbackSectionNm = feedbackSectionNm;
        this.feedbackTypeCd = feedbackTypeCd;
        this.feedbackContent = feedbackContent;
        this.user = user;
    }

    public static UserFeedback of(UserFeedbackReq request, User user) {
        return UserFeedback.builder()
                .feedbackPageNm(request.feedbackPageNm())
                .feedbackSectionNm(request.feedbackSectionNm())
                .feedbackTypeCd(request.feedbackTypeCd().getCode())
                .feedbackContent(request.feedbackContent())
                .user(user)
                .build();
    }

}
