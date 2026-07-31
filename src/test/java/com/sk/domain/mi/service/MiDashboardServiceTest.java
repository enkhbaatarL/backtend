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
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSettingDetail;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.mapper.dynamic.DashboardMapper;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingDetailRepository;
import com.sk.domain.shared.repository.master.tcm.ChartPredictSettingRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataSourceDetailRepository;
import com.sk.domain.shared.service.FileService;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
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
}
