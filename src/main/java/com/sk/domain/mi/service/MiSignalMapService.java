package com.sk.domain.mi.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.response.signalMap.SignalMapDataRes;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSetting;
import com.sk.domain.shared.enums.signalMap.signalMapPeriodType;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiSignalMapService {
    private final ChartPredictSettingRepository chartPredictSettingRepository;

    public String getTitle(int companyNo) {
        ChartPredictSetting chartPredictSetting = chartPredictSettingRepository.findByCustomerCompanyNo(companyNo)
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "customer company no: %d".formatted(companyNo)));

        return chartPredictSetting.getPredictTargetSettingNm();
    }

    public SignalMapDataRes getSignalMap(int companyNo, signalMapPeriodType period, String filters) {

        return null;
    }

}
