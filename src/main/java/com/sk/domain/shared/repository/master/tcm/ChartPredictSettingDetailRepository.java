package com.sk.domain.shared.repository.master.tcm;

import java.util.List;

import com.sk.domain.shared.entity.master.tcm.ChartPredictSetting;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.shared.entity.master.tcm.ChartPredictSettingDetail;
import com.sk.domain.shared.repository.master.tcm.custom.CustomChartPredictSettingDetailRepository;

public interface ChartPredictSettingDetailRepository
        extends JpaRepository<ChartPredictSettingDetail, Integer>, CustomChartPredictSettingDetailRepository {

    @Query(value = """
                SELECT cpsd.predictChartDetailExplanation
                FROM ChartPredictSettingDetail cpsd
                WHERE cpsd.chartPredictSetting.customerCompany.customerCompanyNo = :companyNo
                    AND cpsd.isDeleted = false
                    AND cpsd.goodsDataSourceDetail.isDeleted = false
                    AND cpsd.isChartLinePredictValue = false
                    AND cpsd.isFuture = false
            """)
    List<String> findFilterTypeByCompanyNo(@Param("companyNo") Integer companyNo);

    @EntityGraph(attributePaths = {"goodsDataSourceDetail"})
    List<ChartPredictSettingDetail> findByChartPredictSettingAndIsDeletedFalse(ChartPredictSetting chartPredictSetting);

}
