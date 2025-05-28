package com.sk.domain.mi.dto.response.detail;

import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.PeriodUnitSeparateCd;
import com.sk.domain.shared.enums.chart.Interval;
import com.sk.domain.shared.enums.chart.Range;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

@Builder
public record GoodsDataSourceRes(

    @Schema(description = "상품 데이터 소스 번호", example = "10004")
    Integer goodsDataSourceNo,

    @Schema(description = "상품 데이터 소스 명", example = "structured data 1234")
    String goodsDataSourceNm,

    @Schema(
        description = "데이터 최소 주기 (D: 일간, W: 주간, M: 월간, Y: 연간)",
        allowableValues = {"D", "W", "M", "Y"}, example = "D"
    )
    PeriodUnitSeparateCd minInterval,

    /**
     * Range 별 허용 Interval 목록.
     * <pre>
     * e.g.  RANGE : 1M
     *       DEFAULT : 1W
     *       INTERVALS : [1W , 1M]
     * </pre>
     */
    List<AllowedPeriod> allowedPeriods

) {

    public record AllowedPeriod(
        String range,
        String defaultInterval,
        List<String> intervals
    ) {

        /**
         * Interval 를 표현할 때 사용할 고정된 정렬 순서 (가장 촘촘한 간격 → 가장 긴 간격)
         */
        private static final List<Interval> ORDERED_INTERVALS = List.of(
            Interval.ONE_DAY,
            Interval.ONE_WEEK,
            Interval.TWO_WEEK,
            Interval.ONE_MONTH,
            Interval.THREE_MONTH,
            Interval.SIX_MONTH,
            Interval.ONE_YEAR
        );

        /**
         * Range 별 baseline 허용 Interval 목록을 계산한다 (Interval.allowedRanges 기반)
         */
        private static final EnumMap<Range, List<Interval>> BASE_ALLOWED_INTERVALS;

        /**
         * Range 별 baseline 기본 Interval (문서에 제시된 규칙)
         */
        private static final EnumMap<Range, Interval> BASE_DEFAULT_INTERVAL;

        static {

            // 1) Baseline 허용 Interval 목록
            BASE_ALLOWED_INTERVALS = new EnumMap<>(Range.class);
            for (Range r : Range.values()) {
                List<Interval> list = ORDERED_INTERVALS.stream()
                    .filter(i -> i.getAllowedRanges().contains(r))
                    .toList();
                BASE_ALLOWED_INTERVALS.put(r, list);
            }

            // 2) Baseline 기본 Interval (표 하드‑코딩)
            BASE_DEFAULT_INTERVAL = new EnumMap<>(Range.class);
            BASE_DEFAULT_INTERVAL.put(Range.ONE_WEEK, Interval.ONE_DAY);
            BASE_DEFAULT_INTERVAL.put(Range.ONE_MONTH, Interval.ONE_DAY);
            BASE_DEFAULT_INTERVAL.put(Range.THREE_MONTHS, Interval.ONE_WEEK);
            BASE_DEFAULT_INTERVAL.put(Range.SIX_MONTHS, Interval.ONE_MONTH);
            BASE_DEFAULT_INTERVAL.put(Range.ONE_YEAR, Interval.ONE_MONTH);
            BASE_DEFAULT_INTERVAL.put(Range.TWO_YEARS, Interval.THREE_MONTH);
            BASE_DEFAULT_INTERVAL.put(Range.CUSTOM, Interval.ONE_DAY);
        }


        /**
         * 특정 데이터 소스에 대해, Range‑Interval 매핑 컬렉션을 생성한다.
         *
         * @param entity 데이터 소스 엔티티 (최소 주기 정보를 포함)
         * @return 필터링/정제된 {@link AllowedPeriod} 목록
         */
        public static List<AllowedPeriod> ofList(GoodsDataSourceDetail entity) {

            PeriodUnitSeparateCd minInterval = Optional.ofNullable(entity.getPeriodUnitSeparateCd()).orElse(PeriodUnitSeparateCd.D);

            List<AllowedPeriod> result = new ArrayList<>();

            for (Range range : Range.values()) {

                List<Interval> baseline = BASE_ALLOWED_INTERVALS.get(range);

                if (baseline == null || baseline.isEmpty()) {
                    continue;
                }

                // 최소 주기 필터링
                List<Interval> filtered = baseline.stream()
                    .filter(i -> i.getPeriodCd().isGreaterOrEqual(minInterval))
                    .toList();

                if (filtered.isEmpty()) {
                    // 이 Range 는 노출하지 않는다
                    continue;
                }

                // 기본 Interval 결정
                Interval defaultInterval = BASE_DEFAULT_INTERVAL.get(range);
                if (defaultInterval == null || !filtered.contains(defaultInterval)) {
                    defaultInterval = filtered.get(0);   // 가장 촘촘한 간격을 기본값으로
                }

                result.add(new AllowedPeriod(range.getCode(), defaultInterval.getCode(), filtered.stream().map(Interval::getCode).toList()));
            }

            return List.copyOf(result);
        }

    }

    public static GoodsDataSourceRes of(GoodsDataSourceDetail entity) {
        return GoodsDataSourceRes.builder()
            .goodsDataSourceNo(entity.getGoodsDataSourceNo())
            .goodsDataSourceNm(entity.getGoodsDataSourceNm())
            .minInterval(Optional.ofNullable(entity.getPeriodUnitSeparateCd()).orElse(PeriodUnitSeparateCd.D))
            .allowedPeriods(AllowedPeriod.ofList(entity))
            .build();
    }

    public static List<GoodsDataSourceRes> ofList(List<GoodsDataSourceDetail> entities) {
        return entities.stream()
            .map(GoodsDataSourceRes::of)
            .toList();
    }

}
