package com.sk.domain.mi.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.response.signalMap.ScreenCategoryDto;
import com.sk.domain.mi.dto.response.signalMap.ScreenDetailCategoryDto;
import com.sk.domain.mi.dto.response.signalMap.SignalMapDataCache;
import com.sk.domain.mi.dto.response.signalMap.SignalMapDataDto;
import com.sk.domain.mi.dto.response.signalMap.SignalMapFilterRes;
import com.sk.domain.mi.dto.response.signalMap.SignalMapPeriodRes;
import com.sk.domain.mi.dto.response.signalMap.SignalMapProjection;
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

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public List<SignalMapPeriodRes> getSignalMapPeriodType(int companyNo) {
        return correlationAnalysisResultRepository.findPeriosdByCompanyNo(companyNo);
    }

    public List<SignalMapFilterRes> getSignalMapFilterType(int companyNo) {
        List<Object[]> filterTypes = chartPredictSettingDetailRepository.findFilterTypeByCompanyNo(companyNo);

        List<SignalMapFilterRes> result = filterTypes.stream()
                .map(obj -> new SignalMapFilterRes(
                        (String) obj[0],
                        (String) obj[1]))
                .collect(Collectors.toList());

        return result;
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

        String cacheKey = "signalMapData::companyNo:" + companyNo + "::period:" + period + "::filter:" + filter;
        String cachedData = redisTemplate.opsForValue().get(cacheKey);
        if (cachedData != null) {
            try {
                SignalMapDataCache signalMapDataCache = objectMapper.readValue(cachedData, SignalMapDataCache.class);
                if (signalMapDataCache.data() != null) {
                    return signalMapDataCache.data();
                }
            } catch (Exception e) {
                log.error("Failed to parse cached data for key: {}", cacheKey, e);
            }
        }

        List<SignalMapProjection> rawList = correlationAnalysisResultRepository.findSignalMapData(companyNo, period, filter);

        Map<String, Map<String, List<SignalMapProjection>>> grouped = rawList.stream()
                .collect(Collectors.groupingBy(
                        SignalMapProjection::getInstmScrnCtgrNm,
                        Collectors.groupingBy(SignalMapProjection::getInstmScrnDtlCtgrNm)));

        List<ScreenCategoryDto> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<SignalMapProjection>>> screenCategoryEntry : grouped.entrySet()) {
            String screenCategoryName = screenCategoryEntry.getKey();
            Map<String, List<SignalMapProjection>> screenDetailCategoryMap = screenCategoryEntry.getValue();

            List<ScreenDetailCategoryDto> screenDetailCategoryDtoList = screenDetailCategoryMap.entrySet().stream()
                    .map(detailEntry -> {
                        String screenDetailCategoryName = detailEntry.getKey();
                        List<SignalMapProjection> projList = detailEntry.getValue();

                        List<SignalMapDataDto> itemDtoList = projList.stream()
                                .map(proj -> new SignalMapDataDto(
                                        proj.getInstmNm(),
                                        proj.getCorrCalcVal()))
                                .collect(Collectors.toList());

                        // 20개 초과 시 잘라냄
                        if (itemDtoList.size() > 20) {
                            itemDtoList.subList(20, itemDtoList.size()).clear();
                        }

                        return new ScreenDetailCategoryDto(screenDetailCategoryName, itemDtoList);
                    })
                    .collect(Collectors.toList());

            result.add(new ScreenCategoryDto(screenCategoryName, screenDetailCategoryDtoList));
        }

        try {
            String json = objectMapper.writeValueAsString(SignalMapDataCache.of(result));
            redisTemplate.opsForValue().set(cacheKey, json, Duration.ofSeconds(1)); // todo: 캐시 유효기간 1시간 설정, 기본값: 3600
        } catch (Exception e) {
            log.error("Error processing signal map data", e);
        }

        return result;
    }
}
