package com.sk.domain.shared.entity.master.tmm;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.compositeKey.tmm.AssetAllocationInfoId;

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
@Table(name = "tmm_asetallcspgrinfom")
@Comment("TMM_자산배분비중정보기본")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssetAllocationInfo extends BaseEntity {

    @EmbeddedId
    private AssetAllocationInfoId id;

    @Column(name = "invs_type_cd", length = 3)
    @Comment("투자유형코드")
    private String investmentTypeCode;

    @Column(name = "aset_grp_spgr_rto", precision = 6, scale = 4)
    @Comment("자산그룹비중비율")
    @ColumnDefault("0.0000")
    private BigDecimal assetGroupSpecificRatio;

    @Column(name = "optm_rbln_yn", length = 1, columnDefinition = "char(1)")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    @Comment("최적리밸런싱여부")
    @ColumnDefault("'N'")
    private Boolean isOptimalRebalancing;

    @Column(name = "expc_prft_rt", precision = 6, scale = 4)
    @Comment("기대수익률")
    @ColumnDefault("0.0000")
    private BigDecimal expectedProfitRate;

    @Column(name = "vrvl", precision = 6, scale = 4)
    @Comment("분산값")
    @ColumnDefault("0.0000")
    private BigDecimal varianceValue;

    @Builder
    public AssetAllocationInfo(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId,
            Boolean isDeleted, AssetAllocationInfoId id, String investmentTypeCode,
            BigDecimal assetGroupSpecificRatio, Boolean isOptimalRebalancing,
            BigDecimal expectedProfitRate, BigDecimal varianceValue) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.id = id;
        this.investmentTypeCode = investmentTypeCode;
        this.assetGroupSpecificRatio = assetGroupSpecificRatio;
        this.isOptimalRebalancing = isOptimalRebalancing;
        this.expectedProfitRate = expectedProfitRate;
        this.varianceValue = varianceValue;
    }
}