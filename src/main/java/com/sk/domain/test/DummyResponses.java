package com.sk.domain.test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class DummyResponses {

    // --- Helper Methods 먼저 선언 ---
    private static String generateDates(int count, String type, int year, int month, int day) {
        LocalDate startDate = LocalDate.of(year, month, day);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return IntStream.range(0, count)
            .mapToObj(i -> {
                LocalDate currentDate = "daily".equals(type) ? startDate.plusDays(i) : startDate.plusWeeks(i);
                return "\"" + currentDate.format(formatter) + "\"";
            })
            .collect(Collectors.joining(", "));
    }

    private static String generateRandomValues(int count, double min, double max) {
        return IntStream.range(0, count)
            .mapToDouble(i -> ThreadLocalRandom.current().nextDouble(min, max))
            .mapToObj(d -> String.format("%.2f", d))
            .collect(Collectors.joining(", "));
    }

    // --- Static Final 필드는 메소드 호출 결과로 초기화 ---

    /* --- 샘플 1: 단일 메시지, 단일 차트 (월간 판매 실적) --- */
    private static final String SAMPLE_1 = String.format("""
        {
          "messages": [
            {
              "content": {
                "text": "## 📈 2024년 5월 제품 판매 실적 분석\\n\\n지난 5월, 신제품 **'AI 스마트워치 Pro'**의 출시 효과로 웨어러블 카테고리의 매출이 전월 대비 **23.5%%** 상승하며 가장 큰 성장을 기록했습니다. 반면, '4K 게이밍 모니터'는 경쟁 심화로 소폭 감소했습니다.\\n\\n### 카테고리별 상세 실적\\n| 카테고리 | 주요 제품 | 판매량 (개) | 매출액 (백만 $) | 전월 대비 증감률 |\\n|---|---|---|---|---|\\n| 웨어러블 | AI 스마트워치 Pro | 3,450 | 1.38 | ▲ 23.5%% |\\n| 스마트홈 | AI 스피커 Plus | 1,820 | 0.22 | ▲ 8.1%% |\\n| 오디오 | 무선 이어폰 Z | 2,130 | 0.43 | ▼ 2.1%% |\\n| 디스플레이 | 4K 게이밍 모니터 | 980 | 0.59 | ▼ 4.5%% |\\n\\n'AI 스마트워치 Pro'의 일별 판매량은 월말로 갈수록 꾸준히 증가하는 양상을 보였습니다.",
                "charts": [
                  { "order": 1, "legend": "AI 스마트워치 Pro 일별 판매량", "type": "actual", "date": [%s], "value": [%s] }
                ],
                "sources": [{"source_nm": "내부 판매 데이터베이스", "url": "internal://sales-db/monthly_report_202405"}],
                "vector_sources": [
                    {"source": "SalesDB_202405", "title": "Monthly Sales Report for May 2024", "gds_data_dstg_id": "45b4f8db-3f60-424e-a836-89d6df9ff3d3"}
                ],
                "related_data": [20, 3]
              }
            }
          ],
          "usage": { "completion_tokens": 450, "prompt_tokens": 120, "total_tokens": 570 }
        }
        """, generateDates(31, "daily", 2024, 5, 1), generateRandomValues(31, 30, 80));

    /* --- 샘플 2: 단일 메시지, 단일 차트 (연간 재고 분석) --- */
    private static final String SAMPLE_2 = String.format("""
        {
          "messages": [
            {
              "content": {
                "text": "## 📦 2023년 연간 재고 회전율 분석\\n\\n2023년 전체 평균 재고 회전일수는 **28.5일**로, 2022년(31.2일) 대비 **8.7%% 개선**되었습니다. 이는 SCM 최적화 프로젝트의 성과로 분석됩니다.\\n\\n### 제품 카테고리별 재고 회전일수 (2023년)\\n| 카테고리 | 평균 재고일수 | 2022년 대비 | 비고 |\\n|---|---|---|---|\\n| 패션 의류 | 45.1일 | ▼ 5.2일 | 계절성 상품 재고 관리 개선 |\\n| 디지털 가전 | 18.5일 | ▼ 2.1일 | 수요 예측 정확도 향상 |\\n| 생활 용품 | 33.8일 | ▼ 1.9일 | 안정적 수요 유지 |\\n\\n아래는 2023년 주차별 전체 재고 회전일수 변화 추이입니다.",
                "charts": [
                  { "order": 1, "legend": "주차별 재고 회전일수", "type": "actual", "date": [%s], "value": [%s] }
                ],
                "sources": [],
                "vector_sources": [
                    {"source": "SalesDB_202405", "title": "Monthly Sales Report for May 2024", "gds_data_dstg_id": "45b4f8db-3f60-424e-a836-89d6df9ff3d3"}
                ],
                "related_data": [20, 3]
              }
            }
          ],
          "usage": { "completion_tokens": 420, "prompt_tokens": 110, "total_tokens": 530 }
        }
        """, generateDates(52, "weekly", 2023, 1, 2), generateRandomValues(52, 25, 35));

    /* --- 샘플 3: 단일 메시지, 다중 차트 (경쟁사 주가 비교) --- */
    private static final String SAMPLE_3 = String.format("""
        {
          "messages": [
            {
              "content": {
                "text": "## 🏦 경쟁사 주가 비교 (지난 30일)\\n\\n최근 한 달간, 당사('OurStock')의 주가는 경쟁사 A에 비해 높은 변동성을 보였으나 꾸준히 우상향하는 패턴을 유지했습니다. 경쟁사 B는 안정적인 흐름을 보였습니다.",
                "charts": [
                  { "order": 1, "legend": "OurStock 주가 (USD)", "type": "actual", "date": [%s], "value": [%s] },
                  { "order": 2, "legend": "Competitor A 주가 실제가(USD)", "type": "actual", "date": [%s], "value": [%s] },
                  { "order": 3, "legend": "Competitor B 주가 예측가(USD)", "type": "predict", "date": [%s], "value": [%s] }
                ],
                "sources": [{"source_nm": "Yahoo Finance", "url": "https://finance.yahoo.com"}],
                "vector_sources": [
                    {"source": "SalesDB_202405", "title": "Monthly Sales Report for May 2024", "gds_data_dstg_id": "45b4f8db-3f60-424e-a836-89d6df9ff3d3"}
                ],
                "related_data": [20, 3]
              }
            }
          ],
          "usage": { "completion_tokens": 350, "prompt_tokens": 90, "total_tokens": 440 }
        }
        """,
        generateDates(30, "daily", 2024, 4, 1), generateRandomValues(30, 150, 180), // OurStock
        generateDates(30, "daily", 2024, 4, 1), generateRandomValues(30, 120, 200), // Competitor A
        generateDates(30, "daily", 2024, 4, 1), generateRandomValues(30, 95, 105)   // Competitor B
    );

    /* --- 샘플 4: 다중 메시지, 각 메시지에 차트 포함 (분기별 실적과 다음 분기 예측) --- */
    private static final String SAMPLE_4 = String.format("""
        {
          "messages": [
            {
              "content": {
                "text": "## 💰 2024년 1분기 재무 실적 요약\\n\\n1분기 매출은 **1.2억 달러**로 목표를 5%% 초과 달성했으며, 영업 이익률은 **15.2%%** 를 기록했습니다. 이는 신규 시장 진출 및 비용 절감 노력의 결과입니다.",
                "charts": [
                  { "order": 1, "legend": "월별 매출 (백만 $)", "date": ["2024-01-31", "2024-02-29", "2024-03-31"], "value": [38.5, 39.2, 42.3] }
                ],
                "sources": [],
                "vector_sources": [],
                "related_data": [3]
              }
            },
            {
              "content": {
                "text": "## 🎯 2024년 2분기 전망\\n\\n2분기에는 계절적 요인과 신제품 출시에 힘입어 매출이 **1.4억 달러**까지 성장할 것으로 예측됩니다. 아래는 주차별 예상 매출 추이입니다.",
                "charts": [
                  { "order": 1, "legend": "2분기 주차별 예상 매출 (백만 $)", "date": [%s], "value": [%s] }
                ],
                "sources": [{"source_nm": "내부 재무 예측 모델", "url": "internal://finance-forecast/q2_2024"}],
                "vector_sources": [{"source": "Finance_Forecast_Model_v2.1", "title": "Q2 2024 Financial Forecast"}],
                "related_data": [20]
              }
            }
          ],
          "usage": { "completion_tokens": 520, "prompt_tokens": 150, "total_tokens": 670 }
        }
        """, generateDates(13, "weekly", 2024, 4, 7), generateRandomValues(13, 9.5, 11.5));


    private static final List<String> DUMMY_LIST = List.of(
        SAMPLE_1, SAMPLE_2, SAMPLE_3, SAMPLE_4
    );

    public static String random() {
        return DUMMY_LIST.get(ThreadLocalRandom.current().nextInt(DUMMY_LIST.size()));
    }
}
