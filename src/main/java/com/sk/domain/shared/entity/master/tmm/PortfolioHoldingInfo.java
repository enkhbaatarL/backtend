package com.sk.domain.shared.entity.master.tmm;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.compositeKey.tmm.PortfolioHoldingInfoId;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "tmm_ptfhdinfom")
@Comment("TMM_포트폴리오종목정보기본")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioHoldingInfo extends BaseEntity {

    @EmbeddedId
    private PortfolioHoldingInfoId id;

    @Column(name = "aset_grp_nm", length = 50)
    @Comment("자산그룹명")
    private String assetGroupName;

    @Column(name = "pref_rto", precision = 6, scale = 4)
    @Comment("선호비율")
    @ColumnDefault("0.0000")
    private BigDecimal preferenceRatio;

    @Column(name = "ascl_spgr_rnk", columnDefinition = "smallint unsigned")
    @Comment("자산군비중순위")
    private Integer assetClassSpecificRank;

    @Column(name = "optm_rbln_yn", length = 1, columnDefinition = "char(1)")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    @Comment("최적리밸런싱여부")
    @ColumnDefault("'N'")
    private Boolean isOptimalRebalancing;

    @Builder
    public PortfolioHoldingInfo(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId,
            String modifyUserId,
            Boolean isDeleted, PortfolioHoldingInfoId id, String assetGroupName,
            BigDecimal preferenceRatio, Integer assetClassSpecificRank, Boolean isOptimalRebalancing) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.id = id;
        this.assetGroupName = assetGroupName;
        this.preferenceRatio = preferenceRatio;
        this.assetClassSpecificRank = assetClassSpecificRank;
        this.isOptimalRebalancing = isOptimalRebalancing;
    }
}