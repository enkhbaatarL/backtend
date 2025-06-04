package com.sk.domain.mi.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.response.signalMap.SignalMapDataRes;
import com.sk.domain.mi.dto.response.signalMap.SignalMapPeriodRes;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSetting;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingDetailRepository;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingRepository;
import com.sk.domain.shared.repository.master.tmm.CorrelationAnalysisResultRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiSignalMapService {
    private final EntityManager em;
    private final ChartPredictSettingRepository chartPredictSettingRepository;
    private final ChartPredictSettingDetailRepository chartPredictSettingDetailRepository;
    private final CorrelationAnalysisResultRepository correlationAnalysisResultRepository;

    public List<SignalMapPeriodRes> getSignalMapPeriodType(int companyNo) {
        return correlationAnalysisResultRepository.findPeriosdByCustomerCompanyNo(companyNo);
    }

    public List<String> getSignalMapFilterType(int companyNo) {
        return chartPredictSettingDetailRepository.findFilterTypeByCompanyNo(companyNo);
    }

    public String getTitle(int companyNo) {
        ChartPredictSetting chartPredictSetting = chartPredictSettingRepository.findByCustomerCompanyNo(companyNo)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "customer company no: %d".formatted(companyNo)));

        return chartPredictSetting.getPredictTargetSettingNm();
    }

    public String getLatestUpdatedDt(int companyNo) {
        return correlationAnalysisResultRepository.findLatestUpdatedDt(companyNo)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "company no: %d".formatted(companyNo)));
    }

    /**
     * 지정된 companyNo와 period(주기)에 맞춰 tmm_corranlyrsltd 데이터를 집계하여 SignalMapDataRes 형태로 조회합니다.
     * 
     * @param companyNo 고객사 번호
     * @param period    주기 코드(예: "3M", "6M", "9M", "1Y", "TOT")
     * @param filters   (현재는 사용되지 않지만, 추후 WHERE 절에 추가 조건이 필요할 때 사용할 수 있습니다)
     * @return SignalMapDataRes 리스트
     */
    public SignalMapDataRes getSignalMap(int companyNo, String period, String filters) {

        // 1) SELECT 절: 최종 결과에 포함할 컬럼과 SUM(…) 집계 컬럼을 정의
        String select = """
                SELECT
                    IA.instm_scrn_ctgr_nm               AS instmScrnCtgrNm,
                    IA.instm_scrn_dtl_ctgr_nm           AS instmScrnDtlCtgrNm,
                    IA.corr_calc_cyc_cd                 AS corrCalcCycCd,
                    IA.anly_fulf_dt                     AS anlyFulfDt,
                    IA.instm_nm                         AS instmNm,
                    IA.mnmnc_cd                         AS mnmncCd,
                    SUM(IA.corr_calc_val)               AS corrCalcVal
                """;

        // 2) FROM 절: 내부 서브쿼리에서 필요한 조인과 필터를 정의
        String from = """
                FROM (
                    SELECT
                        a.anly_fulf_dt,
                        a.prdc_rslt_dt,
                        a.corr_calc_cyc_cd,
                        a.corr_calc_val,
                        a.mnmnc_cd,
                        a.instm_nm,
                        c.instm_scrn_ctgr_nm,
                        c.instm_scrn_dtl_ctgr_nm,
                        c.instm_data_spt_cd
                    FROM
                        midb.tmm_corranlyrsltd a,
                        midb.tmm_mrktidxdatam b,
                        midb.tmm_mrktidxdatad c
                    WHERE
                        a.mrkt_idx_anly_tgt_cd = (select tc.mrkt_idx_anly_tgt_cd from midb.tmd_custcpnym tc where tc.cust_cpny_no = :companyNo)
                        AND a.mrkt_idx_anly_tgt_cd = c.mrkt_idx_anly_tgt_cd
                        AND a.anly_fulf_dt = (select max(tc.anly_fulf_dt) from midb.tmm_corranlyrsltd tc)
                        AND b.mrkt_idx_data_no = c.mrkt_idx_data_no
                        AND a.del_yn = b.del_yn
                        AND a.del_yn = c.del_yn
                        AND a.del_yn = 'N'
                        AND c.data_mdl_use_yn = 'Y'
                        AND (b.ric_cd = a.mnmnc_cd OR b.mnmnc_cd = a.mnmnc_cd)
                ) IA
                """;

        // 3) OUTER WHERE 절: 서브쿼리 결과를 다시 한번 주기별로 필터
        String where = """
                WHERE
                    IA.corr_calc_cyc_cd = :period
                """;

        if (StringUtils.hasText(filters)) {
            where += " ";
        }

        // 4) GROUP BY 절: 집계( SUM )를 위해 묶을 컬럼들
        String groupBy = """
                GROUP BY
                    IA.instm_scrn_ctgr_nm,
                    IA.instm_scrn_dtl_ctgr_nm,
                    IA.corr_calc_cyc_cd,
                    IA.mnmnc_cd
                """;

        // 5) ORDER BY 절: 결과 정렬 기준
        String orderBy = """
                ORDER BY
                    IA.instm_scrn_ctgr_nm      ASC,
                    IA.instm_scrn_dtl_ctgr_nm  ASC,
                    IA.corr_calc_cyc_cd        DESC
                """;

        // 6) 위의 구성요소를 모두 합쳐서 최종 SQL을 생성
        String sql = select
                + "\n" + from
                + "\n" + where
                + "\n" + groupBy
                + "\n" + orderBy;

        // 7) 네이티브 쿼리 생성
        Query nativeQuery = em.createNativeQuery(sql);

        // 파라미터 바인딩
        nativeQuery.setParameter("companyNo", companyNo);
        nativeQuery.setParameter("period", period);
        // nativeQuery.setParameter("filters", filters);

        // 8) 실행 후 결과를 Object[] 형태로 가져옴
        @SuppressWarnings("unchecked")
        List<Object[]> rows = nativeQuery.getResultList();

        // 9) Object[] 결과를 SignalMapDataRes DTO로 변환
        List<SignalMapDataRes> result = new ArrayList<>();
        for (Object[] row : rows) {
            String instmScrnCtgrNm = (row[0] == null ? null : row[0].toString());
            String instmScrnDtlCtgrNm = (row[1] == null ? null : row[1].toString());
            String corrCalcCycCd = (row[2] == null ? null : row[2].toString());
            String anlyFulfDt = (row[3] == null ? null : row[3].toString());
            String instmNm = (row[4] == null ? null : row[4].toString());
            String mnmncCd = (row[5] == null ? null : row[5].toString());
            BigDecimal corrCalcVal = (row[6] == null ? BigDecimal.ZERO : (BigDecimal) row[6]);

            System.out.println("instmScrnCtgrNm: " + instmScrnCtgrNm);
            System.out.println("instmScrnDtlCtgrNm: " + instmScrnDtlCtgrNm);
            System.out.println("corrCalcCycCd: " + corrCalcCycCd);
            System.out.println("anlyFulfDt: " + anlyFulfDt);
            System.out.println("instmNm: " + instmNm);
            System.out.println("mnmncCd: " + mnmncCd);
            System.out.println("corrCalcVal: " + corrCalcVal);
            System.out.println("-----------------------------------");

            // SignalMapDataRes dto = new SignalMapDataRes(
            // instmScrnCtgrNm,
            // instmScrnDtlCtgrNm,
            // corrCalcCycCd,
            // anlyFulfDt,
            // instmNm,
            // mnmncCd,
            // corrCalcVal);
            // result.add(dto);
        }

        // return result;

        // SELECT IA.instm_scrn_ctgr_nm
        // , IA.instm_scrn_dtl_ctgr_nm
        // , IA.corr_calc_cyc_cd
        // , IA.anly_fulf_dt
        // , IA.instm_nm
        // , IA.mnmnc_cd
        // , SUM(IA.corr_calc_val)
        // FROM (
        // SELECT a.anly_fulf_dt
        // , a.prdc_rslt_dt
        // , a.corr_calc_cyc_cd
        // , a.corr_calc_val
        // , a.mnmnc_cd
        // , a.instm_nm
        // , c.instm_scrn_ctgr_nm
        // , c.instm_scrn_dtl_ctgr_nm
        // , c.instm_data_spt_cd
        // FROM midb.tmm_corranlyrsltd a
        // , midb.tmm_mrktidxdatam b
        // , midb.tmm_mrktidxdatad c
        // WHERE a.mrkt_idx_anly_tgt_cd = (select tc.mrkt_idx_anly_tgt_cd from midb.tmd_custcpnym tc where tc.cust_cpny_no = 2013)
        // AND a.mrkt_idx_anly_tgt_cd = c.mrkt_idx_anly_tgt_cd
        // AND a.anly_fulf_dt = '20250511'
        // AND b.mrkt_idx_data_no = c.mrkt_idx_data_no
        // AND a.del_yn = b.del_yn
        // AND a.del_yn = c.del_yn
        // AND a.del_yn = 'N'
        // AND c.data_mdl_use_yn = 'Y'
        // AND (b.ric_cd = a.mnmnc_cd OR b.mnmnc_cd = a.mnmnc_cd)
        // ) IA
        // WHERE IA.corr_calc_cyc_cd = :period
        // GROUP BY IA.instm_scrn_ctgr_nm
        // , IA.instm_scrn_dtl_ctgr_nm
        // , IA.corr_calc_cyc_cd
        // , IA.mnmnc_cd
        // ORDER BY IA.instm_scrn_ctgr_nm ASC
        // , IA.instm_scrn_dtl_ctgr_nm ASC
        // , IA.corr_calc_cyc_cd DESC;

        return null;
    }

}
