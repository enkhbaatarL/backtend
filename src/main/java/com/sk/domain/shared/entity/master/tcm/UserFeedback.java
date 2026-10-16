package com.sk.domain.shared.entity.master.tcm;

import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

import com.sk.domain.shared.dto.request.common.McpUserFeedbackReq;
import com.sk.domain.shared.dto.request.common.UserFeedbackReq;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.FeedbackChannelCd;
import com.sk.domain.shared.enums.FeedbackSectionNm;
import com.sk.domain.shared.enums.FeedbackTypeCd;

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

    // [MCP Feedback] fedb_page_nm, fedb_sectn_nm 길이
    private static final int NAME_MAX_LENGTH = 300;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usr_fedb_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("사용자피드백번호")
    private Long userFeedbackNo;

    @Column(name = "fedb_page_nm", length = 300)
    @Comment("피드백페이지명")
    private String feedbackPageNm;

    @Column(name = "fedb_sectn_nm", length = 300)
    @Comment("피드백섹션명")
    private String feedbackSectionNm;

    // [MCP Feedback] 대상 ID. 종류는 feedbackSectionNm 이 정한다 (Event / Scenario Group / Scenario)
    @Column(name = "fedb_trgt_id", columnDefinition = "bigint unsigned")
    @Comment("피드백대상ID")
    private Long feedbackTargetId;

    @Column(name = "fedb_type_cd", nullable = false, length = 20)
    @Comment("피드백유형코드")
    private String feedbackTypeCd;

    // [MCP Feedback] JPA 는 이 컬럼을 INSERT 에 명시적으로 넣으므로 DB 기본값 대신 생성자에서 SCREEN 을 채운다
    @Column(name = "fedb_chnl_cd", nullable = false, length = 20)
    @Comment("피드백채널코드")
    private String feedbackChannelCd;

    // [MCP Feedback] varchar(2000) → mediumtext
    @Column(name = "fedb_cntt", columnDefinition = "mediumtext")
    @Comment("피드백내용")
    private String feedbackContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usr_no", referencedColumnName = "usr_no", nullable = false)
    @Comment("사용자번호")
    private User user;

    @Builder
    public UserFeedback(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId, Boolean isDeleted,
                        Long userFeedbackNo, String feedbackPageNm, String feedbackSectionNm, String feedbackTypeCd, String feedbackContent,
                        User user, Long feedbackTargetId, String feedbackChannelCd) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.userFeedbackNo = userFeedbackNo;
        this.feedbackPageNm = feedbackPageNm;
        this.feedbackSectionNm = feedbackSectionNm;
        this.feedbackTargetId = feedbackTargetId;
        this.feedbackTypeCd = feedbackTypeCd;
        // [MCP Feedback] 채널 미지정(기존 화면 경로 of())은 SCREEN — NOT NULL 컬럼이라 비워 두면 화면 피드백 저장이 실패한다
        this.feedbackChannelCd = feedbackChannelCd != null ? feedbackChannelCd : FeedbackChannelCd.SCREEN.getCode();
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

    /**
     * [MCP Feedback] MCP 피드백. 내용 외의 값은 해석하지 못해도 거부하지 않고 저장한다.
     * 유형은 모르면 etc, 섹션은 지원값으로 맞추되 모르면 원문, 대상 ID 는 숫자가 아니면 null.
     */
    public static UserFeedback ofMcp(McpUserFeedbackReq request, User user) {
        String sectionNm = FeedbackSectionNm.fromLenient(request.feedbackSectionNm())
            .map(FeedbackSectionNm::getValue)
            .orElseGet(() -> truncate(request.feedbackSectionNm()));
        return UserFeedback.builder()
                .feedbackChannelCd(FeedbackChannelCd.MCP.getCode())
                .feedbackPageNm(truncate(request.feedbackPageNm()))
                .feedbackSectionNm(sectionNm)
                .feedbackTargetId(parseTargetId(request.targetId()))
                .feedbackTypeCd(FeedbackTypeCd.fromLenient(request.feedbackTypeCd()).getCode())
                .feedbackContent(request.feedbackContent().strip())
                .user(user)
                .build();
    }

    // [MCP Feedback] 이름 칸(300자)에 맞춰 자른다. 빈 값은 null
    private static String truncate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String stripped = value.strip();
        return stripped.length() > NAME_MAX_LENGTH ? stripped.substring(0, NAME_MAX_LENGTH) : stripped;
    }

    // [MCP Feedback] "1269", " 1269 ", "#1269" 만 ID 로 인정. 그 외는 거부 대신 null (이름·내용으로 운영자가 판단)
    private static Long parseTargetId(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.strip().replaceFirst("^#", "");
        if (!digits.matches("\\d{1,18}")) {
            return null;
        }
        long id = Long.parseLong(digits);
        return id > 0 ? id : null;
    }

}
