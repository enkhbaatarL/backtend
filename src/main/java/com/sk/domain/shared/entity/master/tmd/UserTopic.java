package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.topic.TopicApprovalStatusCode;
import com.sk.domain.shared.enums.topic.TopicRejectReasonCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

/**
 * 사용자 토픽 Entity
 */
@Entity
@Table(
    name = "tmd_usrtopm",
    indexes = {
        @Index(name = "tmd_usrtopm_ix01", columnList = "usr_no"),
        @Index(name = "tmd_usrtopm_ix02", columnList = "nws_top_id"),
        @Index(name = "tmd_usrtopm_ix03", columnList = "aprv_stat_cd")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "tmd_usrtopm_ux01", columnNames = {"usr_no", "usr_top_nm"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("TMD_사용자 토픽")
public class UserTopic extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usr_top_id", columnDefinition = "INT UNSIGNED")
    @Comment("사용자 토픽 ID")
    private Long userTopicId;

    @Column(name = "usr_no", columnDefinition = "INT UNSIGNED", nullable = false)
    @Comment("사용자 번호 (tmd_usrm.user_no)")
    private Long userNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nws_top_id")
    @Comment("뉴스 토픽 마스터 ID (관리자 승인 시 매핑)")
    private NewsTopic newsTopic;

    @Column(name = "usr_top_nm", length = 200, nullable = false)
    @Comment("사용자 등록 토픽 명")
    private String userTopicName;

    @Column(name = "use_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @ColumnDefault("'Y'")
    @Comment("사용 여부 (기본값 Y, Enabled/Disabled)")
    private String useYn;

    @Enumerated(EnumType.STRING)
    @Column(name = "aprv_stat_cd", length = 20, nullable = false)
    @ColumnDefault("'PEN'")
    @Comment("승인 상태 코드 (Pending/Approved/Not Approved)")
    private TopicApprovalStatusCode approvalStatusCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "rjct_rsn_cd", length = 100)
    @Comment("승인 거절 이유 코드 (Low Data/Irrelevant/Duplicate)")
    private TopicRejectReasonCode rejectReasonCode;

    @Builder
    public UserTopic(Long userNo, NewsTopic newsTopic, String userTopicName,
                     String useYn, TopicApprovalStatusCode approvalStatusCode,
                     TopicRejectReasonCode rejectReasonCode) {
        this.userNo = userNo;
        this.newsTopic = newsTopic; //
        this.userTopicName = userTopicName;
        this.useYn = useYn != null ? useYn : "Y";
        this.approvalStatusCode = approvalStatusCode != null ? approvalStatusCode : TopicApprovalStatusCode.PENDING;
        this.rejectReasonCode = rejectReasonCode;
    }
}
