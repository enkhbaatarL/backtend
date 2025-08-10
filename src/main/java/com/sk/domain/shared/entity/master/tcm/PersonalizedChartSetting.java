package com.sk.domain.shared.entity.master.tcm;

import com.sk.domain.mi.dto.request.ChartsPersonalizeReq;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.chart.ChartSettingConditionCd;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Table(name = "tcm_psnlzchrtsetl")
@Comment("TCM_개인화차트설정내역")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonalizedChartSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "psnlz_chrt_set_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("개인화차트설정번호")
    private Integer personalizedChartSettingNo;

    @Column(name = "usr_logn_id", nullable = false, length = 50)
    @Comment("사용자로그인ID")
    private String userLoginId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gds_data_src_no", referencedColumnName = "gds_data_src_no", nullable = false)
    @Comment("상품데이터소스번호")
    private GoodsDataSourceDetail goodsDataSourceDetail;

    @Enumerated(EnumType.STRING)
    @Column(name = "chrt_set_cdtn_spt_cd", nullable = false, length = 20)
    @Comment("차트설정조건구분코드")
    private ChartSettingConditionCd chartSettingConditionCd;

    @Column(name = "chrt_set_cdtn_val", nullable = false, length = 20)
    @Comment("차트설정조건값")
    private String chartSettingConditionVal;

    @Builder
    public PersonalizedChartSetting(String userLoginId, GoodsDataSourceDetail goodsDataSourceDetail, ChartSettingConditionCd chartSettingConditionCd, String chartSettingConditionVal) {
        this.userLoginId = userLoginId;
        this.goodsDataSourceDetail = goodsDataSourceDetail;
        this.chartSettingConditionCd = chartSettingConditionCd;
        this.chartSettingConditionVal = chartSettingConditionVal;
    }

    public static PersonalizedChartSetting of(ChartsPersonalizeReq req, GoodsDataSourceDetail goodsDataSourceDetail, String userLoginId) {
        return PersonalizedChartSetting.builder()
            .chartSettingConditionCd(req.chartConditionCd())
            .chartSettingConditionVal(req.chartConditionVal())
            .goodsDataSourceDetail(goodsDataSourceDetail)
            .userLoginId(userLoginId)
            .build();
    }

    public void update(ChartsPersonalizeReq req) {
        this.chartSettingConditionVal = req.chartConditionVal();
    }

}
