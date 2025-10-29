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

@Getter
@Entity
@Comment("TMD_뉴스지역분류")
@Table(name = "tmd_nws_rgn_cls",
    indexes = {
        @Index(name = "tmd_nws_rgn_cls_ix_01", columnList = "nws_smry_id")
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsRegionClassifications extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nws_rgn_cls_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("뉴스지역분류 ID")
    private Integer newsRegionClassificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nws_smry_id", referencedColumnName = "nws_smry_id", nullable = false)
    @Comment("뉴스요약")
    private NewsSummary newsSummary;

    @Column(name = "nws_smry_id", nullable = false, columnDefinition = "int unsigned", insertable = false, updatable = false)
    @Comment("뉴스요약 ID")
    private Integer newsSummaryId;

    @Column(name = "nws_rgn_spt_cd", nullable = false, length = 20)
    @Comment("뉴스지역구분코드")
    private String newsRegionSeparationCd;

    @Builder
    public NewsRegionClassifications(
        NewsSummary newsSummary,
        String newsRegionSeparationCd) {
        this.newsSummary = newsSummary;
        this.newsRegionSeparationCd = newsRegionSeparationCd;
    }
}
