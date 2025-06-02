package com.sk.domain.test;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class DummyResponses {

    private static final String SAMPLE_1 = """
        {
          "output": {
            "messages": [{
              "content": {
                "text": "## 📈 2024-05 주요 제품 판매 실적\\n\\n스마트워치 X와 4K 모니터 Pro가 두각을 나타냈습니다. (상세 표 생략)",
                "charts": [{
                  "order": 1,
                  "legend": "스마트워치 X 일별 판매량",
                  "date": ["2024-05-01","2024-05-02","2024-05-03","2024-05-04","2024-05-05"],
                  "value": [30,32,35,33,38]
                }]
              }
            }]
          }
        }
        """;

    private static final String SAMPLE_2 = """
        {
          "output": {
            "messages": [{
              "content": {
                "text": "## 📊 2024-05 웹사이트 트래픽\\n\\nSEO 유입이 전월 대비 **+12 %** 늘었습니다.",
                "charts": [{
                  "order": 1,
                  "legend": "일별 방문자 수",
                  "date": ["2024-05-01","2024-05-02","2024-05-03","2024-05-04","2024-05-05"],
                  "value": [1800,1850,1900,1880,1950]
                }]
              }
            }]
          }
        }
        """;

    /* 메시지 1개 + **차트 2개** 예시 */
    private static final String SAMPLE_3 = """
        {
          "output": {
            "messages": [{
              "content": {
                "text": "## 🛒 2024-05 장바구니 이탈 분석\\n\\n패션 vs 디지털 비교",
                "charts": [{
                  "order": 1,
                  "legend": "패션 이탈률",
                  "date": ["2024-05-01","2024-05-02","2024-05-03","2024-05-04","2024-05-05"],
                  "value": [12.5,12.7,13.0,12.8,13.4]
                },{
                  "order": 2,
                  "legend": "디지털 이탈률",
                  "date": ["2024-05-01","2024-05-02","2024-05-03","2024-05-04","2024-05-05"],
                  "value": [9.1,9.3,9.5,9.4,9.7]
                }]
              }
            }]
          }
        }
        """;

    /* **메시지 2개**(CS 통계 + 응답시간) 예시 */
    private static final String SAMPLE_4 = """
        {
          "output": {
            "messages": [{
              "content": {
                "text": "## 💬 2024-05 고객 CS 유형별 통계\\n\\n배송 문의가 가장 많았습니다.",
                "charts": [{
                  "order": 1,
                  "legend": "CS 티켓 일별",
                  "date": ["2024-05-01","2024-05-02","2024-05-03","2024-05-04","2024-05-05"],
                  "value": [45,50,48,46,52]
                }]
              }
            },{
              "content": {
                "text": "## ⏱️ 2024-05 CS 응답시간 추이\\n\\n평균 응답 3.2 h → 2.7 h 로 개선!",
                "charts": [{
                  "order": 1,
                  "legend": "평균 응답시간(시간)",
                  "date": ["2024-05-01","2024-05-02","2024-05-03","2024-05-04","2024-05-05"],
                  "value": [3.4,3.3,3.1,2.9,2.7]
                }]
              }
            }]
          }
        }
        """;

    private static final String SAMPLE_5 = """
        {
          "output": {
            "messages": [{
              "content": {
                "text": "## 🎯 2024-05 마케팅 퍼널 전환율\\n\\n폼 완성률이 **28 % → 32 %** 로 상승.",
                "charts": [{
                  "order": 1,
                  "legend": "랜딩→폼→가입 전환율",
                  "date": ["2024-05-01","2024-05-02","2024-05-03","2024-05-04","2024-05-05"],
                  "value": [30,31,29,32,32]
                }]
              }
            }]
          }
        }
        """;

    private static final String SAMPLE_6 = """
        {
          "output": {
            "messages": [{
              "content": {
                "text": "## ⚙️ 2024-05 서버 에러 로그\\n\\n500 오류 감소, 502 일시 증가.",
                "charts": [{
                  "order": 1,
                  "legend": "500 오류",
                  "date": ["2024-05-01","2024-05-02","2024-05-03","2024-05-04","2024-05-05"],
                  "value": [5,3,4,2,1]
                },{
                  "order": 2,
                  "legend": "502 오류",
                  "date": ["2024-05-01","2024-05-02","2024-05-03","2024-05-04","2024-05-05"],
                  "value": [1,0,2,3,2]
                }]
              }
            }]
          }
        }
        """;

    private static final String SAMPLE_7 = """
        {
          "output": {
            "messages": [{
              "content": {
                "text": "## 📰 2024-05 뉴스레터 구독 퍼널\\n\\n가입 전환 **3.3 %** 기록.",
                "charts": [{
                  "order": 1,
                  "legend": "전환율(%)",
                  "date": ["2024-05-01","2024-05-02","2024-05-03","2024-05-04","2024-05-05"],
                  "value": [3.1,3.3,3.0,3.2,3.5]
                }]
              }
            }]
          }
        }
        """;

    private static final String SAMPLE_8 = """
        {
          "output": {
            "messages": [{
              "content": {
                "text": "## 📱 2024-05 앱 DAU vs 7-day Retention\\n\\n신규 기능 출시 효과 분석.",
                "charts": [{
                  "order": 1,
                  "legend": "일별 DAU",
                  "date": ["2024-05-01","2024-05-02","2024-05-03","2024-05-04","2024-05-05"],
                  "value": [9200,9400,9600,9500,9800]
                },{
                  "order": 2,
                  "legend": "7-일 유지율(%)",
                  "date": ["2024-05-01","2024-05-02","2024-05-03","2024-05-04","2024-05-05"],
                  "value": [42,43,44,43,45]
                }]
              }
            }]
          }
        }
        """;

    private static final String SAMPLE_9 = """
        {
          "output": {
            "messages": [{
              "content": {
                "text": "## 🚚 2024-05 공급망 리드타임 분석\\n\\n평균 14일 → 12일로 단축.",
                "charts": [{
                  "order": 1,
                  "legend": "리드타임(일)",
                  "date": ["2024-05-01","2024-05-08","2024-05-15","2024-05-22","2024-05-29"],
                  "value": [14,13,13,12,12]
                }]
              }
            }]
          }
        }
        """;

    private static final String SAMPLE_10 = """
        {
          "output": {
            "messages": [{
              "content": {
                "text": "## 🔋 2024-05 배터리 수명 테스트\\n\\n18 h 달성! (실험 환경 동일)",
                "charts": [{
                  "order": 1,
                  "legend": "충전 후 잔량(%)",
                  "date": ["2024-05-01","2024-05-02","2024-05-03","2024-05-04","2024-05-05"],
                  "value": [100,85,68,45,22]
                }]
              }
            }]
          }
        }
        """;

    private static final String SAMPLE_11 = """
                {
                  "output": {
                    "messages": [
                      {
                        "content": {
                          "text": "## 📈 2024년 5월 주요 제품 판매 실적 분석\\n\\n안녕하세요! 2024년 5월 주요 제품들의 판매 실적을 분석한 결과입니다.\\n\\n아래 표는 각 제품별 판매량 및 전월 대비 증감률을 나타냅니다.\\n\\n| 제품명         | 카테고리 | 판매량 (개) | 전월 대비 증감률 (%) | 비고     |\\n|----------------|----------|-------------|--------------------|----------|\\n| 스마트워치 X   | 웨어러블 | 1,250       | +15.2              | 신모델 출시 |\\n| AI 스피커 Mini | 스마트홈 | 880         | +8.5               | 프로모션 진행 |\\n| 무선 이어폰 Z  | 오디오   | 2,130       | -2.1               | 경쟁 심화  |\\n| 4K 모니터 Pro  | 디스플레이| 670         | +22.0              | 수요 증가  |\\n\\n전반적으로 스마트워치 X와 4K 모니터 Pro의 판매 성장이 두드러지며, 무선 이어폰 Z는 경쟁 심화로 소폭 감소했습니다. AI 스피커 Mini는 프로모션 효과로 꾸준한 성장을 보이고 있습니다.\\n\\n다음은 '스마트워치 X'의 일별 판매량 추이 그래프입니다.",
                          "charts": [
                            {
                              "order": 1,
                              "legend": "스마트워치 X 일별 판매량 (5월)",
                              "date": [
                                "2024-05-01", "2024-05-02", "2024-05-03", "2024-05-04", "2024-05-05",
                                "2024-05-06", "2024-05-07", "2024-05-08", "2024-05-09", "2024-05-10",
                                "2024-05-11", "2024-05-12", "2024-05-13", "2024-05-14", "2024-05-15",
                                "2024-05-16", "2024-05-17", "2024-05-18", "2024-05-19", "2024-05-20",
                                "2024-05-21", "2024-05-22", "2024-05-23", "2024-05-24", "2024-05-25",
                                "2024-05-26", "2024-05-27", "2024-05-28", "2024-05-29", "2024-05-30",
                                "2024-05-31"
                              ],
                              "value": [
                                30.0, 32.0, 35.0, 33.0, 38.0, 40.0, 42.0, 45.0, 43.0, 48.0,
                                50.0, 55.0, 52.0, 49.0, 45.0, 48.0, 53.0, 58.0, 60.0, 62.0,
                                59.0, 55.0, 50.0, 48.0, 52.0, 57.0, 61.0, 65.0, 68.0, 70.0,
                                66.0
                              ]
                            }
                          ]
                        },
                        "additional_kwargs": {
                          "refusal": null
                        },
                        "response_metadata": {
                          "token_usage": {
                            "completion_tokens": 350,
                            "prompt_tokens": 120,
                            "total_tokens": 470,
                            "completion_tokens_details": {
                              "accepted_prediction_tokens": 340,
                              "audio_tokens": 0,
                              "reasoning_tokens": 10,
                              "rejected_prediction_tokens": 0
                            },
                            "prompt_tokens_details": {
                              "audio_tokens": 0,
                              "cached_tokens": 50
                            }
                          },
                          "model_name": "claude-3-opus-20240229",
                          "system_fingerprint": "fp_abc123",
                          "finish_reason": "stop_sequence",
                          "logprobs": null
                        },
                        "type": "ai_response",
                        "name": "ProductAnalysisBot",
                        "id": "msg_01HXZYJ8K9N7M5G4F3E2D1C0B",
                        "example": false,
                        "tool_calls": [],
                        "invalid_tool_calls": [],
                        "usage_metadata": {
                          "input_tokens": 120,
                          "output_tokens": 350,
                          "total_tokens": 470,
                          "input_token_details": {
                            "audio": 0,
                            "cache_read": 50
                          },
                          "output_token_details": {
                            "audio": 0,
                            "reasoning": 10
                          }
                        }
                      },
                      {
                        "content": {
                          "text": "## 📊 웹사이트 트래픽 분석 (2024년 5월)\\n\\n2024년 5월 한 달간 저희 웹사이트의 트래픽 변화를 요약해 드립니다.\\n\\n주요 유입 경로별 세션 수는 다음과 같습니다.\\n\\n| 유입 경로     | 세션 수   | 전월 대비 (%) | 주요 키워드 (상위 2개) |\\n|---------------|-----------|---------------|------------------------|\\n| 직접 유입     | 15,200    | +5.8          | -                      |\\n| 자연 검색(SEO)| 28,500    | +12.3         | '스마트워치 추천', 'AI 스피커 가격' |\\n| 소셜 미디어   | 8,700     | -3.1          | (캠페인 종료)          |\\n| 추천(Referral)| 4,100     | +7.5          | 'IT뉴스사이트', '기술블로그' |\\n\\n자연 검색을 통한 유입이 크게 증가했으며, 소셜 미디어는 특정 캠페인 종료로 인해 감소했습니다. 전반적인 트래픽은 증가 추세입니다.\\n\\n아래는 일별 총 방문자 수 변화를 나타내는 차트입니다.",
                          "charts": [
                            {
                              "order": 1,
                              "legend": "웹사이트 일별 총 방문자 수 (5월)",
                              "date": [
                                "2024-05-01", "2024-05-02", "2024-05-03", "2024-05-04", "2024-05-05",
                                "2024-05-06", "2024-05-07", "2024-05-08", "2024-05-09", "2024-05-10",
                                "2024-05-11", "2024-05-12", "2024-05-13", "2024-05-14", "2024-05-15",
                                "2024-05-16", "2024-05-17", "2024-05-18", "2024-05-19", "2024-05-20",
                                "2024-05-21", "2024-05-22", "2024-05-23", "2024-05-24", "2024-05-25",
                                "2024-05-26", "2024-05-27", "2024-05-28", "2024-05-29", "2024-05-30",
                                "2024-05-31"
                              ],
                              "value": [
                                1800.0, 1850.0, 1900.0, 1880.0, 1950.0, 2000.0, 2050.0, 2100.0, 2080.0, 2150.0,
                                2200.0, 2250.0, 2220.0, 2180.0, 2100.0, 2150.0, 2230.0, 2300.0, 2350.0, 2380.0,
                                2330.0, 2280.0, 2200.0, 2180.0, 2250.0, 2320.0, 2380.0, 2450.0, 2500.0, 2550.0,
                                2510.0
                              ]
                            }
                          ]
                        },
                        "additional_kwargs": {
                          "refusal": null
                        },
                        "response_metadata": {
                          "token_usage": {
                            "completion_tokens": 400,
                            "prompt_tokens": 100,
                            "total_tokens": 500,
                            "completion_tokens_details": {
                              "accepted_prediction_tokens": 390,
                              "audio_tokens": 0,
                              "reasoning_tokens": 10,
                              "rejected_prediction_tokens": 0
                            },
                            "prompt_tokens_details": {
                              "audio_tokens": 0,
                              "cached_tokens": 30
                            }
                          },
                          "model_name": "claude-3-sonnet-20240307",
                          "system_fingerprint": "fp_def456",
                          "finish_reason": "max_tokens",
                          "logprobs": null
                        },
                        "type": "ai_response",
                        "name": "TrafficAnalysisBot",
                        "id": "msg_01HXZYK2R7S6P5H4G3F2E1D0C",
                        "example": false,
                        "tool_calls": [],
                        "invalid_tool_calls": [],
                        "usage_metadata": {
                          "input_tokens": 100,
                          "output_tokens": 400,
                          "total_tokens": 500,
                          "input_token_details": {
                            "audio": 0,
                            "cache_read": 30
                          },
                          "output_token_details": {
                            "audio": 0,
                            "reasoning": 10
                          }
                        }
                      }
                    ]
                  },
                  "metadata": {
                    "run_id": "run_xyz789abc123",
                    "feedback_tokens": []
                  }
                }
                """;

    private static final String SAMPLE_12 = """
        {
          "output": {
            "messages": [{
              "content": {
                "text": "## 🏬 2024-05 매장별 매출 요약\\n\\n아래 표는 오프라인 매장 3곳의 매출 현황입니다.\\n\\n| 매장 | 매출액 (₩) | 전월 대비 |\\n|------|-----------|-----------|\\n| 강남 | ₁₅₀,₀₀₀,₀₀₀ | +8 % |\\n| 홍대 | ₁₂₀,₀₀₀,₀₀₀ | +5 % |\\n| 부산 | ₈₀,₀₀₀,₀₀₀  | -2 % |",
                "charts": [{
                  "order": 1,
                  "legend": "강남점 일별 매출",
                  "date": ["2024-05-01","2024-05-02","2024-05-03","2024-05-04","2024-05-05"],
                  "value": [4.2,4.4,4.8,4.6,5.0]
                }]
              }
            }]
          }
        }
        """;

    private static final String SAMPLE_13 = """
        {
          "output": {
            "messages": [{
              "content": {
                "text": "## 🍞 2024-05 베이커리 SKU 생산·폐기 현황\\n\\n| SKU | 생산량 (개) | 폐기율 (%) |\\n|-----|------------|-----------|\\n| 미니식빵 | 12,000 | 1.2 |\\n| 치즈크림번 | 8,500 | 0.8 |\\n| 모카롤    | 6,300 | 1.5 |",
                "charts": [{
                  "order": 1,
                  "legend": "미니식빵 일별 생산량",
                  "date": ["2024-05-01","2024-05-02","2024-05-03","2024-05-04","2024-05-05"],
                  "value": [400,420,430,415,435]
                }]
              }
            }]
          }
        }
        """;

    private static final String SAMPLE_14 = """
        {
          "output": {
            "messages": [{
              "content": {
                "text": "## 📦 2024-05 재고 회전율 TOP 3\\n\\n| SKU | 회전일수 | 대상월 대비 |\\n|-----|---------|------------|\\n| SKU-A | 24 | ▲3일 개선 |\\n| SKU-B | 27 | ▲1일 개선 |\\n| SKU-C | 29 | ▼2일 악화 |"
              }
            },{
              "content": {
                "text": "## 🚨 2024-05 재고 부족 Risk SKU\\n\\n| SKU | 현재 재고 | 안전재고 ||\\n|-----|-----------|-----------|\\n| SKU-X | 120 | 300 |\\n| SKU-Y | 80  | 250 |",
                "charts": [{
                  "order": 1,
                  "legend": "SKU-X 잔여 재고 추이",
                  "date": ["2024-05-01","2024-05-02","2024-05-03","2024-05-04","2024-05-05"],
                  "value": [140,135,130,125,120]
                }]
              }
            }]
          }
        }
        """;

    private static final String SAMPLE_15 = """
        {
          "output": {
            "messages": [{
              "content": {
                "text": "## 📧 2024-05 이메일 캠페인 성과\\n\\n| 캠페인 | 오픈율 (%) | 클릭율 (%) | 전환율 (%) |\\n|--------|-----------|-----------|-----------|\\n| 웰컴 시리즈 | 48.2 | 12.3 | 4.1 |\\n| 재참여 유도 | 35.7 |  9.8 | 3.3 |\\n| 시즌 프로모션 | 51.0 | 14.1 | 5.6 |",
                "charts": [{
                  "order": 1,
                  "legend": "웰컴 시리즈 일별 오픈율",
                  "date": ["2024-05-01","2024-05-02","2024-05-03","2024-05-04","2024-05-05"],
                  "value": [45.0,47.0,49.0,48.5,51.5]
                }]
              }
            }]
          }
        }
        """;

    private static final String SAMPLE_16 = """
        {
          "output": {
            "messages": [{
              "content": {
                "text": "## 🏷️ 2024-05 프로모션별 매출 기여도\\n\\n| 프로모션 | 총 매출 (₩) | 기여도 (%) |\\n|-----------|-------------|-------------|\\n| 5월 어린이날 | ₉₀,₀₀₀,₀₀₀ | 35 |\\n| 주말 번들 | ₆₅,₀₀₀,₀₀₀ | 25 |\\n| 멤버십 데이 | ₅₅,₀₀₀,₀₀₀ | 22 |\\n| SNS 이벤트 | ₄₅,₀₀₀,₀₀₀ | 18 |",
                "charts": [{
                  "order": 1,
                  "legend": "어린이날 프로모션 일별 매출",
                  "date": ["2024-05-03","2024-05-04","2024-05-05","2024-05-06","2024-05-07"],
                  "value": [12,15,18,14,11]
                }]
              }
            }]
          }
        }
        """;

    /* 리스트로 묶기 */
    private static final List<String> DUMMY_LIST = List.of(
        SAMPLE_1, SAMPLE_2, SAMPLE_3, SAMPLE_4, SAMPLE_5,
        SAMPLE_6, SAMPLE_7, SAMPLE_8, SAMPLE_9, SAMPLE_10,
        SAMPLE_11, SAMPLE_12, SAMPLE_13, SAMPLE_14, SAMPLE_15, SAMPLE_16
    );

    public static String random() {
        return DUMMY_LIST.get(ThreadLocalRandom.current().nextInt(DUMMY_LIST.size()));
    }

    private DummyResponses() {}
}


