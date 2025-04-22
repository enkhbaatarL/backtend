package com.sk.domain.mi.service.chart;

import com.sk.domain.shared.entity.master.tcm.PersonalizedChartSetting;
import com.sk.domain.shared.enums.ChartSettingConditionCd;
import com.sk.domain.shared.enums.chart.Interval;
import com.sk.domain.shared.repository.master.tcm.PersonalizedChartSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Service
@RequiredArgsConstructor
public class PersonalizationService {

    private final PersonalizedChartSettingRepository personalizedChartSettingRepository;

    public Object getPersonalizedChartSetting(String userLoginId) {

//        Map<ChartSettingConditionCd, List<String>> personalizedMap = personalizedCondition.stream()
//            .collect(groupingBy(
//                PersonalizedChartSetting::getChartSettingConditionCd,
//                Collectors.mapping(PersonalizedChartSetting::getChartSettingConditionVal, Collectors.toList())
//            ));

        return null;

    }

    public Interval getPersonalOrDefault() {
        return null;
    }




}
