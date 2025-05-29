package com.sk.domain.shared.repository.master.tmm;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.mi.dto.response.signalMap.SignalMapPeriodRes;
import com.sk.domain.shared.entity.compositeKey.tmm.CorrelationAnalysisResultId;
import com.sk.domain.shared.entity.master.tmm.CorrelationAnalysisResult;

public interface CorrelationAnalysisResultRepository extends JpaRepository<CorrelationAnalysisResult, CorrelationAnalysisResultId> {

    // @Query(value = """
    // SELECT MAX(tcar.anly_fulf_dt)
    // FROM midb.tmm_corranlyrsltd tcar
    // WHERE tcar.mrkt_idx_anly_tgt_cd = (SELECT tc.mrkt_idx_anly_tgt_cd
    // FROM midb.tmd_custcpnym tc
    // WHERE tc.cust_cpny_no = :companyNo)
    // """, nativeQuery = true)
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
    List<SignalMapPeriodRes> findPeriosdByCustomerCompanyNo(@Param("companyNo") Integer companyNo);
    // select distinct tcar.corr_calc_cyc_cd, tcd.cd_nm
    // from midb.tmm_corranlyrsltd tcar
    // left join midb.tcm_commcdd tcd on tcd.cd_id = tcar.corr_calc_cyc_cd
    // where tcar.mrkt_idx_anly_tgt_cd =
    // (SELECT a.mrkt_idx_anly_tgt_cd
    // FROM midb.tmd_custcpnym a
    // WHERE a.cust_cpny_no = 2013)
    // order by tcd.cd_rnk asc;
}
