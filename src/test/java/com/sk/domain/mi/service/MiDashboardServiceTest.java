package com.sk.domain.mi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.config.database.DataSourceContextHolder;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.SystemErrorException;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.dto.response.dashboard.charts.graph.DashboardChartRawData;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSetting;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSettingDetail;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.PeriodUnitSeparateCd;
import com.sk.domain.shared.mapper.dynamic.DashboardMapper;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingDetailRepository;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataSourceDetailRepository;
import com.sk.domain.shared.service.FileService;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

@DisplayName("MiDashboardService 테스트")
@ExtendWith(MockitoExtension.class)
class MiDashboardServiceTest {

    @Mock
    private GoodsDataRepository goodsDataRepository;

    @Mock
    private ChartPredictSettingRepository chartPredictSettingRepository;

    @Mock
    private CustomerCompanyRepository customerCompanyRepository;

    @Mock
    private ChartPredictSettingDetailRepository chartPredictSettingDetailRepository;

    @Mock
    private MybatisDynamicQueryService mybatisDynamicQueryService;

    @Mock
    private GoodsDataSourceDetailRepository goodsDataSourceDetailRepository;

    @Mock
    private DashboardMapper dashboardMapper;

    @Mock
    private FileService fileService;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private MiDashboardService miDashboardService;

    @BeforeEach
    void setUp() {
        miDashboardService = new MiDashboardService(
                goodsDataRepository,
                chartPredictSettingRepository,
                customerCompanyRepository,
                chartPredictSettingDetailRepository,
                mybatisDynamicQueryService,
                goodsDataSourceDetailRepository,
                dashboardMapper,
                fileService,
                authorizationService,
                redisTemplate,
                objectMapper);
    }

    @AfterEach
    void tearDown() {
        DataSourceContextHolder.clearDataSourceKey();
    }

    @Test
    @DisplayName("predictDate가 없으면 예측 기간 조건 없이 조회한다")
    void getDashboardSummariesWithoutPredictDate() {
        assertThat(miDashboardService.getDashboardSummaries(List.of(), "20250511", null)).isEmpty();
    }

    @Test
    @DisplayName("202506은 2025년 6월 시작일과 종료일로 한 번 변환한다")
    void getDashboardSummariesWithStrictPredictMonth() {
        // given
        ChartPredictSettingDetail settingDetail = mock(ChartPredictSettingDetail.class);
        GoodsDataSourceDetail dataSourceDetail = mock(GoodsDataSourceDetail.class);
        given(settingDetail.getIsChartLinePredictVal()).willReturn(true);
        given(settingDetail.getGoodsDataSourceDetail()).willReturn(dataSourceDetail);
        given(dataSourceDetail.getTargetDbNm()).willReturn("market");
        given(dataSourceDetail.getQueryConditionCntt()).willReturn("query-condition");
        given(dashboardMapper.chartSummary(
                "query-condition", "20250511", "20250601", "20250630"))
                .willReturn(List.of());

        // when
        assertThat(miDashboardService.getDashboardSummaries(
                List.of(settingDetail), "20250511", "202506"))
                .isEmpty();

        // then
        then(dashboardMapper).should().chartSummary(
                "query-condition", "20250511", "20250601", "20250630");
    }

