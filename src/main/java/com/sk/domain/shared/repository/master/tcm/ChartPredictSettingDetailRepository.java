package com.sk.domain.shared.repository.master.tcm;

import java.util.List;
import java.util.Optional;

import com.sk.domain.mi.dto.response.signalMap.SignalMapFilterRes;
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
                SELECT DISTINCT new com.sk.domain.mi.dto.response.signalMap.SignalMapFilterRes(
                car.id.mnemonicCd,
                ''
                )
                FROM CorrelationAnalysisResult car
                JOIN CustomerCompany cc ON cc.customerCompanyNo = :companyNo
                WHERE
                    car.id.marketIndexAnalysisTargetCd = cc.marketIndexAnalysisTargetCd
                    AND car.isDeleted = false
                    AND cc.isDeleted = false
                    AND cc.isUsed = true
                    AND cc.useBeginDt <= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
                    AND cc.useEndDt >= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
            """)
    List<SignalMapFilterRes> findFilterTypeByCompanyNo(@Param("companyNo") Integer companyNo);

    @EntityGraph(attributePaths = {"goodsDataSourceDetail"})
    List<ChartPredictSettingDetail> findByChartPredictSettingAndIsDeletedFalse(ChartPredictSetting chartPredictSetting);

    @EntityGraph(attributePaths = {"chartPredictSetting"})
    Optional<ChartPredictSettingDetail> findByChartPredictSettingDetailNoAndIsDeletedFalse(Integer settingDetailNo);

}
