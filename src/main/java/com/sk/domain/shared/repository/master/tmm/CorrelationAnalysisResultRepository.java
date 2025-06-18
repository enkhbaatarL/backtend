package com.sk.domain.shared.repository.master.tmm;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.mi.dto.response.signalMap.SignalMapPeriodRes;
import com.sk.domain.mi.dto.response.signalMap.SignalMapProjection;
import com.sk.domain.shared.entity.compositeKey.tmm.CorrelationAnalysisResultId;
import com.sk.domain.shared.entity.master.tmm.CorrelationAnalysisResult;

public interface CorrelationAnalysisResultRepository extends JpaRepository<CorrelationAnalysisResult, CorrelationAnalysisResultId> {

    @Query(value = """
            SELECT MAX(tcar.id.analysisFulfilmentDt)
            FROM CorrelationAnalysisResult tcar
            WHERE tcar.id.marketIndexAnalysisTargetCd = (SELECT tc.marketIndexAnalysisTargetCd
                                               FROM CustomerCompany tc
                                               WHERE tc.customerCompanyNo = :companyNo)
            """)
    Optional<String> findLatestUpdatedDt(@Param("companyNo") Integer companyNo);

    @Query(value = """
            SELECT DISTINCT new com.sk.domain.mi.dto.response.signalMap.SignalMapPeriodRes(tcar.id.correlationCalculationCycleCd, tcd.codeNm)
            FROM CorrelationAnalysisResult tcar
            LEFT JOIN CommonCodeDetail tcd ON tcd.id.codeId = tcar.id.correlationCalculationCycleCd
            WHERE tcar.id.marketIndexAnalysisTargetCd = (SELECT a.marketIndexAnalysisTargetCd
                                                          FROM CustomerCompany a
                                                          WHERE a.customerCompanyNo = :companyNo)
            ORDER BY tcd.codeRnk ASC
            """)
    List<SignalMapPeriodRes> findPeriosdByCompanyNo(@Param("companyNo") Integer companyNo);

    @Query(value = """
            SELECT
                IA.instm_scrn_ctgr_nm               AS instmScrnCtgrNm,
                IA.instm_scrn_dtl_ctgr_nm           AS instmScrnDtlCtgrNm,
                IA.corr_calc_cyc_cd                 AS corrCalcCycCd,
                IA.anly_fulf_dt                     AS anlyFulfDt,
                IA.instm_nm                         AS instmNm,
                IA.prdc_inpt_mnmnc_cd              AS prdc_inpt_mnmnc_cd,
                SUM(IA.corr_calc_val)               AS corrCalcVal
            FROM (
                SELECT a.anly_fulf_dt
                     , a.prdc_rslt_dt
                     , a.corr_calc_cyc_cd
                     , a.corr_calc_val
                     , a.prdc_inpt_mnmnc_cd
                     , a.instm_nm
                     , c.instm_scrn_ctgr_nm
                     , c.instm_scrn_dtl_ctgr_nm
                     , c.instm_data_spt_cd
                FROM midb.tmm_corranlyrsltd a
                   , midb.tmm_mrktidxdatam b
                   , midb.tmm_mrktidxdatad c
                WHERE a.mrkt_idx_anly_tgt_cd = (select tc.mrkt_idx_anly_tgt_cd from midb.tmd_custcpnym tc where tc.cust_cpny_no = :companyNo)
                    AND a.mrkt_idx_anly_tgt_cd = c.mrkt_idx_anly_tgt_cd
                    AND a.anly_fulf_dt = (select max(tc.anly_fulf_dt) from midb.tmm_corranlyrsltd tc where tc.mrkt_idx_anly_tgt_cd = a.mrkt_idx_anly_tgt_cd)
                    AND (:filter IS NULL OR a.mnmnc_cd = :filter)
                    AND b.mrkt_idx_data_no = c.mrkt_idx_data_no
                    AND a.del_yn = b.del_yn
                    AND a.del_yn = c.del_yn
                    AND a.del_yn = 'N'
                    AND c.data_mdl_use_yn = 'Y'
                    AND (b.ric_cd = a.prdc_inpt_mnmnc_cd OR b.mnmnc_cd = a.prdc_inpt_mnmnc_cd)
            ) IA
            WHERE IA.corr_calc_cyc_cd = :period
            GROUP BY
                IA.instm_scrn_ctgr_nm,
                IA.instm_scrn_dtl_ctgr_nm,
                IA.corr_calc_cyc_cd,
                IA.prdc_inpt_mnmnc_cd
            ORDER BY
                IA.instm_scrn_ctgr_nm ASC,
                IA.instm_scrn_dtl_ctgr_nm ASC,
                IA.corr_calc_cyc_cd DESC
            """, nativeQuery = true)
    List<SignalMapProjection> findSignalMapData(
            @Param("companyNo") int companyNo,
            @Param("period") String period,
            @Param("filter") String filter);
}
