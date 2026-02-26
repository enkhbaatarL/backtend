package com.sk.domain.mi.service.forecast;

import com.sk.domain.shared.entity.master.tcm.UiComponentSetting;
import com.sk.domain.shared.repository.master.tcm.UiComponentSettingRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ForecastDecimalScalePolicy {

    private static final String FORECAST_CHART_SCREEN_CODE = "MI_FORECAST";
    private static final String FORECAST_CHART_COMPONENT_TYPE = "COMMON";
    private static final String FORECAST_CHART_COMPONENT_CODE = "COMMON";
    private static final String DECIMAL_SCALE_ITEM_KEY = "decimalScale";
    private static final int DEFAULT_SCALE = 2;
    private static final int MIN_SCALE = 0;
    private static final int MAX_SCALE = 6;

    private final UiComponentSettingRepository uiComponentSettingRepository;

    public int resolveScale(int companyNo) {
        Optional<UiComponentSetting> setting = findSetting(companyNo);
        if (setting.isEmpty()) {
            return DEFAULT_SCALE;
        }

        String value = setting.get().getSettingValue();
        if (value == null || value.isBlank()) {
            return DEFAULT_SCALE;
        }

        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < MIN_SCALE || parsed > MAX_SCALE) {
                log.warn("decimalScale out of range. companyNo={}, value={}", companyNo, value);
                return DEFAULT_SCALE;
            }
            return parsed;
        } catch (NumberFormatException e) {
            log.warn("invalid decimalScale value. companyNo={}, value={}", companyNo, value);
            return DEFAULT_SCALE;
        }
    }

    private Optional<UiComponentSetting> findSetting(int companyNo) {
        return uiComponentSettingRepository
            .findFirstByCustomerCompany_CustomerCompanyNoAndScreenCodeAndComponentTypeCodeAndComponentCodeAndItemKeyAndIsDeletedFalse(
                companyNo,
                FORECAST_CHART_SCREEN_CODE,
                FORECAST_CHART_COMPONENT_TYPE,
                FORECAST_CHART_COMPONENT_CODE,
                DECIMAL_SCALE_ITEM_KEY
            );
    }
}
