package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 뉴스 토픽 마스터 Entity
 */
@Entity
@Table(
    name = "tmd_nwstopm",
    indexes = {
        @Index(name = "tmd_nwstopm_ix02", columnList = "cre_usr_no")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "tmd_nwstopm_ux01", columnNames = {"nws_top_nm"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("TMD_뉴스 토픽 마스터")
public class NewsTopic extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nws_top_id", columnDefinition = "INT UNSIGNED")
    @Comment("뉴스 토픽 ID")
    private Long newsTopicId;

    @Column(name = "evnt_ctgr_id", columnDefinition = "INT UNSIGNED", insertable = false, updatable = false)
    @Comment("이벤트 카테고리 ID")
    private Long eventCategoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evnt_ctgr_id")
    @Comment("이벤트 카테고리")
    private NewsEventCategory eventCategory;

    @Column(name = "nws_top_nm", length = 200, nullable = false)
    @Comment("뉴스 토픽 명")
    private String newsTopicName;

    @Column(name = "use_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @ColumnDefault("'Y'")
    @Comment("활성화 여부 (기본값 Y)")
    private String useYn;

    @Column(name = "usr_reg_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @ColumnDefault("'N'")
    @Comment("사용자 등록 여부 (Y: 사용자 승인 토픽, N: 시스템 기본 토픽)")
    private String userRegisteredYn;

    @Column(name = "cre_usr_no", columnDefinition = "INT UNSIGNED")
    @Comment("등록자 번호 (사용자 등록 토픽인 경우)")
    private Long creatorUserNo;

    @Builder
    public NewsTopic(NewsEventCategory eventCategory, String newsTopicName, String useYn,
                     String userRegisteredYn, Long creatorUserNo) {
        this.eventCategory = eventCategory;
        this.newsTopicName = newsTopicName;
        this.useYn = useYn != null ? useYn : "Y";
        this.userRegisteredYn = userRegisteredYn != null ? userRegisteredYn : "N";
        this.creatorUserNo = creatorUserNo;
    }
}
