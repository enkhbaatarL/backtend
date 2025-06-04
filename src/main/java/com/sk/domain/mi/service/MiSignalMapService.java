package com.sk.domain.mi.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.response.signalMap.ScreenCategoryDto;
import com.sk.domain.mi.dto.response.signalMap.ScreenDetailCategoryDto;
import com.sk.domain.mi.dto.response.signalMap.SignalMapDataDto;
import com.sk.domain.mi.dto.response.signalMap.SignalMapDataRes;
import com.sk.domain.mi.dto.response.signalMap.SignalMapPeriodRes;
import com.sk.domain.mi.dto.response.signalMap.SignalMapProjection;
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
     * @return List<ScreenCategoryDto>
     */
    public List<ScreenCategoryDto> getSignalMap(int companyNo, String period, String filter) {
        // ① 네이티브쿼리 실행 → Projection 인터페이스로 바로 결과를 가져옴
        List<SignalMapProjection> rawList = correlationAnalysisResultRepository.findSignalMapData(companyNo, period, filter);

        // ② 2단계 그룹핑: 1차(instmScrnCtgrNm) → 2차(instmScrnDtlCtgrNm)
        Map<String, Map<String, List<SignalMapProjection>>> grouped = rawList.stream()
                .collect(Collectors.groupingBy(
                        SignalMapProjection::getInstmScrnCtgrNm,
                        Collectors.groupingBy(SignalMapProjection::getInstmScrnDtlCtgrNm)));

        // ③ DTO 구조로 변환
        List<ScreenCategoryDto> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<SignalMapProjection>>> parentEntry : grouped.entrySet()) {
            String parentCategoryName = parentEntry.getKey();
            Map<String, List<SignalMapProjection>> detailMap = parentEntry.getValue();

            List<ScreenDetailCategoryDto> detailDtoList = detailMap.entrySet().stream()
                    .map(detailEntry -> {
                        String detailCategoryName = detailEntry.getKey();
                        List<SignalMapProjection> projList = detailEntry.getValue();

                        // 각 프로젝션에서 instmNm, corrCalcVal만 꺼내서 DTO로 변환
                        List<SignalMapDataDto> itemDtoList = projList.stream()
                                .map(proj -> new SignalMapDataDto(
                                        proj.getInstmNm(),
                                        proj.getCorrCalcVal()))
                                .collect(Collectors.toList());

                        return new ScreenDetailCategoryDto(detailCategoryName, itemDtoList);
                    })
                    .collect(Collectors.toList());

            result.add(new ScreenCategoryDto(parentCategoryName, detailDtoList));
        }
        return result;
    }

}
