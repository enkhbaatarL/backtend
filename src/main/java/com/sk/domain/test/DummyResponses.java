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
        { "output": {
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
              },
              "usage": { "completion_tokens": 450, "prompt_tokens": 812, "total_tokens": 1234 }
            }
          ]
        }}}
        """, generateDates(31, "daily", 2024, 5, 1), generateRandomValues(31, 30, 80));

    /* --- 샘플 2: 단일 메시지, 단일 차트 (연간 재고 분석) --- */
    private static final String SAMPLE_2 = String.format("""
        { "output": {
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
              },
              "usage": { "completion_tokens": 420, "prompt_tokens": 4211, "total_tokens": 1245 }
            }
          ]
        }}
        """, generateDates(52, "weekly", 2023, 1, 2), generateRandomValues(52, 25, 35));

    /* --- 샘플 3: 단일 메시지, 다중 차트 (경쟁사 주가 비교) --- */
    private static final String SAMPLE_3 = String.format("""
            { "output": {
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
                  },
                  "usage": { "completion_tokens": 350, "prompt_tokens": 523, "total_tokens": 5341 }
                }
              ]
            }}
            """,
        generateDates(30, "daily", 2024, 4, 1), generateRandomValues(30, 150, 180), // OurStock
        generateDates(30, "daily", 2024, 4, 1), generateRandomValues(30, 120, 200), // Competitor A
        generateDates(30, "daily", 2024, 4, 1), generateRandomValues(30, 95, 105)   // Competitor B
    );

    /* --- 샘플 4: 다중 메시지, 각 메시지에 차트 포함 (분기별 실적과 다음 분기 예측) --- */
    private static final String SAMPLE_4 = String.format("""
        { "output": {
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
              },
              "usage": { "completion_tokens": 520, "prompt_tokens": 150, "total_tokens": 670 }
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
              },
              "usage": { "completion_tokens": 250, "prompt_tokens": 120, "total_tokens": 1200 }
            }
          ]
        }}
        """, generateDates(13, "weekly", 2024, 4, 7), generateRandomValues(13, 9.5, 11.5));

    private static final String SAMPLE_5 = """
        {
          "output": {
              "messages": [
                {
                  "content": {
                    "text": "## 📈 브랜트유 가격 추세 분석 및 그래프 생성\\n\\n### 📊 추세 분석\\n최근 브랜트유 가격의 주요 추세는 다음과 같습니다:\\n1. **2025년 초반(1~3월)**: 가격은 $80 이상으로 정점을 찍었으나 이후 하락 추세를 보였습니다. 1월 중반 최고점 $82.49 이후 점진적으로 하락했습니다.\\n2. **2025년 4월~6월**: 4월 초 $73.37에서 중순 $66.91로 하락, 이후 5월~6월까지 비교적 안정적이며 $60~$65 범위를 유지했습니다.\\n\\n### 📊 시계열 특성\\n- **변동성**: 2025년 초반(1월~3월) 변동성이 높았으나, 이후 안정적인 흐름을 보였습니다.\\n- **계절성**: 데이터에 의한 명확한 계절성은 확인되지 않으나, 연초와 봄철의 가격 변동이 두드러집니다.\\n- **이상치**: 2025년 1월 중반 $82.49까지 상승한 이후, 2025년 5월 초 $60.25까지 급격히 하락한 구간은 주목할 필요가 있습니다.\\n\\n---\\n\\n### 📈 그래프 설명\\n아래 그래프는 2025년 브랜트유 가격의 시계열 추세를 나타냅니다. X축은 날짜, Y축은 가격($)입니다. \\n\\n```python\\nimport matplotlib.pyplot as plt\\nimport pandas as pd\\n\\n# 데이터 정리\\ndata = [\\n    {'date': '2025-06-05T00:00:00', 'price': 65.2900}, {'date': '2025-06-04T00:00:00', 'price': 64.9100},\\n    {'date': '2025-06-03T00:00:00', 'price': 65.6100}, {'date': '2025-06-02T00:00:00', 'price': 65.1200},\\n    {'date': '2025-05-30T00:00:00', 'price': 63.9000}, {'date': '2025-05-29T00:00:00', 'price': 64.1300},\\n    {'date': '2025-05-28T00:00:00', 'price': 64.9800}, {'date': '2025-05-27T00:00:00', 'price': 64.2900},\\n    # ... 데이터 축약 ...\\n]\\ndf = pd.DataFrame(data)\\ndf['date'] = pd.to_datetime(df['date'])\\n\\n# 그래프 생성\\nplt.figure(figsize=(10, 6))\\nplt.plot(df['date'], df['price'], marker='o', label='브랜트유 가격')\\nplt.title('2025년 브랜트유 가격 추세')\\nplt.xlabel('날짜')\\nplt.ylabel('가격 ($)')\\nplt.grid(True)\\nplt.legend()\\nplt.show()\\n```\\n\\n🔹 위 코드를 실행하면 브랜트유의 최근 가격 변동을 시각적으로 확인할 수 있습니다.\\n\\n---\\n\\n### 💡 해석 및 시사점\\n1. **연초 급등과 하락**: 2025년 초반 가격 급등은 공급망 변화, 지정학적 리스크 등에 의해 발생했을 가능성이 높습니다. 이후 하락은 수요 감소 또는 시장 안정화로 해석됩니다.\\n2. **현재 안정적 추세**: 2025년 5월 이후 가격이 $60~$65 사이에서 안정적으로 유지되는 것은 시장의 수급이 안정화되고 있음을 나타냅니다.\\n3. **전망**: 안정적 흐름은 지속될 가능성이 있지만, 지정학적 사건이나 수요 변화가 발생할 경우 변동성이 다시 증가할 수 있습니다. 특히, 여름철 수요 증가 가능성은 주목할 필요가 있습니다.\\n\\n---\\n\\n### 추가로 궁금하실 수 있는 질문들:\\n1. 브랜트유 가격 변동에 영향을 미친 주요 요인은 무엇인가요?\\n2. 2024년과 2025년 브랜트유 가격의 연간 비교 분석을 제공해 줄 수 있나요?\\n3. 향후 브랜트유 가격 전망과 예상되는 시장 이벤트는 무엇인가요?",
                    "charts": [
                      {
                        "order": 1,
                        "legend": "Brent Crude Oil Price (USD/barrel)",
                        "date": [
                          "2025-01-15",
                          "2025-04-01",
                          "2025-05-05",
                          "2025-06-05"
                        ],
                        "value": [
                          82.49,
                          74.51,
                          60.25,
                          65.29
                        ]
                      }
                    ],
                    "sources": [],
                    "vector_sources": [
                      {
                        "source": "DAM Capital Advisors",
                        "title": "India Trade: Goods deficit widened; services in surplus",
                        "gds_no": 70008,
                        "document_id": 114763753
                      },
                      {
                        "source": "DAM Capital Advisors",
                        "title": "India Trade: Goods deficit widened; services in surplus",
                        "gds_no": 70008,
                        "document_id": 114763753
                      },
                      {
                        "source": "DAM Capital Advisors",
                        "title": "India Trade: Goods deficit widened; services in surplus",
                        "gds_no": 70008,
                        "document_id": 114763753
                      }
                    ],
                    "related_data": [
                      934,
                      935
                    ]
                  },
                  "usage": {
                    "completion_tokens": 951,
                    "prompt_tokens": 6124,
                    "total_tokens": 7075,
                    "completion_tokens_details": {
                      "accepted_prediction_tokens": 0,
                      "audio_tokens": 0,
                      "reasoning_tokens": 0,
                      "rejected_prediction_tokens": 0
                    },
                    "prompt_tokens_details": {
                      "audio_tokens": 0,
                      "cached_tokens": 5888
                    }
                  },
                  "sql_query": "SELECT date, trdprc_1 AS price\\nFROM midb.tif_iceeubrncrueltrenfutr\\nWHERE sequence = 1\\nORDER BY date DESC\\nLIMIT 200;",
                  "used_tables": [
                    "tif_iceeubrncrueltrenfutr: ICE(Intercontinental Exchange) 유럽 시장에서 전자거래 방식으로 거래되는 브렌트유(Brent Crude) 선물 가격을 1~41개월물 기준으로 연속적으로 구성한 월물 시계열 지표"
                  ]
                }
              ]
            }
        }
        """;


    private static final List<String> DUMMY_LIST = List.of(
        SAMPLE_1, SAMPLE_2, SAMPLE_3, SAMPLE_4, SAMPLE_5
    );

    public static String random() {
        return DUMMY_LIST.get(ThreadLocalRandom.current().nextInt(DUMMY_LIST.size()));
    }
}
