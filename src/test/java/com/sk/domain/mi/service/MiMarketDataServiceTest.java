package com.sk.domain.mi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.exception.SystemErrorException;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.dto.response.marketData.charts.calculatePeriodChange.CalculatePeriodChangeValue;
import com.sk.domain.mi.service.custom.CustomRegistry;
import com.sk.domain.mi.service.period.DoDPeriodCalculator;
import com.sk.domain.mi.service.period.MoMPeriodCalculator;
import com.sk.domain.mi.service.period.PeriodCalculator;
import com.sk.domain.shared.enums.marketData.CalculatePeriodChangeType;
import com.sk.domain.shared.repository.master.tcm.CommonCodeDetailRepository;
import com.sk.domain.shared.repository.master.tcm.MarketDataChartSettingRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.DataSourceGroupMappingRepository;
import com.sk.domain.shared.repository.master.tmd.UserRepository;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

@DisplayName("MiMarketDataService 테스트")
class MiMarketDataServiceTest {

    @Test
    @DisplayName(
        "calculator의 null은 key를 생략하고 ofEmpty와 정상 값은 순서대로 유지한다")
    void keepNullAndOfEmptyPeriodKeyShape() {
        PeriodCalculator doD = periodCalculator(
            CalculatePeriodChangeType.DOD, null);
        PeriodCalculator moM = periodCalculator(
            CalculatePeriodChangeType.MOM, CalculatePeriodChangeValue.ofEmpty());
        PeriodCalculator yoY = periodCalculator(
            CalculatePeriodChangeType.YOY,
            CalculatePeriodChangeValue.of(10.0, 2.0));
        MiMarketDataService service = service(Map.of(
            "doD", doD,
            "moM", moM,
            "yoY", yoY));

        Map<CalculatePeriodChangeType, CalculatePeriodChangeValue> result =
            service.calculatePeriodChange(
                null,
                List.of(),
                "date",
                "value",
                linkedSet(
                    CalculatePeriodChangeType.DOD,
                    CalculatePeriodChangeType.MOM,
                    CalculatePeriodChangeType.YOY));

        assertThat(result).containsExactly(
            Map.entry(
                CalculatePeriodChangeType.MOM,
                CalculatePeriodChangeValue.ofEmpty()),
            Map.entry(
                CalculatePeriodChangeType.YOY,
                CalculatePeriodChangeValue.of(10.0, 2.0)));
    }

    @Test
    @DisplayName("actual calculator의 empty raw data null은 기존처럼 period key를 생략한다")
    void omitPeriodKeyForEmptyRawData() {
        MiMarketDataService service = service(Map.of(
            "doD", new DoDPeriodCalculator(),
            "moM", new MoMPeriodCalculator()));

        Map<CalculatePeriodChangeType, CalculatePeriodChangeValue> result =
            service.calculatePeriodChange(
                null,
                List.of(),
                "date",
                "value",
                linkedSet(
                    CalculatePeriodChangeType.DOD,
                    CalculatePeriodChangeType.MOM));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName(
        "actual calculator의 invalid 마지막 날짜 내부 오류를 service가 정상 응답으로 바꾸지 않는다")
    void propagateInvalidLastDateError() {
        MiMarketDataService service = service(Map.of(
            "doD", new DoDPeriodCalculator()));
        Map<String, Object> invalidRow = new HashMap<>();
        invalidRow.put("date", null);
        invalidRow.put("value", 10.0);

        assertThatThrownBy(() -> service.calculatePeriodChange(
            null,
            List.of(invalidRow),
            "date",
            "value",
            Set.of(CalculatePeriodChangeType.DOD)))
            .isInstanceOfSatisfying(SystemErrorException.class,
                exception -> assertThat(exception.getAdditionalMessage()).isNull());
    }

    private PeriodCalculator periodCalculator(
        CalculatePeriodChangeType type,
        CalculatePeriodChangeValue value) {
        PeriodCalculator calculator = mock(PeriodCalculator.class);
        given(calculator.getCalculatePeriodChangeType()).willReturn(type);
        given(calculator.calculatePeriodChange(null, List.of(), "date", "value"))
            .willReturn(value);
        return calculator;
    }

    private MiMarketDataService service(Map<String, PeriodCalculator> calculators) {
        MiMarketDataService service = new MiMarketDataService(
            mock(UserRepository.class),
            mock(EntityManager.class),
            mock(MiGoodsDataInquiryUserService.class),
            mock(MarketDataChartSettingRepository.class),
            mock(CommonCodeDetailRepository.class),
            mock(CustomerCompanyRepository.class),
            mock(MybatisDynamicQueryService.class),
            mock(DataSourceGroupMappingRepository.class),
            mock(AuthorizationService.class),
            mock(CustomRegistry.class),
            mock(MiMarketDynamicQueryService.class),
            mock(StringRedisTemplate.class),
            mock(ObjectMapper.class),
            mock(EntityManager.class),
            calculators);
        service.init();
        return service;
    }

    @SafeVarargs
    private final <T> LinkedHashSet<T> linkedSet(T... values) {
        LinkedHashSet<T> result = new LinkedHashSet<>();
        result.addAll(List.of(values));
        return result;
    }
}
