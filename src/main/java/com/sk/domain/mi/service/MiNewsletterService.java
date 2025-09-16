package com.sk.domain.mi.service;

import com.sk.domain.mi.dto.response.charts.Series;
import com.sk.domain.mi.dto.response.newsletter.NewsletterDetailRes;
import com.sk.domain.mi.dto.response.newsletter.NewsletterDetailRes.IssueDetail;
import com.sk.domain.mi.dto.response.newsletter.NewsletterDetailRes.NewsFlowChart;
import com.sk.domain.mi.dto.response.newsletter.NewsletterDetailRes.NewsItem;
import com.sk.domain.mi.dto.response.newsletter.NewsletterDetailRes.RelatedNews;
import com.sk.domain.mi.dto.response.newsletter.NewsletterFeaturedListRes;
import com.sk.domain.mi.dto.response.newsletter.NewsletterIssueListRes;
import com.sk.domain.mi.dto.response.newsletter.NewsletterListRes;
import com.sk.domain.shared.enums.chart.RenderAs;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.sk.common.model.PaginationRes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MiNewsletterService {

    /**
     * 뉴스레터 목록 조회 (Mock 데이터)
     */
    public PaginationRes<NewsletterListRes> getNewsletterList(int page, int size,
                                                              String keyword) {

        log.info("뉴스레터 목록 조회 - page: {}, size: {}, keyword: '{}'", page, size, keyword);

        // Mock 데이터 생성
        List<NewsletterListRes> allNewsletters = generateMockNewsletters();

        // 필터링 적용
        List<NewsletterListRes> filteredNewsletters = allNewsletters.stream()
            .filter(newsletter -> applyKeywordFilter(newsletter, keyword))
            .toList();

        // 페이징 처리
        Pageable pageable = PageRequest.of(page - 1, size);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredNewsletters.size());

        List<NewsletterListRes> pageContent = start >= filteredNewsletters.size()
            ? new ArrayList<>()
            : filteredNewsletters.subList(start, end);

        Page<NewsletterListRes> newsletterPage = new PageImpl<>(pageContent, pageable, filteredNewsletters.size());

        return PaginationRes.toRes(newsletterPage);
    }

    /**
     * Mock 뉴스레터 데이터 생성
     */
    private List<NewsletterListRes> generateMockNewsletters() {
        List<NewsletterListRes> newsletters = new ArrayList<>();

        // 다양한 상태와 내용으로 Mock 데이터 생성
        String[] names = {
            "Oil Price Trends", "Tech Trend Report", "Economic Outlook",
            "Global Market Analysis", "Industry Focus", "Investment Strategy Brief"
        };

        String[] statuses = {"PUBLISHING", "CHECK", "BLANK"};

        String[][] titlesByName = {
            {"Market Volatility Analysis", "Quarterly Performance Review", "Emerging Market Trends"},
            {"AI Technology Breakthrough", "Semiconductor Industry Update", "Cloud Computing Growth"},
            {"Interest Rate Impact", "Inflation Outlook 2025", "GDP Growth Projections"},
            {"International Trade Update", "Currency Market Analysis", "Commodity Price Trends"},
            {"Healthcare Sector Review", "Energy Transition Report", "Manufacturing Industry Insights"},
            {"Portfolio Optimization Tips", "Risk Management Strategies", "ESG Investment Trends"}
        };

        IntStream.range(1, 51).forEach(i -> {
            int nameIndex = (i - 1) % names.length;
            String name = names[nameIndex];
            String status = statuses[i % 3];
            String[] titles = titlesByName[nameIndex];
            String title = titles[(i - 1) % titles.length];

            newsletters.add(new NewsletterListRes(
                String.format("NL2025%03d", i),
                LocalDateTime.now().minusDays(i).withHour(9).withMinute(0).withSecond(0),
                String.format("%03d", i),
                name,
                title + String.format(" - Issue %d", i),
                status
            ));
        });

        return newsletters;
    }

    /**
     * 키워드 필터 적용
     */
    private boolean applyKeywordFilter(NewsletterListRes newsletter, String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return true;
        }
        String lowerKeyword = keyword.toLowerCase();
        return newsletter.title().toLowerCase().contains(lowerKeyword) ||
               newsletter.name().toLowerCase().contains(lowerKeyword);
    }

    /**
     * 상태 필터 적용
     */
    private boolean applyStatusFilter(NewsletterListRes newsletter, String status) {
        if (StringUtils.isBlank(status)) {
            return true;
        }
        return newsletter.status().equals(status);
    }

    /**
     * 발행번호 필터 적용
     */
    private boolean applyIssueNoFilter(NewsletterListRes newsletter, String issueNo) {
        if (StringUtils.isBlank(issueNo)) {
            return true;
        }
        return newsletter.issueNo().contains(issueNo);
    }

    /**
     * 뉴스레터별 최근호 조회
     */
    public PaginationRes<NewsletterFeaturedListRes> getLatestNewsletters(int page, int size) {

        // Mock 데이터 생성
        List<NewsletterFeaturedListRes> featuredList = generateMockLatestNewsletters();

        // 페이징 처리
        Pageable pageable = PageRequest.of(page - 1, size);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), featuredList.size());

        List<NewsletterFeaturedListRes> pageContent = start >= featuredList.size()
            ? new ArrayList<>()
            : featuredList.subList(start, end);

        Page<NewsletterFeaturedListRes> newsletterPage = new PageImpl<>(pageContent, pageable, featuredList.size());

        return PaginationRes.toRes(newsletterPage);
    }

    /**
     * Mock 뉴스레터 최근호 데이터 생성
     */
    private List<NewsletterFeaturedListRes> generateMockLatestNewsletters() {
        List<NewsletterFeaturedListRes> latestNewsletters = new ArrayList<>();

        // 뉴스레터 기본 정보
        Object[][] newsletterData = {
            {"NL001", "Oil Price Trends", "유가와 관련한 산업 동향에 대해 분석하고 주요 뉴스의 Flow 및 Brent Crude 추이를 분석", "DAILY", "152", "https://example.com/images/oil-trends.jpg", true, true},
            {"NL002", "Tech Innovation Report", "최신 기술 혁신과 스타트업 동향을 다루는 격주 뉴스레터", "WEEKLY", "087", "https://example.com/images/tech-innovation.jpg", false, true},
            {"NL003", "Economic Outlook", "국내외 경제 전망과 주요 지표 분석 월간 뉴스레터", "WEEKLY", "034", "https://example.com/images/economic-outlook.jpg", false, false},
            {"NL004", "Global Market Flash", "주요 글로벌 시장 이슈를 빠르게 전달하는 일간 뉴스레터", "DAILY", "2891", "https://example.com/images/market-flash.jpg", false, false},
            {"NL005", "Industry Focus", "산업별 심층 분석과 트렌드를 제공하는 주간 뉴스레터", "WEEKLY", "198", "https://example.com/images/industry-focus.jpg", false, false},
            {"NL006", "Investment Strategy Brief", "투자 전략과 포트폴리오 분석을 제공하는 격주 뉴스레터", "WEEKLY", "076", "https://example.com/images/investment-brief.jpg", false, false},
            {"NL007", "ESG Insights", "ESG 투자와 지속가능경영 트렌드를 다루는 월간 뉴스레터", "MONTHLY", "023", "https://example.com/images/esg-insights.jpg", false, false},
            {"NL008", "Crypto Weekly", "암호화폐 시장 동향과 블록체인 기술 분석 주간 뉴스레터", "WEEKLY", "143", "https://example.com/images/crypto-weekly.jpg", false, false},
            {"NL009", "Real Estate Monitor", "부동산 시장 분석과 투자 기회를 제공하는 월간 뉴스레터", "QUARTERLY", "045", "https://example.com/images/real-estate.jpg", false, false},
            {"NL010", "Healthcare Trends", "헬스케어 산업 동향과 의료 기술 혁신을 다루는 격주 뉴스레터", "WEEKLY", "092", "https://example.com/images/healthcare.jpg", false, false}
        };

        for (Object[] data : newsletterData) {
            latestNewsletters.add(new NewsletterFeaturedListRes(
                (String) data[0],           // newsletterId
                (String) data[3],           // frequency
                (String) data[1],           // name
                (String) data[2],           // description
                (Boolean) data[6],          // subscribed
                (String) data[4],           // issueNo
                (String) data[5],           // image
                (Boolean) data[7]           // enabled
            ));
        }

        return latestNewsletters;
    }

    /**
     * Mock 발행목록 생성
     */
    private List<NewsletterIssueListRes.IssuedItem> generateMockIssuedList(String newsletterId) {
      return List.of(
          new NewsletterIssueListRes.IssuedItem("25-09-12", "102"),
          new NewsletterIssueListRes.IssuedItem("25-09-11", "101"),
          new NewsletterIssueListRes.IssuedItem("25-09-10", "100"),
          new NewsletterIssueListRes.IssuedItem("25-09-09", "099"),
          new NewsletterIssueListRes.IssuedItem("25-09-08", "098")
      );
    }

    /**
     * 뉴스레터 발행 리스트 조회 (Mock 데이터)
     */
    public NewsletterIssueListRes getNewsletterIssueList(String newsletterId) {
        log.info("뉴스레터 발행 리스트 조회 - newsletterId: {}", newsletterId);

        List<NewsletterIssueListRes.IssuedItem> issuedList = generateMockIssuedList(newsletterId);

        return new NewsletterIssueListRes(
            newsletterId,
            issuedList
        );
    }

    /**
     * 뉴스레터 상세 조회 (Mock 데이터)
     */
    public NewsletterDetailRes getNewsletterDetail(String newsletterId, String issueNo) {
        log.info("뉴스레터 상세 조회 - newsletterId: {}, issueNo: {}", newsletterId, issueNo);

        // Mock 데이터 생성 및 반환
        return generateMockNewsletterDetail(newsletterId, issueNo);
    }

    /**
     * Mock 뉴스레터 상세 데이터 생성
     */
    private NewsletterDetailRes generateMockNewsletterDetail(String newsletterId, String issueNo) {

        // CSV 파일에서 차트 데이터 읽기
        NewsletterDetailRes.ChartData chartData = readBrentPriceDataAsChartData();

        NewsFlowChart newsFlowChart = new NewsFlowChart(
            "주요 뉴스 Flow 및 Brent 유가 추이",
            "뉴욕시간 전일 20:00부터 당일 17:00까지",
            chartData
        );

      RelatedNews relatedNews = new RelatedNews(
          "뉴욕시간 전일 20:00부터 당일 17:00까지",
          List.of(
              new NewsItem("ns001", "유럽 주요 경제 지표 발표",
                  List.of("건설 생산, 총계(EA19), 전월 대비 변동, 물량 전월 IC +0.92% 증가한 스0.76% (05:00)",
                      "EC 소비자 설문조사, 전체 응답자, 소비자 신뢰 지수 전년 발표 It A0.8 감소한 스15.5 (10:00)"),
                  Long.parseLong("1757466000"), "2025-09-10", "10:00", "Bloomberg", false),
              new NewsItem("ns002", "미국·EU, 공식 공동성명 통해 자동차•의약품 관세 협상 타결",
                  List.of("미국과 EU가 구두 합의한 무역협정 세부 내용 발표",
                      "미국은 EU 회원국산 차량에 대해 27.5% 관세 유지, 대부분 상품엔 15% 관세 적용",
                      "EU, 미국산 산업•농산품 관세 인하 조건 제시",
                      "의약품은 15% 관세 적용 제외, 반도체•목재 등도 유럽 면제"
                      ),
                  Long.parseLong("1757470200"), "2025-09-10", "11:10", "Reuters", true),
              new NewsItem("ns003", "인도-러시아 외무장관 회담, 무역 확대 합의 부각",
                  List.of("원유 수입 관련 최대 50% 추가 관세 불과, 러시아 원유 공급 의지 강조",
                      "인도•러시아 에너지 협력 확대"),
                  Long.parseLong("1757473500"), "2025-09-10", "12:05", "Financial Times|", true),
              new NewsItem("ns004", "미국 주요 경제 지표 발표",
                  List.of("신규 실업수당 청구건수 전주 Ht +11K건, 총 235K건 (08:30)",
                      "신규 실업수당 청구건수 4주 이동평균 +4.5K건, 총 226.25K건 (08:30)",
                      "지속 실업수당 청구건수 전주 Ht +0.02K건, 총 1.972M건 (08:30)",
                      "필라델피아 연방준비은행 제조업 지수: A 16.2p (08:30)",
                      "신규 주문, 고용지수, 지불가격지수 등 다양한 지표 발표"),
                  Long.parseLong("1757477700"), "2025-09-10", "13:15", "Reuters", false)
          )
      );


        IssueDetail issueDetail = new IssueDetail(
            "미-EU 우크라 안보 협상 및 이란 원유 제재 강화로 유가 강보합",
            "2025년 9월 12일",
            "지정학적 긴장이 고조되면서 유가는 변동성을 이어갔습니다. 미국과 EU 간의 관세 갈등, 미국의 대이란 추가 제재, 인도와 러시아의 에너지 협력 논의가 맞물리며 공급 불안 우려가 부각되었습니다. 동시에 미국 EIA의 주간 원유 재고 감소 발표는 유가 상승 압력을 강화했습니다. WTI는 주식시장 심리와 동조하는 흐름을 보이며 위험자산 전반의 변화를 반영했습니다. 전반적으로 글로벌 불확실성이 이어지는 가운데 단기적으로 유가의 상승 압력이 지속될 것으로 전망됩니다.",
            newsFlowChart,
            relatedNews,
            null
        );

        // 무조건 하나의 고정된 Mock 데이터 반환
        return new NewsletterDetailRes(
            "NL001",
            "DAILY",
            "Oil Price Trends",
            "유가와 관련한 산업 동향에 대해 분석하고 주요 뉴스의 Flow 및 Brent Crude 추이를 분석",
            true,
            "https://example.com/images/newsletter1.jpg",
            true,
            issueDetail
        );
    }

    /**
     * Brent 유가 CSV 데이터를 읽어서 새로운 ChartData 형태로 변환
     */
    private NewsletterDetailRes.ChartData readBrentPriceDataAsChartData() {
        List<String> timestamps = new ArrayList<>();
        List<BigDecimal> highValues = new ArrayList<>();
        List<BigDecimal> lowValues = new ArrayList<>();
        List<BigDecimal> openValues = new ArrayList<>();
        List<BigDecimal> closeValues = new ArrayList<>();
        List<BigDecimal> volumeValues = new ArrayList<>();

        try {
            ClassPathResource resource = new ClassPathResource("newsletter/brent_price_20250911.csv");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
                String line;
                boolean isFirstLine = true;

                while ((line = reader.readLine()) != null) {
                    // 첫 번째 라인(헤더)는 스킵
                    if (isFirstLine) {
                        isFirstLine = false;
                        continue;
                    }

                    String[] columns = line.split(",");
                    if (columns.length >= 6) { // volume 컬럼까지 포함하여 최소 6개 컬럼 필요
                        try {
                            // CSV 컬럼 매핑: timestamp, high_1, low_1, open_prc, trdprc_1, volume
                            String timestampStr = columns[0].trim();
                            String highStr = columns[2].trim();
                            String lowStr = columns[3].trim();
                            String openStr = columns[4].trim();
                            String closeStr = columns[5].trim();
                            String volumeStr = columns.length > 6 ? columns[6].trim() : "0";

                            // timestamp를 String으로 저장 (밀리초 변환)
                            Long timestamp = parseTimestamp(timestampStr);
                            BigDecimal high = parseBigDecimal(highStr);
                            BigDecimal low = parseBigDecimal(lowStr);
                            BigDecimal open = parseBigDecimal(openStr);
                            BigDecimal close = parseBigDecimal(closeStr);
                            BigDecimal volume = parseBigDecimal(volumeStr);

                            if (timestamp != null && high != null && low != null &&
                                open != null && close != null) {

                                timestamps.add(timestamp.toString());
                                highValues.add(high);
                                lowValues.add(low);
                                openValues.add(open);
                                closeValues.add(close);
                                volumeValues.add(volume != null ? volume : BigDecimal.ZERO);
                            }

                        } catch (Exception e) {
                            log.warn("CSV 라인 파싱 실패: {}", line, e);
                        }
                    }
                }
            }

        } catch (IOException e) {
            log.error("Brent 유가 CSV 파일 읽기 실패", e);
            // CSV 읽기 실패 시 빈 데이터 반환
            return new NewsletterDetailRes.ChartData(
                Collections.emptyList(),
                Collections.emptyList()
            );
        }

        // Series 생성 - seriesId가 high, low, open, close, volume로 설정됨
        List<Series> seriesList = Arrays.asList(
            Series.of("high", "High", RenderAs.HIGH, highValues, true),
            Series.of("low", "Low", RenderAs.LOW, lowValues, true),
            Series.of("open", "Open", RenderAs.OPEN, openValues, true),
            Series.of("close", "Close", RenderAs.CLOSE, closeValues, true),
            Series.of("volume", "Volume", RenderAs.VOLUME, volumeValues, true)
        );

        return new NewsletterDetailRes.ChartData(timestamps, seriesList);
    }

    /**
     * 타임스탬프 문자열을 Long으로 변환
     */
    private Long parseTimestamp(String timestampStr) {
        try {
            // "2025-09-10 00:00:00" 형태의 날짜 문자열을 파싱
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime dateTime = LocalDateTime.parse(timestampStr.trim(), formatter);
            return dateTime.toInstant(ZoneOffset.UTC).toEpochMilli();

        } catch (Exception e) {
            log.warn("타임스탬프 파싱 실패: {}", timestampStr, e);

            // 다른 형식도 시도해보기
            try {
                // 다양한 날짜 형식 지원
                DateTimeFormatter[] formatters = {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                    DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                    DateTimeFormatter.ofPattern("MM/dd/yyyy")
                };

                for (DateTimeFormatter fmt : formatters) {
                    try {
                        LocalDate date = LocalDate.parse(timestampStr.trim(), fmt);
                        return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
                    } catch (Exception ignored) {
                        // 다음 formatter 시도
                    }
                }
            } catch (Exception ignored) {
                // 모든 형식 실패
            }
        }

        return null;
    }

    /**
     * 문자열을 BigDecimal로 안전하게 변환
     */
    private BigDecimal parseBigDecimal(String str) {
        try {
            if (StringUtils.isBlank(str) || "null".equalsIgnoreCase(str)) {
                return null;
            }
            return new BigDecimal(str);
        } catch (NumberFormatException e) {
            log.warn("BigDecimal 파싱 실패: {}", str, e);
            return null;
        }
    }
}