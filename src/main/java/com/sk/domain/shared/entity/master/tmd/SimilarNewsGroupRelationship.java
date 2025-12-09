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
import org.hibernate.annotations.Comment;

/**
 * 유사 뉴스 그룹 관계 Entity
 */
@Entity
@Table(
    name = "tmd_simlr_nws_grp_rel",
    indexes = {
        @Index(name = "tmd_simlr_nws_grp_rel_ix01", columnList = "simlr_nws_grp_id"),
        @Index(name = "tmd_simlr_nws_grp_rel_ix02", columnList = "nws_smry_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "tmd_simlr_nws_grp_rel_ux01", columnNames = {"simlr_nws_grp_id", "nws_smry_id"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("TMD_유사뉴스그룹관계")
public class SimilarNewsGroupRelationship extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "simlr_nws_grp_rel_id", columnDefinition = "INT UNSIGNED")
    @Comment("유사 뉴스 그룹 관계 ID")
    private Long similarNewsGroupRelationshipId;

    @Column(name = "simlr_nws_grp_id", columnDefinition = "INT UNSIGNED", nullable = false, insertable = false, updatable = false)
    @Comment("유사 뉴스 그룹 ID")
    private Long similarNewsGroupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "simlr_nws_grp_id", nullable = false)
    @Comment("유사 뉴스 그룹")
    private SimilarNewsGroup similarNewsGroup;

    @Column(name = "nws_smry_id", columnDefinition = "INT UNSIGNED", nullable = false, insertable = false, updatable = false)
    @Comment("유사 뉴스 그룹에 속하는 뉴스의 뉴스 요약 ID")
    private Long newsSummaryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nws_smry_id", nullable = false)
    @Comment("유사 뉴스")
    private NewsSummary newsSummary;

    @Builder
    public SimilarNewsGroupRelationship(SimilarNewsGroup similarNewsGroup, NewsSummary newsSummary) {
        this.similarNewsGroup = similarNewsGroup;
        this.newsSummary = newsSummary;
    }
}
