package com.sk.domain.mi;

import com.sk.domain.mi.conversion.dto.BollingerBandResult;
import com.sk.domain.mi.dto.response.chart.ChartData;
import com.sk.domain.mi.dto.response.chart.IndicatorData;
import com.sk.domain.mi.dto.response.chart.SchemaEntry;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ChartBundleFactory {

    /**
     * ChartData 객체에서 '종가(close)' 또는 'main' 해당하는 데이터 시리즈를 추출하여 List<BigDecimal>로 반환합니다.
     *
     */
    public static List<BigDecimal> extractMainSeries(ChartData chartData) {

        // 1. 스키마에서 "close" 역할(renderAs)을 하는 컬럼명(name) 찾기
        List<String> majorColumnNames = chartData.schema().stream()
            .filter(entry -> "close".equals(entry.renderAs()) || "main".equals(entry.renderAs()))
            .map(SchemaEntry::name) // 레코드 getter 사용 (Java 16+) 또는 entry -> entry.name()
            .toList();

        if (majorColumnNames.isEmpty()) {
            return Collections.emptyList();
        }

        String closeColumnName = majorColumnNames.get(0);

        // 2. 데이터 맵에서 해당 컬럼명의 데이터 리스트 가져오기
        // Map의 값 타입이 List<?> 이므로 형변환 필요 없음
        List<?> rawDataList = chartData.data().get(closeColumnName);

        if (rawDataList == null || rawDataList.isEmpty()) {
            // 해당 컬럼의 데이터가 없음, 빈 리스트 반환
            return Collections.emptyList();
        }

        // 3. 데이터 리스트의 각 요소를 BigDecimal로 변환 (null 유지)
        return rawDataList.stream()
            .map(s -> new BigDecimal(String.valueOf(s)))
            .collect(Collectors.toList());
    }

    public static ChartData convertToChartData(List<Map<String, Object>> rawData, String tableColumnListNm, Map<String, String> columnLabelsMap) {

        // todo 파라미터 검증...

        // tableColumnListNm을 쉼표로 분리하여 순서가 보장된 DB 컬럼명 리스트 생성
        List<String> dbColumnNamesInOrder = Arrays.stream(tableColumnListNm.split(","))
            .map(String::trim) // 공백 제거
            .filter(s -> !s.isEmpty()) // 빈 문자열 제거
            .collect(Collectors.toList());

        // 1. schema 생성 (순서 기반 renderAs 결정)
        List<SchemaEntry> schema = IntStream.range(0, dbColumnNamesInOrder.size())
            .mapToObj(i -> {
                String dbColName = dbColumnNamesInOrder.get(i);
                String renderAs = determineRenderAsByIndex(i); // 인덱스 기반으로 renderAs 결정
                String label = columnLabelsMap.getOrDefault(dbColName, dbColName); // 레이블 조회 (없으면 컬럼명 사용)
                return new SchemaEntry(dbColName, renderAs, label);
            })
            .collect(Collectors.toList());

        // 2. data 생성 (순서 보장된 컬럼명 리스트 사용)
        Map<String, List<?>> data = new HashMap<>();
        if (!rawData.isEmpty()) {

            for (String dbColName : dbColumnNamesInOrder) {
                // 컬럼명에 해당하는 값들을 rawData에서 추출
                List<Object> columnValues = rawData.stream()
                    .map(row -> row.get(dbColName)) // 해당 컬럼 값 추출
                    .collect(Collectors.toList());
                data.put(dbColName, columnValues);
            }

        } else {

            // rawData가 비어있으면, 모든 컬럼에 대해 빈 리스트를 넣어줌
            for (String dbColName : dbColumnNamesInOrder) {
                data.put(dbColName, Collections.emptyList());
            }

        }


        // 3. ChartData 객체 생성 및 반환
        // `columns` 필드에는 순서가 보장된 DB 컬럼명 리스트를 사용
        return new ChartData("CANDLE", dbColumnNamesInOrder, schema, data);
    }

    /**
     * 컬럼 인덱스를 기반으로 renderAs 값을 결정합니다.
     * (0: date, 1: open, 2: high, 3: close, 4: low, 그 외: "unknown")
     *
     * @param index 컬럼의 인덱스 (0부터 시작)
     * @return 결정된 renderAs 문자열
     */
    private static String determineRenderAsByIndex(int index) {
        return switch (index) {
            case 0 -> "date";
            case 1 -> "open";
            case 2 -> "high";
            case 3 -> "close";
            case 4 -> "low";
            default -> "unknown"; // 그 외의 경우
        };
    }

    /**
     * 이동평균선(MA) 지표 데이터를 생성하는 메서드
     */
    public static IndicatorData createMAIndicatorData(ChartData chartData, List<BigDecimal> ma5, List<BigDecimal> ma20, List<BigDecimal> ma60, List<BigDecimal> ma120) {

        // 날짜 데이터 가져오기
        List<?> dateData = chartData.data().get(chartData.columns().get(0)); // 첫 번째 컬럼은 날짜

        // 컬럼명 목록 생성
        List<String> columns = List.of("date", "ma5", "ma20", "ma60", "ma120");

        // 스키마 생성
        List<SchemaEntry> schema = new ArrayList<>();
        schema.add(new SchemaEntry("date", "date", "날짜"));
        schema.add(new SchemaEntry("ma5", "ma", "5일 이동평균"));
        schema.add(new SchemaEntry("ma20", "ma", "20일 이동평균"));
        schema.add(new SchemaEntry("ma60", "ma", "60일 이동평균"));
        schema.add(new SchemaEntry("ma120", "ma", "120일 이동평균"));

        // 데이터 맵 생성
        Map<String, List<?>> data = new HashMap<>();
        data.put("date", dateData);
        data.put("ma5", ma5);
        data.put("ma20", ma20);
        data.put("ma60", ma60);
        data.put("ma120", ma120);

        // IndicatorData 생성 및 반환
        return new IndicatorData("MA", columns, schema, data);
    }

    /**
     * 볼린저 밴드(BB) 지표 데이터를 생성하는 메서드
     */
    public static IndicatorData createBBIndicatorData(ChartData chartData, List<BollingerBandResult> bollingerBandResults) {
        // 날짜 데이터 가져오기
        List<?> dateData = chartData.data().get(chartData.columns().get(0)); // 첫 번째 컬럼은 날짜

        // 볼린저 밴드 데이터 추출
        List<BigDecimal> middleBand = new ArrayList<>();
        List<BigDecimal> upperBand = new ArrayList<>();
        List<BigDecimal> lowerBand = new ArrayList<>();

        for (BollingerBandResult result : bollingerBandResults) {
            middleBand.add(result.middle());
            upperBand.add(result.upper());
            lowerBand.add(result.lower());
        }

        // 컬럼명 목록 생성
        List<String> columns = List.of("date", "middle", "upper", "lower");

        // 스키마 생성
        List<SchemaEntry> schema = new ArrayList<>();
        schema.add(new SchemaEntry("date", "date", "날짜"));
        schema.add(new SchemaEntry("middle", "bb", "중간선"));
        schema.add(new SchemaEntry("upper", "bb", "상단선"));
        schema.add(new SchemaEntry("lower", "bb", "하단선"));

        // 데이터 맵 생성
        Map<String, List<?>> data = new HashMap<>();
        data.put("date", dateData);
        data.put("middle", middleBand);
        data.put("upper", upperBand);
        data.put("lower", lowerBand);

        // IndicatorData 생성 및 반환
        return new IndicatorData("BB", columns, schema, data);
    }

}
