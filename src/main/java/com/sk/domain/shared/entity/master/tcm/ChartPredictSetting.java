package com.sk.domain.shared.entity.master.tcm;

import org.hibernate.annotations.Comment;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.enums.PeriodUnitSeparateCd;

import jakarta.persistence.Column;
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

@Getter
@Entity
@Table(name = "tcm_michrtprdcsetm")
@Comment("TCM_MI차트예측설정기본")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChartPredictSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chrt_prdc_set_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("차트예측설정번호")
    private Integer chartPredictSettingNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cust_cpny_no", nullable = false)
    @Comment("고객회사번호")
    private CustomerCompany customerCompany;

    @Column(name = "prdc_tgt_set_nm", nullable = false, length = 255)
    @Comment("예측대상설정명")
    private String predictTargetSettingNm;

    @Column(name = "prdc_perd_mths_cnt", nullable = false, columnDefinition = "mediumint unsigned")
    @Comment("예측기간개월수")
    private Integer predictPeriodMonthsCnt;

    @Enumerated(EnumType.STRING)
    @Column(name = "prdc_perd_unit_spt_cd", length = 2)
    @Comment("예측기간단위구분코드")
    private PeriodUnitSeparateCd predictPeriodUnitSeparateCd;

    @Column(name = "prdc_chic_day", columnDefinition = "tinyint unsigned")
    @Comment("예측선택일")
    private Integer predictChoiceDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "scop_perd_unit_spt_cd", length = 2)
    @Comment("범위기간단위구분코드")
    private PeriodUnitSeparateCd scopePeriodUnitSeparateCd;

    @Enumerated(EnumType.STRING)
    @Column(name = "defl_perd_unit_spt_cd", length = 2)
    @Comment("기본기간단위구분코드")
    private PeriodUnitSeparateCd defaultPeriodUnitSeparateCd;

    @Column(name = "prdc_tgt_repr_spt_nm", length = 255)
    @Comment("예측대상대표품명")
    private String predictTargetRepresentSpecNm;
}
