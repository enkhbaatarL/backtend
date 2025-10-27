package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Comment("TMD_뉴스산업분류")
@Table(name = "tmd_nws_ind_cls")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsIndustryClassifications extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nws_ind_cls_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("뉴스산업분류 ID")
    private Integer newsIndustryClassificationId;

    @Column(name = "nws_smry_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("뉴스요약 ID")
    private Integer newsSummaryId;

    @Column(name = "nws_ind_sctr_cd", nullable = false, length = 20)
    @Comment("뉴스산업부문코드")
    private String newsIndustrySectorCd;

    @Builder
    public NewsIndustryClassifications(
        Integer newsSummaryId,
        String newsIndustrySectorCd) {
        this.newsSummaryId = newsSummaryId;
        this.newsIndustrySectorCd = newsIndustrySectorCd;
    }
}
