package com.sk.domain.shared.entity.master.tmd;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Comment("TMD_단위변환기본")
@Table(name = "tmd_unitconvm")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UnitConvert extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "unit_conv_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("단위변환번호")
    private Integer unitConvNo;

    @Column(name = "unit_conv_nm", nullable = false, length = 50)
    @Comment("단위변환명")
    private String unitConvNm;

    @Column(name = "conv_tgt_ctgr_nm", nullable = false, length = 50)
    @Comment("변환대상카테고리명")
    private String convertTargetCategoryNm;

    @Column(name = "conv_tgt_dtl_type_nm", length = 50)
    @Comment("변환대상상세유형명")
    private String convertTargetDetailTypeNm;

    @Column(name = "conv_prev_unit_nm", nullable = false, length = 50)
    @Comment("변환전단위명")
    private String convertPreviousUnitNm;

    @Column(name = "conv_aftr_unit_nm", nullable = false, length = 50)
    @Comment("변환후단위명")
    private String convertAfterUnitNm;

    @Column(name = "conv_coef", precision = 12, scale = 6, columnDefinition = "decimal(12,6)")
    @Comment("변환계수")
    private BigDecimal convertCoef;

    @Column(name = "calc_fmla_svc_nm", length = 50)
    @Comment("계산공식서비스명")
    private String calculateFormulaServiceNm;

    @Column(name = "unit_conv_expn", length = 50)
    @Comment("단위변환설명")
    private String unitConvertExpn;

    @Column(name = "use_yn", length = 1, columnDefinition = "char(1)")
    @Comment("사용여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isUsed;

    @Builder
    public UnitConvert(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId, Boolean isDeleted,
            Integer unitConvNo, String unitConvNm, String convertTargetCategoryNm, String convertTargetDetailTypeNm, String convertPreviousUnitNm,
            String convertAfterUnitNm, BigDecimal convertCoef, String calculateFormulaServiceNm, String unitConvertExpn, Boolean isUsed) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.unitConvNo = unitConvNo;
        this.unitConvNm = unitConvNm;
        this.convertTargetCategoryNm = convertTargetCategoryNm;
        this.convertTargetDetailTypeNm = convertTargetDetailTypeNm;
        this.convertPreviousUnitNm = convertPreviousUnitNm;
        this.convertAfterUnitNm = convertAfterUnitNm;
        this.convertCoef = convertCoef;
        this.calculateFormulaServiceNm = calculateFormulaServiceNm;
        this.unitConvertExpn = unitConvertExpn;
        this.isUsed = isUsed;
    }

    public static UnitConvert of(BigDecimal convertCoef) {
        return UnitConvert.builder()
                .convertCoef(convertCoef)
                .build();
    }

    public void softDelete() {
        super.softDelete();
    }

}