    @ParameterizedTest(name = "[{index}] predictDate={0}")
    @ValueSource(strings = {"", "x", "202613", "2026-06", "20250601", "202506suffix", " 202506"})
    @DisplayName("잘못된 predictDate는 NPE가 아닌 REQ_INVALID_PARAMETER로 처리한다")
    void rejectInvalidPredictDate(String predictDate) {
        assertThatThrownBy(() -> miDashboardService.getDashboardSummaries(
                List.of(), "20250511", predictDate))
                .isInstanceOfSatisfying(AppErrorException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ResultCode.Error.REQ_INVALID_PARAMETER);
                    assertThat(exception.getAdditionalMessage()).isEqualTo("predictDate는 yyyyMM 형식이어야 합니다.");
                });

        then(dashboardMapper).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("파일 다운로드 Service는 invalid predictDate를 repository 조회 전에 거절한다")
    void rejectInvalidPredictDateBeforeFileDownloadLookup() {
        assertThatThrownBy(() -> miDashboardService.getDashboardSummaryFileDownload(
                1, "20250511", "202613", "summary.zip"))
                .isInstanceOfSatisfying(AppErrorException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ResultCode.Error.REQ_INVALID_PARAMETER));

        then(customerCompanyRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("빈 시장 데이터는 기존 기간별 null 값을 유지한다")
    void keepEmptyMarketDataShape() {
        ChartPredictSetting setting = monthlySetting();
        List<LocalDate> monthlyDates = List.of(
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 7, 1));

        List<DashboardChartRawData> result = miDashboardService.calculateAverages(
                monthlyDates, List.of(), "date", "value", false, null, setting);

        assertThat(result).containsExactly(
                DashboardChartRawData.ofActual("2025-06", null),
                DashboardChartRawData.ofActual("2025-07", null));
    }

    @Test
    @DisplayName("nullable value와 정상 평균 계산 결과를 유지한다")
    void keepNullableValueAndNormalAverage() {
        ChartPredictSetting setting = monthlySetting();
        Map<String, Object> nullableValueRow = marketRow("20250615", null);
        Map<String, Object> normalValueRow = marketRow(
                "20250715", new BigDecimal("12.345"));

        List<DashboardChartRawData> result = miDashboardService.calculateAverages(
                List.of(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 7, 1)),
                List.of(nullableValueRow, normalValueRow),
                "date",
                "value",
                false,
                null,
                setting);

        assertThat(result).containsExactly(
                DashboardChartRawData.ofActual("2025-06", 0.0),
                DashboardChartRawData.ofActual("2025-07", 12.35));
    }

    @ParameterizedTest(name = "[{index}] requiredDate={0}")
    @NullSource
    @ValueSource(strings = {"", "invalid", "20251301"})
    @DisplayName("null 또는 invalid 필수 날짜는 공개 detail 없는 내부 오류로 처리한다")
    void rejectInvalidRequiredMarketDate(String requiredDate) {
        ChartPredictSetting setting = mock(ChartPredictSetting.class);
        Map<String, Object> invalidRow = marketRow(requiredDate, 10.0);

        assertThatThrownBy(() -> miDashboardService.calculateAverages(
                List.of(LocalDate.of(2025, 6, 1)),
                List.of(invalidRow),
                "date",
                "value",
                false,
                null,
                setting))
                .isInstanceOfSatisfying(SystemErrorException.class, exception -> {
                    assertThat(exception.getErrorCode())
                            .isEqualTo(ResultCode.Error.SYS_INTERNAL_ERROR);
                    assertThat(exception.getAdditionalMessage()).isNull();
                });
    }

    @Test
    @DisplayName("정상 row와 invalid 날짜가 섞여도 invalid row를 skip하지 않는다")
    void rejectMixedInvalidRequiredMarketDate() {
        ChartPredictSetting setting = monthlySetting();

        assertThatThrownBy(() -> miDashboardService.calculateAverages(
                List.of(LocalDate.of(2025, 6, 1)),
                List.of(marketRow("20250601", 10.0), marketRow("invalid", 20.0)),
                "date",
                "value",
                false,
                null,
                setting))
                .isInstanceOfSatisfying(SystemErrorException.class,
                        exception -> assertThat(exception.getAdditionalMessage()).isNull());
    }

    @ParameterizedTest(name = "[{index}] forecastDate={0}")
    @NullSource
    @ValueSource(strings = {"", "2025", "invalid", "20251301"})
    @DisplayName("invalid 예측 수행일은 skip 없이 공개 detail 없는 내부 오류로 처리한다")
    void rejectInvalidForecastDate(String forecastDate) {
        ChartPredictSetting setting = monthlySetting();
        Map<String, Object> forecastRow = forecastRow(forecastDate, 10.0);

        assertThatThrownBy(() -> miDashboardService.calculateAverages(
                List.of(LocalDate.of(2025, 6, 1)),
                List.of(forecastRow),
                "date",
                "value",
                true,
                LocalDate.of(2025, 6, 1),
                setting))
                .isInstanceOfSatisfying(SystemErrorException.class, exception -> {
                    assertThat(exception.getErrorCode())
                            .isEqualTo(ResultCode.Error.SYS_INTERNAL_ERROR);
                    assertThat(exception.getAdditionalMessage()).isNull();
                });
    }

    @Test
    @DisplayName("정상 예측 수행일의 기존 평균과 predictDate를 유지한다")
    void keepValidForecastAverage() {
        ChartPredictSetting setting = monthlySetting();

        List<DashboardChartRawData> result = miDashboardService.calculateAverages(
                List.of(LocalDate.of(2025, 6, 1)),
                List.of(forecastRow("20250601", 10.0)),
                "date",
                "value",
                true,
                LocalDate.of(2025, 6, 1),
                setting);

        assertThat(result).containsExactly(
                DashboardChartRawData.ofForecast("2025-06", "20250601", 10.0));
    }

    private ChartPredictSetting monthlySetting() {
        ChartPredictSetting setting = mock(ChartPredictSetting.class);
        given(setting.getDefaultPeriodUnitSeparateCd()).willReturn(PeriodUnitSeparateCd.M);
        return setting;
    }

    private Map<String, Object> marketRow(Object date, Object value) {
        Map<String, Object> row = new HashMap<>();
        row.put("date", date);
        row.put("value", value);
        return row;
    }

    private Map<String, Object> forecastRow(Object forecastDate, Object value) {
        Map<String, Object> row = marketRow("20250615", value);
        row.put("prdc_fulf_dt", forecastDate);
        return row;
    }
}
