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
            SELECT DISTINCT new com.sk.domain.mi.dto.response.signalMap.SignalMapPeriodRes(tcar.id.correlationCalculationCycleCd, tcd.codeNm, tcd.codeRnk)
            FROM CorrelationAnalysisResult tcar
            LEFT JOIN CommonCodeDetail tcd ON tcd.id.codeId = tcar.id.correlationCalculationCycleCd
            WHERE tcar.id.marketIndexAnalysisTargetCd = (SELECT a.marketIndexAnalysisTargetCd
                                                          FROM CustomerCompany a
                                                          WHERE a.customerCompanyNo = :companyNo)
            ORDER BY tcd.codeRnk ASC
            """)
    List<SignalMapPeriodRes> findPeriosdByCompanyNo(@Param("companyNo") Integer companyNo);

  @Query(
      value =
          """
            WITH params AS (
                 SELECT tc.mrkt_idx_anly_tgt_cd AS tgt
                 FROM midb.tmd_custcpnym tc
                 WHERE tc.cust_cpny_no = :companyNo
             ),
             latest AS (
                 SELECT a.anly_fulf_dt
                 FROM midb.tmm_corranlyrsltd as a
                 JOIN params p ON p.tgt = a.mrkt_idx_anly_tgt_cd
                 WHERE a.del_yn = 'N'
                 ORDER BY a.anly_fulf_dt DESC
                 LIMIT 1
             ),
             bx AS (SELECT mrkt_idx_data_no, ric_cd AS code
                    FROM midb.tmm_mrktidxdatam
                    WHERE del_yn = 'N'
                    UNION ALL
                    SELECT mrkt_idx_data_no, mnmnc_cd AS code
                    FROM midb.tmm_mrktidxdatam
                    WHERE del_yn = 'N')
             SELECT
                IFNULL(c.instm_scrn_ctgr_nm, '')     AS instmScrnCtgrNm,
                IFNULL(c.instm_scrn_dtl_ctgr_nm, '') AS instmScrnDtlCtgrNm,
                a.corr_calc_cyc_cd                   AS corrCalcCycCd,
                a.anly_fulf_dt                       AS anlyFulfDt,
                a.instm_nm                           AS instmNm,
                a.prdc_inpt_mnmnc_cd                 AS prdc_inpt_mnmnc_cd,
                SUM(a.corr_calc_val)                 AS corrCalcVal
             FROM midb.tmm_corranlyrsltd as a
                 JOIN params p ON a.mrkt_idx_anly_tgt_cd = p.tgt
                 JOIN latest l ON a.anly_fulf_dt = l.anly_fulf_dt
                 JOIN bx bx ON bx.code = a.prdc_inpt_mnmnc_cd
                 JOIN midb.tmm_mrktidxdatad c ON c.mrkt_idx_data_no = bx.mrkt_idx_data_no AND c.mrkt_idx_anly_tgt_cd = p.tgt AND c.data_mdl_use_yn = 'Y'
             WHERE a.del_yn = 'N'
                 AND c.del_yn = 'N'
                 AND a.corr_calc_cyc_cd = :period
                 AND a.mnmnc_cd = :filter
             GROUP BY c.instm_scrn_ctgr_nm, c.instm_scrn_dtl_ctgr_nm, a.corr_calc_cyc_cd, a.prdc_inpt_mnmnc_cd
             ORDER BY c.instm_scrn_ctgr_nm ASC, c.instm_scrn_dtl_ctgr_nm ASC, ABS(SUM(a.corr_calc_val)) DESC
            """,
      nativeQuery = true)
  List<SignalMapProjection> findSignalMapData(
      @Param("companyNo") int companyNo,
      @Param("period") String period,
      @Param("filter") String filter);
}
