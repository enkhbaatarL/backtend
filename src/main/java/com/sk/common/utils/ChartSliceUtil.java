package com.sk.common.utils;

import com.sk.domain.mi.dto.response.charts.Chart;
import com.sk.domain.mi.dto.response.charts.ChartBundleRes;
import com.sk.domain.mi.dto.response.charts.Indicator;
import com.sk.domain.mi.dto.response.charts.Series;
import com.sk.domain.shared.enums.chart.Interval;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@UtilityClass
public class ChartSliceUtil {

    /**
     * 타임스탬프 목록에서 시작일에 해당하는 인덱스를 찾는다
     *
     * @param timestamps 타임스탬프 목록
     * @param startYmd   시작일자
     * @return 시작일에 해당하는 인덱스
     */
    private static int findStartIdx(List<String> timestamps, LocalDate startYmd) {
        String ymd = DateUtil.toYmd(startYmd);
        for (int i = 0; i < timestamps.size(); i++) {
            if (timestamps.get(i).compareTo(ymd) >= 0) return i;
        }
        return timestamps.size();
    }

    /**
     * 수치 데이터 리스트를 시작 인덱스부터 잘라서 새 리스트로 반환
     *
     * @param values 수치 데이터 목록
     * @param cut    시작 인덱스
     * @return 잘라낸 새 리스트
     */
    private static <N extends Number> List<N> sliceValues(List<N> values, int cut) {
        return new ArrayList<>(values.subList(cut, values.size()));
    }

    /**
     * 타임스탬프 리스트를 시작 인덱스부터 잘라서 새 리스트로 반환
     *
     * @param ts  타임스탬프 목록
     * @param cut 시작 인덱스
     * @return 잘라낸 새 리스트
     */
    private static List<String> sliceTimestamps(List<String> ts, int cut) {
        return new ArrayList<>(ts.subList(cut, ts.size()));
    }

    /**
     * 시리즈 데이터를 시작 인덱스부터 잘라서 새로운 시리즈 목록 생성
     *
     * @param src 원본 시리즈 목록
     * @param cut 시작 인덱스
     * @return 잘라낸 새 시리즈 목록
     */
    private static List<Series> sliceSeries(List<Series> src, int cut) {
        return src.stream()
            .map(s -> Series.of(
                s.seriesId(), s.label(), s.renderAs(),
                sliceValues(s.values(), cut),
                s.isVisible()))
            .collect(Collectors.toList());
    }

    /**
     * 차트 컨테이너 데이터를 시작 인덱스부터 잘라서 새로운 컨테이너 생성
     * builder 인자는 (timestamps, series)를 받아서 컨테이너를 만든다
     *
     * @param ts      타임스탬프 목록
     * @param series  시리즈 목록
     * @param cut     시작 인덱스
     * @param builder 새 컨테이너 생성 함수
     * @return 잘라낸 새 컨테이너
     */
    private static <C> C sliceContainer(List<String> ts, List<Series> series, int cut, BiFunction<List<String>, List<Series>, C> builder) {

        if (cut == 0) return builder.apply(ts, series);
        if (cut >= ts.size()) return builder.apply(List.of(), List.of());

        return builder.apply(sliceTimestamps(ts, cut), sliceSeries(series, cut));
    }

    /**
     * 차트 데이터를 시작일 기준으로 잘라내어 새로운 차트 생성
     *
     * @param src   원본 차트
     * @param start 시작일자
     * @return 잘라낸 새 차트
     */
    public static Chart sliceChart(Chart src, LocalDate start) {
        int cut = findStartIdx(src.timestamps(), start);
        return sliceContainer(
            src.timestamps(), src.series(), cut,
            (ts, ss) -> Chart.of(src.goodsDataSourceNo(), src.goodsDataSourceName(), src.goodDataName(), src.unitCd(), src.chartType(), ts, ss));
    }

    /**
     * 보조지표 데이터를 시작일 기준으로 잘라내어 새로운 보조지표 생성
     *
     * @param src   원본 보조지표
     * @param start 시작일자
     * @return 잘라낸 새 보조지표
     */
    public static Indicator sliceIndicator(Indicator src, LocalDate start) {
        int cut = findStartIdx(src.timestamps(), start);
        return sliceContainer(
            src.timestamps(), src.series(), cut,
            (ts, ss) -> Indicator.of(src.indicatorType(), ts, ss));
    }

    /**
     * 차트 번들의 모든 데이터를 시작일 기준으로 잘라내어 새로운 번들 생성
     *
     * @param src      원본 차트 번들
     * @param start    시작일자
     * @param interval 적재주기
     * @return 잘라낸 새 차트 번들
     */
    public static ChartBundleRes sliceBundle(ChartBundleRes src, LocalDate start, Interval interval) {

        LocalDate alignStart = Interval.resolveInclusiveStart(interval, start);

        // 메인차트 슬라이싱
        Chart main = sliceChart(src.mainChart(), alignStart);

        // 보조지표 슬라이싱
        List<Indicator> indicators = src.indicators() == null ? List.of() :
            src.indicators().stream()
                .map(ind -> sliceIndicator(ind, alignStart))
                .toList();

//        List<Chart> comps = src.comparisonCharts() == null ? List.of()
//            : src.comparisonCharts().stream()
//            .map(c -> sliceChart(c, alignStart))
//            .toList();

        return ChartBundleRes.of(src.metadata(), main, indicators, src.comparisonCharts());
    }
}
