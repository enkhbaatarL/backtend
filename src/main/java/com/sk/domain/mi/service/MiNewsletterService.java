package com.sk.domain.mi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.model.PaginationRes;
import com.sk.common.model.TableRes;
import com.sk.domain.mi.dto.response.charts.Series;
import com.sk.domain.mi.dto.response.newsletter.NewsletterDetailRes;
import com.sk.domain.mi.dto.response.newsletter.NewsletterDetailRes.IssueDetail;
import com.sk.domain.mi.dto.response.newsletter.NewsletterDetailRes.NewsEventItem;
import com.sk.domain.mi.dto.response.newsletter.NewsletterDetailRes.NewsFlowChart;
import com.sk.domain.mi.dto.response.newsletter.NewsletterDetailRes.RelatedNews;
import com.sk.domain.mi.dto.response.newsletter.NewsletterFeaturedListRes;
import com.sk.domain.mi.dto.response.newsletter.NewsletterIssueListRes;
import com.sk.domain.mi.dto.response.newsletter.NewsletterListRes;
import com.sk.domain.shared.entity.master.tif.NewsletterChart;
import com.sk.domain.shared.entity.master.tmd.NewsletterEvent;
import com.sk.domain.shared.entity.master.tmd.NewsletterMaster;
import com.sk.domain.shared.entity.master.tmd.NewsletterPublishHistory;
import com.sk.domain.shared.entity.master.tmd.NewsletterTemplate;
import com.sk.domain.shared.entity.master.tmd.PreferenceTable;
import com.sk.domain.shared.enums.chart.RenderAs;
import com.sk.domain.shared.enums.newsletter.PublishStatusCode;
import com.sk.domain.shared.repository.master.tif.NewsletterChartRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterEventRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterMasterRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterPublishHistoryRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterTemplateRepository;
import com.sk.domain.shared.repository.master.tmd.PreferenceTableRepository;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MiNewsletterService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final NewsletterMasterRepository newsletterMasterRepository;
    private final NewsletterPublishHistoryRepository newsletterPublishHistoryRepository;
    private final NewsletterTemplateRepository newsletterTemplateRepository;
    private final NewsletterEventRepository newsletterEventRepository;
    private final NewsletterChartRepository newsletterChartRepository;
    private final PreferenceTableRepository preferenceTableRepository;

    public PaginationRes<NewsletterListRes> getNewsletterList(int page, int size, String keyword) {
        log.info("뉴스레터 목록 조회 - page: {}, size: {}, keyword: '{}'", page, size, keyword);

        try {
            // Pageable 객체 생성 (발행일시 역순 정렬)
            Pageable pageable = PageRequest.of(page - 1, size,
                Sort.by(Sort.Direction.DESC, "publishDateTime"));

            Page<NewsletterPublishHistory> historyPage;

            if (StringUtils.isBlank(keyword)) {
                historyPage = newsletterPublishHistoryRepository
                    .findByPublishStatusCodeAndIsDeletedFalseWithMaster(PublishStatusCode.PUBLISHED,
                        pageable);
            } else {
                historyPage = newsletterPublishHistoryRepository
                    .findByPublishStatusCodeAndIsDeletedFalseAndKeywordWithMaster(
                        PublishStatusCode.PUBLISHED, keyword, pageable);
            }

            Page<NewsletterListRes> newsletterPage = historyPage.map(
                this::convertToNewsletterListRes);

            return PaginationRes.toRes(newsletterPage);

        } catch (Exception e) {
            log.error("뉴스레터 목록 조회 중 오류 발생: {}", e.getMessage(), e);

            Pageable pageable = PageRequest.of(page - 1, size);
            Page<NewsletterListRes> emptyPage = new PageImpl<>(new ArrayList<>(), pageable, 0);
            return PaginationRes.toRes(emptyPage);
        }
    }

    /**
     * NewsletterPublishHistory를 NewsletterListRes로 변환
     */
    private NewsletterListRes convertToNewsletterListRes(NewsletterPublishHistory publishHistory) {
        NewsletterMaster master = publishHistory.getNewsletterMaster();

        return new NewsletterListRes(
            master.getNewsletterMasterId().toString(),                    // newsletterId
            publishHistory.getPublishDateTime(),                          // date
            publishHistory.getIssueNo().toString(),                       // issueNo
            master.getMasterName(),                                       // name
            publishHistory.getNewsletterTitle(),                          // title
            publishHistory.getPublishStatusCode().name()                  // status
        );
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
            {"Market Volatility Analysis", "Quarterly Performance Review",
                "Emerging Market Trends"},
            {"AI Technology Breakthrough", "Semiconductor Industry Update",
                "Cloud Computing Growth"},
            {"Interest Rate Impact", "Inflation Outlook 2025", "GDP Growth Projections"},
            {"International Trade Update", "Currency Market Analysis", "Commodity Price Trends"},
            {"Healthcare Sector Review", "Energy Transition Report",
                "Manufacturing Industry Insights"},
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

        List<NewsletterFeaturedListRes> featuredList = getFeaturedNewsletters();

        // 페이징 처리
        Pageable pageable = PageRequest.of(page - 1, size);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), featuredList.size());

        List<NewsletterFeaturedListRes> pageContent = start >= featuredList.size()
            ? new ArrayList<>()
            : featuredList.subList(start, end);

        Page<NewsletterFeaturedListRes> newsletterPage = new PageImpl<>(pageContent, pageable,
            featuredList.size());

        return PaginationRes.toRes(newsletterPage);
    }

    private List<NewsletterFeaturedListRes> getFeaturedNewsletters() {
        List<NewsletterFeaturedListRes> latestNewsletters = new ArrayList<>();

        try {
            List<NewsletterMaster> newsletterMasters = newsletterMasterRepository.findAll();

            for (NewsletterMaster master : newsletterMasters) {
                Optional<NewsletterPublishHistory> latestPublished =
                    newsletterPublishHistoryRepository.findLatestPublishedByNewsletterMasterId(
                        master.getNewsletterMasterId(),
                        PublishStatusCode.PUBLISHED
                    );

                NewsletterFeaturedListRes featuredRes = createFeaturedListRes(master,
                    latestPublished.orElse(null));
                latestNewsletters.add(featuredRes);
            }

        } catch (Exception e) {
            log.error("뉴스레터 데이터 조회 중 오류 발생: {}", e.getMessage(), e);
            return new ArrayList<>();
        }

        return latestNewsletters;
    }

    private NewsletterFeaturedListRes createFeaturedListRes(NewsletterMaster master,
        NewsletterPublishHistory latestHistory) {
        return new NewsletterFeaturedListRes(
            master.getNewsletterMasterId().toString(),                    // newsletterId
            master.getPeriodType().name(),                                // frequency
            master.getMasterName(),                                       // name
            master.getExplanation(),                                      // description
            false,
            latestHistory != null ? latestHistory.getIssueNo().toString() : "0",  // issueNo
            encodeImageToBase64(master.getImageUrl(),
                "image/png"),                                         // image
            true

        );
    }


    /**
     * 더미 Base64 이미지 생성 실제로는 이미지 파일을 읽어서 Base64로 인코딩하거나, DB에서 Base64 인코딩된 이미지 데이터를 조회해야 합니다.
     */
    private String generateDummyBase64Image() {
        String imageBase64 = encodeImageToBase64("static/images/oil_trend_cover_img.png",
            "image/png");
        // 1x1 픽셀 투명 PNG 이미지의 Base64 인코딩
        return imageBase64;
    }

    /**
     * 이미지 파일을 Base64로 인코딩하는 유틸리티 메서드
     */
    private String encodeImageToBase64(String imagePath, String mimeType) {
        try {
            ClassPathResource resource = new ClassPathResource(imagePath);
            try (InputStream inputStream = resource.getInputStream()) {
                byte[] imageBytes = inputStream.readAllBytes();
                String base64 = Base64.getEncoder().encodeToString(imageBytes);
                return "data:" + mimeType + ";base64," + base64;
            }
        } catch (IOException e) {
            log.error("이미지 파일을 Base64로 인코딩하는 중 오류 발생: {}", imagePath, e);
            return generateDummyBase64Image();
        }
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
     * 뉴스레터 발행 리스트 조회
     */
    public NewsletterIssueListRes getNewsletterIssueList(String newsletterId) {
    log.info("뉴스레터 발행 리스트 조회 - newsletterId: {}", newsletterId);

    try {
        Optional<NewsletterMaster> newsletterMaster = newsletterMasterRepository.findByNewsletterMasterIdAndIsDeletedFalse(Integer.parseInt(newsletterId));

        if (newsletterMaster.isEmpty()) {
            log.warn("뉴스레터를 찾을 수 없습니다. - newsletterId: {}", newsletterId);
            return new NewsletterIssueListRes(newsletterId, Collections.emptyList());
        }

        List<NewsletterPublishHistory> publishedHistories = newsletterPublishHistoryRepository
            .findByNewsletterMasterAndPublishStatusCodeAndIsDeletedFalseOrderByIssueNoDesc(
                newsletterMaster.get(),
                PublishStatusCode.PUBLISHED
            );

        List<NewsletterIssueListRes.IssuedItem> issuedList = publishedHistories.stream()
            .map(history -> new NewsletterIssueListRes.IssuedItem(
                history.getPublishDateTime().format(DateTimeFormatter.ofPattern("yy-MM-dd")),
                history.getIssueNo().toString())
            )
            .toList();

        return new NewsletterIssueListRes(newsletterId, issuedList);

    } catch (Exception e) {
        log.error("뉴스레터 발행 리스트 조회 중 오류 발생 - newsletterId: {}", newsletterId, e);
        return new NewsletterIssueListRes(newsletterId, Collections.emptyList());
    }
}


    /**
     * 뉴스레터 상세 조회
     */
    public NewsletterDetailRes getNewsletterDetail(String newsletterId, String issueNo) {
        log.info("뉴스레터 상세 조회 - newsletterId: {}, issueNo: {}", newsletterId, issueNo);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");
        DateTimeFormatter f2 = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try {
            NewsletterMaster nm = newsletterMasterRepository.findByNewsletterMasterIdAndIsDeletedFalse(
                Integer.parseInt(newsletterId)).get();

            NewsletterPublishHistory nph =
                newsletterPublishHistoryRepository.findByNewsletterMasterAndIssueNo(
                    nm,
                    Integer.parseInt(issueNo)
                ).get();

            List<NewsletterTemplate> templates = newsletterTemplateRepository.findByNewsletterTemplateId(
                nph.getNewsletterTemplateId());

            List<NewsletterChart> flowChart = newsletterChartRepository.findByNewsletterPublishHistoryAndCode(nph, "LCOc1");

            List<NewsletterEvent> events = newsletterEventRepository.findByNewsletterPublishHistory(
                nph);

            NewsletterDetailRes.ChartData chartData = convertNewsletterChartsToChartData(flowChart);

            NewsFlowChart newsFlowChart = new NewsFlowChart(
                "주요 뉴스 Flow 및 Brent 유가 추이",
                "뉴욕시간 전일 20:00부터 당일 17:00까지",
                chartData
            );

            PreferenceTable preferenceTable = preferenceTableRepository.findByNewsletterPublishHistoryAndPreferenceTableCode(
                nph, "OIL_PRC_TRND").get();

            List<NewsEventItem> newsEventItems = events.stream()
                .map(event -> new NewsEventItem(
                    event.getNewsletterEventId().toString(),  // newsId
                    event.getEventTitle(),                     // title
                    List.of(event.getEventContent()),         // content (단일 문자열을 리스트로 변환)
                    Timestamp.valueOf(event.getEventCreateTime()).getTime(),
                    event.getEventCreateTime().format(f2), // createdDate
                    event.getEventMarkTime(),                 // createTime
                    event.getEventProvider(),                 // source
                    "Y".equals(event.getTurningPointMarkYn()),
                    event.getDirectionalityType()// showInChart (변곡점 표시 여부)
                ))
                .toList();

            RelatedNews relatedNews = new RelatedNews(
                "뉴욕시간 전일 20:00부터 당일 17:00까지",
                newsEventItems
            );

            IssueDetail issueDetail = new IssueDetail(
                nph.getNewsletterTitle(),
                nph.getPublishDateTime().format(formatter),
                nph.getAiSummary(),
                newsFlowChart,
                relatedNews,
                null
            );

            NewsletterDetailRes nldr = new NewsletterDetailRes(
                nm.getNewsletterMasterId().toString(),
                nm.getPeriodType().getCode(),
                nm.getMasterName(),
                nm.getExplanation(),
                false,
                encodeImageToBase64(nm.getImageUrl(), "image/png"),
                true,
                issueDetail);

            return nldr;

        } catch (Exception e) {
            log.error("뉴스레터 데이터 조회 중 오류 발생: {}", e.getMessage(), e);
            return new NewsletterDetailRes(newsletterId,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            );
        }


    }

    private TableRes readTableDataFromJson(String jsonPath) {
        try {
            ClassPathResource resource = new ClassPathResource(jsonPath);
            try (InputStream inputStream = resource.getInputStream()) {
                // JSON을 Map으로 파싱
                TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {
                };
                Map<String, Object> jsonData = objectMapper.readValue(inputStream, typeRef);

                // header 파싱 (JSON의 "header" 키 사용)
                List<TableRes.Column> headers = new ArrayList<>();
                @SuppressWarnings("unchecked")
                List<Map<String, String>> headersList = (List<Map<String, String>>) jsonData.get(
                    "header");
                if (headersList != null) {
                    for (Map<String, String> headerMap : headersList) {
                        String field = headerMap.get("field");
                        String name = headerMap.get("name");
                        headers.add(TableRes.Column.of(field, name));
                    }
                }

                // detail 파싱 (JSON의 "detail" 키 사용)
                List<TableRes.Row> rows = new ArrayList<>();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> detailList = (List<Map<String, Object>>) jsonData.get(
                    "detail");
                if (detailList != null) {
                    for (Map<String, Object> detailMap : detailList) {
                        @SuppressWarnings("unchecked")
                        List<String> cells = (List<String>) detailMap.get("cells");
                        if (cells != null) {
                            rows.add(TableRes.Row.of(cells));
                        }
                    }
                }

                return TableRes.of("mock1", headers, rows);

            }
        } catch (IOException e) {
            log.error("JSON 파일을 TableRes로 변환하는 중 오류 발생: {}", jsonPath, e);
            // 오류 시 빈 테이블 반환
            return TableRes.of(Collections.emptyList(), Collections.emptyList());
        }
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

    /**
     * NewsletterChart 리스트를 ChartData로 변환
     */
    private NewsletterDetailRes.ChartData convertNewsletterChartsToChartData(
        List<NewsletterChart> flowCharts) {
        List<String> timestamps = new ArrayList<>();
        List<BigDecimal> highValues = new ArrayList<>();
        List<BigDecimal> lowValues = new ArrayList<>();
        List<BigDecimal> openValues = new ArrayList<>();
        List<BigDecimal> closeValues = new ArrayList<>();
        List<BigDecimal> volumeValues = new ArrayList<>();

        try {
            for (NewsletterChart chart : flowCharts) {
                // NewsletterChart 엔티티의 필드명은 추정이므로, 실제 필드명에 맞게 수정해야 합니다
                // 일반적으로 차트 데이터는 다음과 같은 필드를 가질 것으로 예상됩니다
                Long timestamp = Timestamp.valueOf(chart.getTimestamp()).getTime(); // 또는 getDateTime(), getCreateTime() 등
                BigDecimal high = chart.getHigh1();           // 고가
                BigDecimal low = chart.getLow1();             // 저가
                BigDecimal open = chart.getOpenPrice();           // 시가
                BigDecimal close = chart.getTradePrice1();         // 종가
                BigDecimal volume = chart.getNumMoves();       // 거래량


                if (timestamp != null && high != null && low != null &&
                    open != null && close != null) {

                    timestamps.add(timestamp.toString());
                    highValues.add(high);
                    lowValues.add(low);
                    openValues.add(open);
                    closeValues.add(close);
                    volumeValues.add(volume != null ? volume : BigDecimal.ZERO);
                }
            }
        } catch (Exception e) {
            log.error("NewsletterChart 데이터 변환 중 오류 발생", e);
            return new NewsletterDetailRes.ChartData(
                Collections.emptyList(),
                Collections.emptyList()
            );
        }

        // Series 생성
        List<Series> seriesList = Arrays.asList(
            Series.of("high", "High", RenderAs.HIGH, highValues, true),
            Series.of("low", "Low", RenderAs.LOW, lowValues, true),
            Series.of("open", "Open", RenderAs.OPEN, openValues, true),
            Series.of("close", "Close", RenderAs.CLOSE, closeValues, true),
            Series.of("volume", "Volume", RenderAs.VOLUME, volumeValues, true)
        );

        return new NewsletterDetailRes.ChartData(timestamps, seriesList);
    }
}
