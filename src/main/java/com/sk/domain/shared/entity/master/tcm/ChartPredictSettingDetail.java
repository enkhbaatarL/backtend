package com.sk.domain.shared.entity.master.tcm;

import org.hibernate.annotations.Comment;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.chart.ChartPredictLineType;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tcm_michrtprdcsetd")
@Comment("TCM_MI차트예측설정상세")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChartPredictSettingDetail extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chrt_prdc_set_dtl_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("차트예측설정상세번호")
    private Integer chartPredictSettingDetailNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chrt_prdc_set_no", referencedColumnName = "chrt_prdc_set_no", nullable = false)
    @Comment("차트예측설정번호")
    private ChartPredictSetting chartPredictSetting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gds_data_src_no", referencedColumnName = "gds_data_src_no", nullable = false)
    @Comment("상품데이터소스번호")
    private GoodsDataSourceDetail goodsDataSourceDetail;

    @Column(name = "prdc_chrt_dtl_expn", length = 100)
    @Comment("예측차트상세설명")
    private String predictChartDetailExpn;

    @Column(name = "chrt_line_prdc_val_yn", nullable = false, columnDefinition = "char(1)")
    @Comment("차트선예측값여부")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isChartLinePredictVal;

    @Enumerated(EnumType.STRING)
    @Column(name = "chrt_prdc_line_spt_cd", length = 10)
    @Comment("차트예측선구분코드")
    private ChartPredictLineType chartPredictLineSeparateCd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asso_gds_data_src_no", referencedColumnName = "gds_data_src_no")
    @Comment("연관상품데이터소스번호")
    private GoodsDataSourceDetail associatedGoodsDataSourceDetail;

    @Column(name = "futr_yn", nullable = false, columnDefinition = "char(1)")
    @Comment("선물여부")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isFuture;
}
