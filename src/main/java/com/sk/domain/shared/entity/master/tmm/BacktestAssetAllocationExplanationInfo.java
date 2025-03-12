package com.sk.domain.shared.entity.master.tmm;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.compositeKey.tmm.BacktestAssetAllocationExplanationInfoId;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "tmm_btstasetallcexppinfom")
@Comment("TMM_백테스팅자산배분설명력정보기준")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BacktestAssetAllocationExplanationInfo extends BaseEntity {

    @EmbeddedId
    private BacktestAssetAllocationExplanationInfoId id;

    @Column(name = "ric_cd_rel_cntt", length = 100)
    @Comment("RIC코드관련내용")
    private String ricCodeRelatedContent;

    @Column(name = "expp_scre", precision = 6, scale = 2, columnDefinition = "decimal(6,2)")
    @Comment("설명력점수")
    private BigDecimal explanationScore;

    @Column(name = "expp_rnk", columnDefinition = "smallint unsigned")
    @Comment("설명력순위")
    private Integer explanationRank;

    @Column(name = "expp_dir_cd", length = 2)
    @Comment("설명력방향코드")
    private String explanationDirectionCode;

    @Builder
    public BacktestAssetAllocationExplanationInfo(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId,
            String modifyUserId, Boolean isDeleted,
            BacktestAssetAllocationExplanationInfoId id,
            String ricCodeRelatedContent, BigDecimal explanationScore,
            Integer explanationRank, String explanationDirectionCode) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.id = id;
        this.ricCodeRelatedContent = ricCodeRelatedContent;
        this.explanationScore = explanationScore;
        this.explanationRank = explanationRank;
        this.explanationDirectionCode = explanationDirectionCode;
    }
}