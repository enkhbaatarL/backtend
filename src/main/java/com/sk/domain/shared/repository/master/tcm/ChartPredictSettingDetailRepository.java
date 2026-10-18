package com.sk.domain.shared.repository.master.tcm;

import java.util.List;
import java.util.Optional;

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
            SELECT REPLACE(REPLACE(REGEXP_SUBSTR(
                        tgds.qry_cdtn_cntt,
                        'code\\ *=\\s*''([^'']*)''',
                        1, 1, ''
                        ), 'code=', ''), '''', '') as code,
                        tmd.prdc_chrt_dtl_expn
            FROM midb.tcm_michrtprdcsetm tm
            JOIN midb.tcm_michrtprdcsetd tmd ON tmd.chrt_prdc_set_no = tm.chrt_prdc_set_no
            JOIN midb.tmd_gdsdatasrcd tgds ON tgds.gds_data_src_no = tmd.gds_data_src_no
            WHERE
                tm.cust_cpny_no = :companyNo
                AND tm.del_yn = 'N'
                AND tmd.del_yn = 'N'
                AND tmd.chrt_line_prdc_val_yn = 'N'
                AND tmd.futr_yn = 'N'
                AND tgds.del_yn = 'N'
                AND tgds.gds_data_use_mnagt_cd = 'backend'
        """, nativeQuery = true)
    List<Object[]> findFilterTypeByCompanyNo(@Param("companyNo") Integer companyNo);

    @EntityGraph(attributePaths = {"goodsDataSourceDetail"})
    List<ChartPredictSettingDetail> findByChartPredictSettingAndIsDeletedFalse(ChartPredictSetting chartPredictSetting);

    @EntityGraph(attributePaths = {"chartPredictSetting"})
    Optional<ChartPredictSettingDetail> findByChartPredictSettingDetailNoAndIsDeletedFalse(Integer settingDetailNo);

}
