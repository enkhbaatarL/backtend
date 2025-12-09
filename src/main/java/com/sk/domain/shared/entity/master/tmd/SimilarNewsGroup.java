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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 유사 뉴스 그룹 Entity
 */
@Entity
@Table(
    name = "tmd_simlr_nws_grp",
    indexes = {
        @Index(name = "tmd_simlr_nws_grp_ix01", columnList = "nws_smry_id"),
        @Index(name = "tmd_simlr_nws_grp_ix02", columnList = "crtr_dt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("TMD_유사뉴스그룹")
public class SimilarNewsGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "simlr_nws_grp_id", columnDefinition = "INT UNSIGNED")
    @Comment("유사 뉴스 그룹 ID")
    private Long similarNewsGroupId;

    @Column(name = "crtr_dt", length = 8, nullable = false)
    @Comment("기준 일자 (YYYYMMDD)")
    private String crtrDt;

    @Column(name = "nws_smry_id", columnDefinition = "INT UNSIGNED", nullable = false, insertable = false, updatable = false)
    @Comment("대표 뉴스의 뉴스 요약 ID")
    private Long newsSummaryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nws_smry_id", referencedColumnName = "nws_smry_id", nullable = false)
    @Comment("대표 뉴스 요약")
    private NewsSummary representativeNewsSummary;

    @Builder
    public SimilarNewsGroup(String crtrDt, NewsSummary representativeNewsSummary) {
        this.crtrDt = crtrDt;
        this.representativeNewsSummary = representativeNewsSummary;
    }
}
