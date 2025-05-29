package com.sk.domain.mi.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.response.signalMap.SignalMapDataRes;
import com.sk.domain.mi.dto.response.signalMap.SignalMapPeriodRes;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSetting;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingDetailRepository;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingRepository;
import com.sk.domain.shared.repository.master.tmm.CorrelationAnalysisResultRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiSignalMapService {
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

    public SignalMapDataRes getSignalMap(int companyNo, String period, String filters) {

        return null;
    }

}
