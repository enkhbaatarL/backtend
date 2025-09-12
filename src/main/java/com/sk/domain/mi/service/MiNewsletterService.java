package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.response.newsletter.NewsletterDetailRes;
import com.sk.domain.mi.dto.response.newsletter.NewsletterFeaturedListRes;
import com.sk.domain.mi.dto.response.newsletter.NewsletterListRes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.apache.commons.lang3.StringUtils;
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
            {"NL003", "Economic Outlook", "국내외 경제 전망과 주요 지표 분석 월간 뉴스레터", "ON-DEMAND", "034", "https://example.com/images/economic-outlook.jpg", false, false},
            {"NL004", "Global Market Flash", "주요 글로벌 시장 이슈를 빠르게 전달하는 일간 뉴스레터", "DAILY", "2891", "https://example.com/images/market-flash.jpg", false, false},
            {"NL005", "Industry Focus", "산업별 심층 분석과 트렌드를 제공하는 주간 뉴스레터", "WEEKLY", "198", "https://example.com/images/industry-focus.jpg", false, false},
            {"NL006", "Investment Strategy Brief", "투자 전략과 포트폴리오 분석을 제공하는 격주 뉴스레터", "WEEKLY", "076", "https://example.com/images/investment-brief.jpg", false, false},
            {"NL007", "ESG Insights", "ESG 투자와 지속가능경영 트렌드를 다루는 월간 뉴스레터", "ON-DEMAND", "023", "https://example.com/images/esg-insights.jpg", false, false},
            {"NL008", "Crypto Weekly", "암호화폐 시장 동향과 블록체인 기술 분석 주간 뉴스레터", "WEEKLY", "143", "https://example.com/images/crypto-weekly.jpg", false, false},
            {"NL009", "Real Estate Monitor", "부동산 시장 분석과 투자 기회를 제공하는 월간 뉴스레터", "ON-DEMAND", "045", "https://example.com/images/real-estate.jpg", false, false},
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
    /**
     * Mock 뉴스레터 상세 데이터 생성
     */
    private NewsletterDetailRes generateMockNewsletterDetail(String newsletterId, String issueNo) {
        // Mock 발행목록 생성
        List<NewsletterDetailRes.IssuedItem> issuedList = List.of(
            new NewsletterDetailRes.IssuedItem(LocalDate.of(2025, 1, 15), "102"),
            new NewsletterDetailRes.IssuedItem(LocalDate.of(2025, 1, 14), "101"),
            new NewsletterDetailRes.IssuedItem(LocalDate.of(2025, 1, 13), "100"),
            new NewsletterDetailRes.IssuedItem(LocalDate.of(2025, 1, 12), "099"),
            new NewsletterDetailRes.IssuedItem(LocalDate.of(2025, 1, 11), "098")
        );

        // 무조건 하나의 고정된 Mock 데이터 반환
        return new NewsletterDetailRes(
            newsletterId,
            "DAILY",
            "Oil Price Trends",
            "유가와 관련한 산업 동향에 대해 분석하고 주요 뉴스의 Flow 및 Brent Crude 추이를 분석",
            true,
            issueNo,
            "https://example.com/images/newsletter1.jpg",
            true,
            issuedList
        );
    }
}