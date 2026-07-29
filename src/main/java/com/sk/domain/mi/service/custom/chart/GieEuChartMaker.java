package com.sk.domain.mi.service.custom.chart;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.model.ValidationResult;
import com.sk.common.utils.DateUtil;
import com.sk.domain.mi.conversion.dto.BollingerBandResult;
import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.mi.dto.response.charts.Indicator;
import com.sk.domain.mi.dto.response.charts.Series;
import com.sk.domain.mi.dto.response.marketData.charts.MiMarketDataCache;
import com.sk.domain.mi.dto.response.marketData.charts.MiMarketDataRes;
import com.sk.domain.mi.dto.response.marketData.charts.graph.MarketDataChart;
import com.sk.domain.mi.dto.response.marketData.charts.graph.MarketDataChartRawData;
import com.sk.domain.mi.dto.response.marketData.charts.metaData.MarketMetaData;
import com.sk.domain.mi.service.MiMarketDataService;
import com.sk.domain.mi.service.MiMarketDynamicQueryService;
import com.sk.domain.mi.service.chart.ChartBundleFactory;
import com.sk.domain.mi.validator.GoodsDataSourceValidator;
import com.sk.domain.shared.entity.master.tif.GieEu;
import com.sk.domain.shared.entity.master.tmd.Goods;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.chart.IndicatorType;
import com.sk.domain.shared.enums.chart.RenderAs;
import com.sk.domain.shared.repository.master.tif.GieEuRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataSourceDetailRepository;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GieEuChartMaker implements CustomChartMaker<MiMarketDataRes>{

    private final GoodsDataSourceValidator validatedGoodsDataSource;
    private final AuthorizationService authorizationService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final MiMarketDynamicQueryService miMarketDynamicQueryService;

    private final GoodsDataSourceDetailRepository goodsDataSourceDetailRepository;
    private final GieEuRepository gieEuRepository;

    @Value("${custom.gie-eu.goods-data-source-no}")
    private Integer goodsDataSourceNo;
    @Value("${custom.gie-eu.card-index:7}")
    private Short cardIndex;

    private static final String CUSTOM_ID = "gie-eu";
    private static final String CUSTOM_DATA_CACHE_KEY = "data_source:"+CUSTOM_ID+":%d";

    private enum SeriesLabel {
        upper("max"),
        middle("avg"),
        lower("min");

        final String display;
        SeriesLabel(String display){
            this.display = display;
        }
        String getDisplay(){
            return display;
        }
    }

    @Override
    public String getCustomId() {
        return "gie-eu";
    }

    @Override
    public Integer getGoodsDataSourceId() {
        return goodsDataSourceNo;
    }

    @Override
    @Transactional
    public MiMarketDataRes getDashboardChart(Integer companyNo) {
        // 권한 조회
        PermissionRes permissionRes = authorizationService.getPermissions();
        return this.getDashboardChart(companyNo, permissionRes);
    }

    @Override
    public MiMarketDataRes getDashboardChart(Integer companyNo, PermissionRes permissionRes) {

        ValidationResult<GoodsDataSourceDetail> validationResult = validatedGoodsDataSource.ensureExists(goodsDataSourceNo);
        // 26.07.29 취약점 진단 수정, '널 역참조-102206,102207,102208,102209' -> ensureExists 실패 시 data()가 null이므로 getOrThrow()로 교체
        GoodsDataSourceDetail mainGoodsDataSource = validationResult.getOrThrow();
        GoodsData mainGoodsData = mainGoodsDataSource.getGoodsData();
        Goods mainGoods = mainGoodsData.getGoods();

        Boolean isPermitted =
            permissionRes
                .getGoodsViewableMap(companyNo)
                .getOrDefault(mainGoods.getGoodsNo(), false);

        String cacheKey = String.format(CUSTOM_DATA_CACHE_KEY, goodsDataSourceNo);

        LocalDate lastCurrentDate = getLastGieEuCurrent();
        if(lastCurrentDate == null){
            return emptyMarketData(mainGoodsData, mainGoodsDataSource, isPermitted, cacheKey);
        }

        final LocalDate START_DATE = LocalDate.of(lastCurrentDate.getYear(), 1, 1);
        final LocalDate END_DATE = LocalDate.of(lastCurrentDate.getYear(), 12, 31);

        // 캐시 조회
        String cachedData = redisTemplate.opsForValue().get(cacheKey);
        if (cachedData != null) {
            try {
                MiMarketDataCache miMarketDataCache =
                    objectMapper.readValue(cachedData, MiMarketDataCache.class);

                // 권한이 없는 경우 메타데이터까지만 제공
                if (!isPermitted) {
                    // 마켓데이터 메타데이터 생성 (기간 변화 유형 제외)
                    MarketMetaData marketMetaData =
                        MarketMetaData.of(
                            cardIndex,
                            mainGoodsData,
                            mainGoodsDataSource,
                            null,
                            miMarketDataCache.marketMetaData().indexStartDt(),
                            miMarketDataCache.marketMetaData().lastUpdateDt());

                    return MiMarketDataRes.of(isPermitted, marketMetaData);
                }

                // 권한이 있는 경우 모든 데이터 제공
                return MiMarketDataRes.of(
                    isPermitted,
                    miMarketDataCache.marketMetaData(),
                    miMarketDataCache.mainChart(),
                    miMarketDataCache.compareCharts());
            } catch (Exception e) {
                // 캐시 데이터가 유효하지 않은 경우 캐시 삭제
                redisTemplate.delete(cacheKey);
                log.error("마켓데이터 캐시 조회 실패 : {}", e.getMessage());
            }
        }

        String[] tableColumnListNm =
            Arrays.stream(mainGoodsDataSource.getTableColumnListNm().split(","))
                .map(String::trim)
                .toArray(String[]::new);
        // 날짜 칼럼
        String dateColumnName = tableColumnListNm[MiMarketDataService.MARKET_DATA_DATE_COLUMN_INDEX];
        // 기본 데이터 칼럼
        String defaultDataColumnName =
            tableColumnListNm[MiMarketDataService.MARKET_DATA_DEFAULT_DATA_COLUMN_INDEX];

        // 원시 데이터 조회
        List<Map<String, Object>> rawData =
            miMarketDynamicQueryService.getMarketDataRows(mainGoodsDataSource, dateColumnName, START_DATE, END_DATE);

        // 원시 데이터가 없는 경우 권한이 없는 것으로 처리
        if (rawData.isEmpty()) {

        }

        // 월별 데이터로 변환
        List<MarketDataChartRawData> mainDashboardData =
            convertToDashBoardData(
                rawData,
                dateColumnName,
                defaultDataColumnName,
                START_DATE, END_DATE);

        MarketDataChart mainChart = MarketDataChart.of(mainDashboardData);

        // 비교군 데이터소스 조회
        List<MarketDataChart> compareCharts = new ArrayList<>();


        List<GoodsDataSourceDetail> goodsDataSourceDetails = goodsDataSourceDetailRepository.findByGoodsDataAndIsDeletedFalse(mainGoodsData);
        for(GoodsDataSourceDetail goodsDataSourceDetail : goodsDataSourceDetails){
            String code;
            if(goodsDataSourceDetail.getQueryConditionCntt().contains(GieEu.GIE_EU_CODE_PAST_5Y_AVG)){
                code = GieEu.GIE_EU_CODE_PAST_5Y_AVG;
            }else if(goodsDataSourceDetail.getQueryConditionCntt().contains(GieEu.GIE_EU_CODE_PAST_5Y_MAX)){
                code = GieEu.GIE_EU_CODE_PAST_5Y_MAX;
            }else if(goodsDataSourceDetail.getQueryConditionCntt().contains(GieEu.GIE_EU_CODE_PAST_5Y_MIN)){
                code = GieEu.GIE_EU_CODE_PAST_5Y_MIN;
            }else{
                continue;
            }

            List<Map<String, Object>> codeRawData =
                miMarketDynamicQueryService.getMarketDataRows(goodsDataSourceDetail, dateColumnName, START_DATE, END_DATE);

            List<MarketDataChartRawData> codeData =
                convertToDashBoardData(
                    codeRawData,
                    dateColumnName,
                    defaultDataColumnName, START_DATE, END_DATE);

            compareCharts.add(MarketDataChart.of(codeData, code));
        }

        // 마켓데이터 메타데이터 생성
        MarketMetaData marketMetaData =
            MarketMetaData.of(
                cardIndex,
                mainGoodsData,
                mainGoodsDataSource,
                Collections.emptyMap(),
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        // 캐시 저장
        MiMarketDataCache miMarketDataCache =
            MiMarketDataCache.of(marketMetaData, mainChart, compareCharts);
        saveToMarketDataCache(cacheKey, miMarketDataCache);

        // 권한이 없는 경우 메타데이터까지만 제공
        if (!isPermitted) {
            // 마켓데이터 메타데이터 생성 (기간 변화 유형 제외)
            MarketMetaData marketMetaDataNoPermitted =
                MarketMetaData.of(
                    cardIndex,
                    mainGoodsData,
                    mainGoodsDataSource,
                    Collections.emptyMap(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);

            return MiMarketDataRes.of(isPermitted, marketMetaDataNoPermitted);
        }

        return MiMarketDataRes.of(isPermitted, marketMetaData, mainChart, compareCharts);
    }


    @Override
    public Indicator createBollingerBandData(List<String> timestamps, List<BigDecimal> series,
                                             List<String> finalizedIndicators) {
        List<Series> bollingerBandsIndicator = createBollingerBandsIndicator(getMinMaxAvg5Years(timestamps), finalizedIndicators);
        return Indicator.of(IndicatorType.BOLLINGER_BANDS, timestamps, bollingerBandsIndicator);
    }

    private void saveToMarketDataCache(String key, MiMarketDataCache cache) {
        try {
            String json = objectMapper.writeValueAsString(cache);
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(MiMarketDataService.MARKET_DATA_CACHE_TTL));
        } catch (Exception e) {
            log.error("Failed to serialize and save cache for key: {}", key, e);
        }
    }

    private List<BollingerBandResult> getMinMaxAvg5Years(List<String> timestamps){
        LocalDateTime start = DateUtil.fromYmd(timestamps.get(0)).atTime(0,0,0);
        LocalDateTime end = DateUtil.fromYmd(timestamps.get(timestamps.size()-1)).atTime(0,0,0);

        Map<String, BigDecimal> upper = gieEuRepository.findAllByCodeAndDateBetween(GieEu.GIE_EU_CODE_PAST_5Y_MAX, start, end)
            .stream().collect(Collectors.toMap(i -> DateUtil.toYmd(i.getDate()), GieEu::getFull));
        Map<String, BigDecimal> mid = gieEuRepository.findAllByCodeAndDateBetween(GieEu.GIE_EU_CODE_PAST_5Y_AVG, start, end)
            .stream().collect(Collectors.toMap(i -> DateUtil.toYmd(i.getDate()), GieEu::getFull));
        Map<String, BigDecimal> lower = gieEuRepository.findAllByCodeAndDateBetween(GieEu.GIE_EU_CODE_PAST_5Y_MIN, start, end)
            .stream().collect(Collectors.toMap(i -> DateUtil.toYmd(i.getDate()), GieEu::getFull));

        return timestamps.stream().map(t->BollingerBandResult.of(mid.get(t), upper.get(t), lower.get(t))).toList();
    }

    public static List<Series> createBollingerBandsIndicator(List<BollingerBandResult> results, List<String> finalizedIndicators) {

        Map<SeriesLabel, List<BigDecimal>> series = Map.of(
            SeriesLabel.upper, results.stream().map(BollingerBandResult::upper).toList(),
            SeriesLabel.middle, results.stream().map(BollingerBandResult::middle).toList(),
            SeriesLabel.lower, results.stream().map(BollingerBandResult::lower).toList()
        );

        RenderAs renderAs = RenderAs.Y5;

        List<Series> seriesList = new ArrayList<>();

        series.forEach((seriesLabel, value) -> {
            String renderingId = renderAs.getRenderingId(seriesLabel.name());
            String label = String.format("%s(%s)", renderAs.getDisplayDescription(), seriesLabel.getDisplay());
            // 마지막 설정을 읽어 오기 위해서는 BB 로 표시 해야함.
            seriesList.add(Series.of(renderingId, label, renderAs, value, finalizedIndicators.contains("BB")));
        });

        return seriesList;
    }

    private static List<MarketDataChartRawData> convertToDashBoardData(List<Map<String, Object>> rawData,
                                                                       String dateColumnName,
                                                                       String defaultDataColumnName,
                                                                       LocalDate start, LocalDate end) {
        if (rawData == null || rawData.isEmpty()) {
            return Collections.emptyList();
        }

        return rawData.stream()
            .map(r->{
                String date = DateUtil.toYmdHyphen(DateUtil.parseDate(r.get(dateColumnName)));
                Object value = r.get(defaultDataColumnName);

                return MarketDataChartRawData.builder()
                    .date(date)
                    .value(value == null ? null : Double.parseDouble(value.toString()))
                    .build()
                    ;
            }).filter(data->{
                LocalDate date = DateUtil.fromYmdHyphen(data.date());
                return date.isAfter(start.minusDays(1)) && date.isBefore(end.plusDays(1));
            })
            .toList();
    }

    private LocalDate getLastGieEuCurrent(){
        Optional<GieEu> lastGieEuOptional = gieEuRepository.findFirstByCodeInOrderByDateDesc(Arrays.asList(
            GieEu.GIE_EU_CODE_CURRENT
        ));
        return lastGieEuOptional.map(gieEu -> gieEu.getDate().toLocalDate()).orElse(null);
    }

    private MiMarketDataRes emptyMarketData(GoodsData mainGoodsData, GoodsDataSourceDetail mainGoodsDataSource,
                                            Boolean isPermitted, String cacheKey){
        MarketMetaData marketMetaData =
            MarketMetaData.of(
                cardIndex,
                mainGoodsData,
                mainGoodsDataSource,
                null,
                null,
                null);
        MarketDataChart mainChart = MarketDataChart.of(Collections.emptyList());
        List<MarketDataChart> compareCharts = Collections.emptyList();

        MiMarketDataCache miMarketDataCache =
            MiMarketDataCache.of(marketMetaData, mainChart, compareCharts);
        saveToMarketDataCache(cacheKey, miMarketDataCache);

        return MiMarketDataRes.of(true, marketMetaData, mainChart, compareCharts);
    }
}
