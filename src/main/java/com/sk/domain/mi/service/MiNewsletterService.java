package com.sk.domain.mi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.model.PaginationRes;
import com.sk.common.model.TableRes;
import com.sk.common.utils.CombineUtil;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataReq;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.dto.response.charts.Series;
import com.sk.domain.mi.dto.response.newsletter.NewsletterDetailRes;
import com.sk.domain.mi.dto.response.newsletter.NewsletterDetailRes.IssueDetail;
import com.sk.domain.mi.dto.response.newsletter.NewsletterDetailRes.NewsEventItem;
import com.sk.domain.mi.dto.response.newsletter.NewsletterDetailRes.NewsFlowChart;
import com.sk.domain.mi.dto.response.newsletter.NewsletterDetailRes.Reference;
import com.sk.domain.mi.dto.response.newsletter.NewsletterDetailRes.RelatedNews;
import com.sk.domain.mi.dto.response.newsletter.NewsletterFeaturedListRes;
import com.sk.domain.mi.dto.response.newsletter.NewsletterIssueListRes;
import com.sk.domain.mi.dto.response.newsletter.NewsletterListRes;
import com.sk.domain.shared.entity.master.tif.NewsletterChart;
import com.sk.domain.shared.entity.master.tmd.NewsletterCollectionChart;
import com.sk.domain.shared.entity.master.tmd.NewsletterEvent;
import com.sk.domain.shared.entity.master.tmd.NewsletterMaster;
import com.sk.domain.shared.entity.master.tmd.NewsletterPublishHistory;
import com.sk.domain.shared.entity.master.tmd.NewsletterTemplate;
import com.sk.domain.shared.entity.master.tmd.NewsletterTemplateCollectionChart;
import com.sk.domain.shared.entity.master.tmd.NewsletterTemplateDedicatedChart;
import com.sk.domain.shared.entity.master.tmd.NewsletterTemplatePreferenceTable;
import com.sk.domain.shared.enums.chart.RenderAs;
import com.sk.domain.shared.enums.newsletter.PublishStatusCode;
import com.sk.domain.shared.repository.master.tif.NewsletterChartRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterCollectionChartRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterEventRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterMasterRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterPublishHistoryRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterTemplateCollectionChartRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterTemplateDedicatedChartRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterTemplatePreferenceTableRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterTemplateRepository;
import com.sk.domain.shared.repository.master.tmd.PreferenceTableRepository;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
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

    private static final String MIME_TYPE = "image/png";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NewsletterMasterRepository newsletterMasterRepository;
    private final NewsletterPublishHistoryRepository newsletterPublishHistoryRepository;
    private final NewsletterTemplateRepository newsletterTemplateRepository;
    private final NewsletterEventRepository newsletterEventRepository;
    private final NewsletterChartRepository newsletterChartRepository;
    private final PreferenceTableRepository preferenceTableRepository;
    private final NewsletterTemplateDedicatedChartRepository newsletterTemplateDedicatedChartRepository;
    private final NewsletterTemplateCollectionChartRepository newsletterTemplateCollectionChartRepository;
    private final MybatisDynamicQueryService mybatisDynamicQueryService;
    private final NewsletterCollectionChartRepository newsletterCollectionChartRepository;
    private final NewsletterTemplatePreferenceTableRepository newsletterTemplatePreferenceTableRepository;
    private final AuthorizationService authorizationService;

    public PaginationRes<NewsletterListRes> getNewsletterList(Integer companyNo, int page, int size, String keyword) {
        log.info("뉴스레터 목록 조회 - page: {}, size: {}, keyword: '{}'", page, size, keyword);

        try {
            // Pageable 객체 생성 (발행일시 역순 정렬)
            Pageable pageable = PageRequest.of(page - 1, size,
                Sort.by(Sort.Direction.DESC, "publishDateTime"));

            Page<NewsletterPublishHistory> historyPage;

            if (StringUtils.isBlank(keyword)) {
                historyPage = newsletterPublishHistoryRepository
                    .findByPublishStatusCodeAndIsDeletedFalseWithMaster(PublishStatusCode.PUBLISHED,
                        companyNo, pageable);
            } else {
                historyPage = newsletterPublishHistoryRepository
                    .findByPublishStatusCodeAndIsDeletedFalseAndKeywordWithMaster(
                        PublishStatusCode.PUBLISHED, companyNo, keyword, pageable);
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
    public PaginationRes<NewsletterFeaturedListRes> getLatestNewsletters(Integer companyNo, int page, int size) {

        List<NewsletterFeaturedListRes> featuredList = getFeaturedNewsletters(companyNo);

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

    private List<NewsletterFeaturedListRes> getFeaturedNewsletters(Integer companyNo) {
        List<NewsletterFeaturedListRes> latestNewsletters = new ArrayList<>();

        try {
            List<NewsletterMaster> newsletterMasters = newsletterMasterRepository
                .findByCustomerCompanyNoAndIsDeletedFalse(companyNo);

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
            master.getPeriodType().getCode(),                                // frequency
            master.getMasterName(),                                       // name
            master.getExplanation(),                                      // description
            false,
            latestHistory != null ? latestHistory.getIssueNo().toString() : "0",  // issueNo
            encodeImageToBase64(master.getImageUrl(),
                MIME_TYPE),                                         // image
            master.isActive()

        );
    }

    /**
     * 더미 Base64 이미지 생성 실제로는 이미지 파일을 읽어서 Base64로 인코딩하거나, DB에서 Base64 인코딩된 이미지 데이터를 조회해야 합니다.
     */
    private String generateDummyBase64Image() {
        return encodeImageToBase64("static/images/oil_trend_cover_img.png", MIME_TYPE);
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
     * 뉴스레터 발행 리스트 조회
     */
    public NewsletterIssueListRes getNewsletterIssueList(Integer companyNo, String newsletterId) {
        log.info("뉴스레터 발행 리스트 조회 - newsletterId: {}", newsletterId);

        try {
            Integer parsedNewsletterId = parseInteger(newsletterId, "newsletterId");

            Optional<NewsletterMaster> newsletterMaster = newsletterMasterRepository
                .findByNewsletterMasterIdAndIsDeletedFalse(parsedNewsletterId, companyNo);

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

        } catch (NumberFormatException e) {
            log.error("잘못된 newsletterId 형식 - newsletterId: {}", newsletterId, e);
            return new NewsletterIssueListRes(newsletterId, Collections.emptyList());
        } catch (Exception e) {
            log.error("뉴스레터 발행 리스트 조회 중 오류 발생 - newsletterId: {}", newsletterId, e);
            return new NewsletterIssueListRes(newsletterId, Collections.emptyList());
        }
    }

    public NewsletterDetailRes getNewsletterDetail(Integer companyNo, String newsletterId, String issueNo) {
        log.info("뉴스레터 상세 조회 - newsletterId: {}, issueNo: {}", newsletterId, issueNo);

        DateTimeFormatter f1 = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");
        DateTimeFormatter f2 = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try {
            // 1. 입력 파라미터 검증 및 변환
            Integer parsedNewsletterId = parseInteger(newsletterId, "newsletterId");
            Integer parsedIssueNo = parseInteger(issueNo, "issueNo");

            // 2. 뉴스레터 마스터 조회
            NewsletterMaster nm = newsletterMasterRepository
                .findByNewsletterMasterIdAndIsDeletedFalse(parsedNewsletterId, companyNo)
                .orElseThrow(
                    () -> new IllegalArgumentException("Newsletter not found: " + newsletterId));

            // 3. 발행 히스토리 조회
            NewsletterPublishHistory nph = newsletterPublishHistoryRepository
                .findByNewsletterMasterAndIssueNo(nm, parsedIssueNo)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Publish history not found - newsletterId: %s, issueNo: %s",
                        newsletterId, issueNo)));

            List<NewsletterTemplate> templates = newsletterTemplateRepository
                .findByNewsletterTemplateIdOrderByElementOrder(nph.getNewsletterTemplateId());

            Map<Integer, Map<String, String>> templatesByOrder = templates.stream()
                .collect(Collectors.groupingBy(
                    NewsletterTemplate::getElementOrder,
                    Collectors.toMap(
                        NewsletterTemplate::getElementType,
                        NewsletterTemplate::getElementValue,
                        (existing, replacement) -> existing
                    )
                ));

            Map<String, String> mainChartTemplate = templatesByOrder.getOrDefault(1,
                new HashMap<>());
            String newsFlowChartTitle = mainChartTemplate.getOrDefault("MAIN_CHRT_NM",
                "MAIN_CHRT_NM is empty");
            String newsFlowChartCode = mainChartTemplate.getOrDefault("MAIN_CHRT_CODE", "");

            List<NewsletterChart> flowChart = newsletterChartRepository
                .findByNewsletterPublishHistoryAndCode(nph, newsFlowChartCode);

            List<NewsletterEvent> events = newsletterEventRepository
                .findByNewsletterPublishHistoryOrderByEventCreateTime(nph);

            String[] marketTimes = calculateMarketTimes(nph.getPublishDateTime());

            NewsletterDetailRes.ChartData chartData = convertNewsletterChartsToChartData(flowChart,
                marketTimes);

            String aggregationTime = generateAggregationTime(chartData.timestamps());

            NewsFlowChart newsFlowChart = new NewsFlowChart(
                newsFlowChartTitle,
                aggregationTime,
                chartData
            );

            List<Map<String, String>> referenceList = templatesByOrder.entrySet().stream()
                .filter(entry -> entry.getKey() >= 2)
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList();

            List<Reference> references = referenceList.stream()
                .map(referenceMap -> {
                    if (referenceMap.containsKey("RERNC_TABLE_NM")) {
                        return createTableReference(referenceMap, nph);
                    } else if (referenceMap.containsKey("DEDI_CHRT_NM")) {
                        return createDediChartReference(referenceMap, nph);
                    } else if (referenceMap.containsKey("CLTN_CHRT_NM")) {
                        return createCltnChartReference(referenceMap, nph);
                    } else {
                        log.warn("Unknown reference type: {}", referenceMap.keySet());
                        return new Reference("", "", "UNKNOWN", null, null);
                    }
                })
                .filter(reference -> !"UNKNOWN".equals(reference.type())) // 알 수 없는 타입 제외
                .toList();

            // 2. 관련 뉴스
            List<NewsEventItem> newsEventItems = events.stream()
                .map(event -> new NewsEventItem(
                    event.getNewsletterEventId().toString(),  // newsId
                    event.getEventTitle(),                     // title
                    Arrays.asList(event.getEventContent().split("\n")),
                    event.getEventCreateTime().toInstant(ZoneOffset.UTC).toEpochMilli(),
                    event.getEventCreateTime().minusHours(4).format(f2), // createdDate
                    event.getEventMarkTime(),                 // createTime
                    event.getEventProvider(),                 // source
                    "Y".equals(event.getTurningPointMarkYn()),
                    event.getDirectionalityType()// showInChart (변곡점 표시 여부)
                ))
                .toList();

            RelatedNews relatedNews = new RelatedNews(
                aggregationTime,
                newsEventItems
            );

            IssueDetail issueDetail = new IssueDetail(
                nph.getNewsletterTitle(),
                nph.getPublishDateTime().format(f1),
                nph.getAiSummary(),
                marketTimes[0], // marketOpenTime
                marketTimes[1], // marketCloseTime
                newsFlowChart,
                relatedNews,
                references,
                nph.getEmailSubject()
            );

            return new NewsletterDetailRes(
                nm.getNewsletterMasterId().toString(),
                nm.getPeriodType().getCode(),
                nm.getMasterName(),
                nm.getExplanation(),
                false,
                encodeImageToBase64(nm.getImageUrl(), MIME_TYPE),
                true,
                issueDetail);

        } catch (NumberFormatException e) {
            log.error("잘못된 숫자 형식 - newsletterId: {}, issueNo: {}", newsletterId, issueNo, e);
            return createEmptyNewsletterDetailRes(newsletterId);
        } catch (IllegalArgumentException e) {
            log.warn("데이터를 찾을 수 없음: {}", e.getMessage());
            return createEmptyNewsletterDetailRes(newsletterId);
        } catch (Exception e) {
            log.error("뉴스레터 데이터 조회 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            return createEmptyNewsletterDetailRes(newsletterId);
        }
    }

    /**
     * 문자열을 Integer로 안전하게 변환
     */
    private Integer parseInteger(String value, String fieldName) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException(fieldName + "이(가) 비어있습니다.");
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new NumberFormatException(String.format("잘못된 %s 형식: %s", fieldName, value));
        }
    }

    /**
     * 빈 NewsletterDetailRes 응답 생성
     */
    private NewsletterDetailRes createEmptyNewsletterDetailRes(String newsletterId) {
        return new NewsletterDetailRes(
            newsletterId,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    /**
     * NewsletterChart 리스트를 ChartData로 변환
     */
    private NewsletterDetailRes.ChartData convertNewsletterChartsToChartData(
        List<NewsletterChart> flowCharts, String[] marketTimes) {
        List<String> timestamps = new ArrayList<>();
        List<BigDecimal> highValues = new ArrayList<>();
        List<BigDecimal> lowValues = new ArrayList<>();
        List<BigDecimal> openValues = new ArrayList<>();
        List<BigDecimal> closeValues = new ArrayList<>();
        List<BigDecimal> volumeValues = new ArrayList<>();

        try {
            if (flowCharts.isEmpty()) {
                return new NewsletterDetailRes.ChartData(
                    Collections.emptyList(),
                    Collections.emptyList()
                );
            }

            TreeMap<Long, NewsletterChart> chartMap = new TreeMap<>();
            for (NewsletterChart chart : flowCharts) {
                long timestamp = Timestamp.valueOf(chart.getTimestamp()).getTime();
                chartMap.put(timestamp, chart);
            }

            long firstTimestamp = chartMap.keySet().iterator().next();
            long lastTimestamp = chartMap.lastKey();

            long startTimestamp = Long.parseLong(marketTimes[2]);
            long endTimestamp = Long.parseLong(marketTimes[3]);

            // 실제 데이터와 설정된 범위를 비교하여 더 넓은 범위 사용
            long actualStartTimestamp = Math.min(firstTimestamp, startTimestamp);
            long actualEndTimestamp = Math.max(lastTimestamp, endTimestamp);

            // 5분 간격으로 모든 timestamp 생성 (5분 = 300,000 밀리초)
            long interval = 5 * 60 * 1000L;

            for (long currentTimestamp = actualStartTimestamp;
                currentTimestamp <= actualEndTimestamp;
                currentTimestamp += interval) {

                timestamps.add(String.valueOf(currentTimestamp));

                NewsletterChart chart = chartMap.get(currentTimestamp);

                if (chart != null) {
                    BigDecimal high = chart.getHigh1();
                    BigDecimal low = chart.getLow1();
                    BigDecimal open = chart.getOpenPrice();
                    BigDecimal close = chart.getTradePrice1();
                    BigDecimal volume = chart.getNumMoves();

                    highValues.add(high);
                    lowValues.add(low);
                    openValues.add(open);
                    closeValues.add(close);
                    volumeValues.add(volume != null ? volume : BigDecimal.ZERO);
                } else {
                    highValues.add(null);
                    lowValues.add(null);
                    openValues.add(null);
                    closeValues.add(null);
                    volumeValues.add(null);
                }
            }
        } catch (Exception e) {
            log.error("NewsletterChart 데이터 변환 중 오류 발생", e);
            return new NewsletterDetailRes.ChartData(
                Collections.emptyList(),
                Collections.emptyList()
            );
        }

        BigDecimal max = closeValues.stream()
            .filter(Objects::nonNull)
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);

        BigDecimal min = closeValues.stream()
            .filter(Objects::nonNull)
            .min(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);

        String range = calculateRange(max, min, "blue");

        // Series 생성
        List<Series> seriesList = Arrays.asList(
            Series.of("high", "High", RenderAs.HIGH, highValues, true),
            Series.of("low", "Low", RenderAs.LOW, lowValues, true),
            Series.of("open", "Open", RenderAs.OPEN, openValues, true),
            Series.ofWithChartOption("close", "Close", RenderAs.CLOSE, closeValues, true, null, "1",
                range),
            Series.of("volume", "Volume", RenderAs.VOLUME, volumeValues, true)
        );

        return new NewsletterDetailRes.ChartData(timestamps, seriesList);
    }

    private String generateAggregationTime(List<String> timestamps) {
        if (timestamps == null || timestamps.isEmpty()) {
            return "뉴욕시간 전일 20:00부터 당일 17:00까지"; // 기본값
        }

        try {
            // 첫 번째와 마지막 타임스탬프
            String firstTimestamp = timestamps.get(0);
            String lastTimestamp = timestamps.get(timestamps.size() - 1);

            // Unix timestamp (밀리초)를 뉴욕 시간으로 변환
            ZoneId newYorkZone = ZoneId.of("America/New_York");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            String startTime = Instant.ofEpochMilli(Long.parseLong(firstTimestamp))
                .atZone(newYorkZone)
                .format(timeFormatter);

            String endTime = Instant.ofEpochMilli(Long.parseLong(lastTimestamp))
                .atZone(newYorkZone)
                .format(timeFormatter);

            return String.format("뉴욕시간 전일 %s부터 당일 %s까지", startTime, endTime);

        } catch (Exception e) {
            log.error("타임스탬프 변환 중 오류 발생", e);
            return "뉴욕시간 전일 20:00부터 당일 17:00까지";
        }
    }

    private TableRes readTableDataFromJsonString(String jsonString) {
        try {

            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {
            };
            Map<String, Object> jsonData = objectMapper.readValue(jsonString, typeRef);

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

            return TableRes.of("", headers, rows);

        } catch (JsonProcessingException e) {
            log.error("JSON 문자열을 TableRes로 변환하는 중 오류 발생", e);
            return TableRes.of(Collections.emptyList(), Collections.emptyList());
        }
    }

    private Reference createTableReference(Map<String, String> referenceMap,
        NewsletterPublishHistory nph) {
        String title = referenceMap.get("RERNC_TABLE_NM");
        String dataCode = referenceMap.get("RERNC_TABLE_CD");

        try {
            NewsletterTemplatePreferenceTable config =
                newsletterTemplatePreferenceTableRepository.findTopByPreferenceTableCodeAndIsDeletedFalse(dataCode).orElse(null);

            if (config == null) {
                return Reference.ofTable(title, "템플릿 참조 테이블 데이터를 찾을 수 없습니다", null);
            }
            String additionalWhereClause = CombineUtil.createWhereClause("nws_ltr_pub_id", String.valueOf(nph.getNewsletterPublishId()));

            DynamicDataReq dynamicDataReq = DynamicDataReq.builder()
                .targetDataSource(config.getTargetDatabaseName())     // tgt_db_nm
                .tableName(config.getTargetTableName())               // tgt_tbl_nm
                .columns(config.getTableColumnName())                 // tbl_colm_nm
                .whereClause(
                    config.getQueryConditionContent())       // qry_cdtn_cntt
                .additionalWhereClause(additionalWhereClause)
                .build();

            // 동적 쿼리 실행
            List<Map<String, Object>> dataQuery = mybatisDynamicQueryService.getDynamicData(
                dynamicDataReq);

            if (dataQuery.isEmpty()) {
                log.warn("No Reference Table data found : {}", dataCode);
                return Reference.ofTable(title, "데이터를 찾을 수 없습니다", null);
            } else {
                TableRes tableData = readTableDataFromJsonString(
                    dataQuery.get(0).get(config.getTableColumnName()).toString());
                return Reference.ofTable(title, "", tableData);
            }

        } catch (Exception e) {
            log.error("PreferenceTable 조회 중 오류 발생 - code: {}", dataCode, e);
            return Reference.ofTable(title, "데이터 조회 중 오류 발생", null);
        }
    }

    private Reference createDediChartReference(Map<String, String> referenceMap,
        NewsletterPublishHistory nph) {
        String title = referenceMap.get("DEDI_CHRT_NM");
        String description = referenceMap.get("DEDI_CHRT_DECR");
        String dataCode = referenceMap.get("DEDI_CHRT_CD");

        try {
            NewsletterDetailRes.ChartData chartData = getDediChartData(nph, dataCode);
            return Reference.ofChart(title, description, chartData);
        } catch (Exception e) {
            log.error("DEDI 차트 데이터 조회 중 오류 발생 - code: {}", dataCode, e);
            return Reference.ofChart(title, description, null);
        }
    }

    private Reference createCltnChartReference(Map<String, String> referenceMap,
        NewsletterPublishHistory nph) {
        String title = referenceMap.get("CLTN_CHRT_NM");
        String description = referenceMap.get("CLTN_CHRT_DECR");
        String dataCode = referenceMap.get("CLTN_CHRT_CD");

        try {
            NewsletterCollectionChart newsletterCollectionChart = newsletterCollectionChartRepository.findByNewsletterPublishHistoryAndCollectionChartTypeAndIsDeletedFalse(
                    nph, dataCode)
                .orElseThrow(
                    () -> new IllegalArgumentException("NewsletterCollectionChart not found"));

            NewsletterDetailRes.ChartData chartData = getCltnChartData(dataCode,
                newsletterCollectionChart);
            return Reference.ofChart(title, description, chartData);
        } catch (Exception e) {
            log.error("CLTN 차트 데이터 조회 중 오류 발생 - code: {}", dataCode, e);
            return Reference.ofChart(title, description, null);
        }
    }

    private NewsletterDetailRes.ChartData getDediChartData(NewsletterPublishHistory nph,
        String dataCode) {
        List<NewsletterTemplateDedicatedChart> dedicatedChartsConfig =
            newsletterTemplateDedicatedChartRepository.findByDedicatedChartTypeAndIsDeleted(
                dataCode,
                false);

        // 1. 모든 데이터를 timestamp 기준으로 수집
        Map<String, Map<String, BigDecimal>> timeSeriesData = new TreeMap<>();
        List<Series> allSeries = new ArrayList<>();

        for (NewsletterTemplateDedicatedChart config : dedicatedChartsConfig) {
            List<NewsletterChart> chartData = newsletterChartRepository
                .findByNewsletterPublishHistoryAndCode(nph, config.getCode());

            // timestamp별로 데이터 정리
            for (NewsletterChart chart : chartData) {
                String timestamp = String.valueOf(
                    Timestamp.valueOf(chart.getTimestamp()).getTime());
                BigDecimal value = chart.getTradePrice1();

                timeSeriesData.computeIfAbsent(timestamp, k -> new HashMap<>())
                    .put(config.getCode(), value);
            }
        }

        // 2. 정렬된 timestamp로 Series 생성
        List<String> timestamps = new ArrayList<>(timeSeriesData.keySet());

        for (NewsletterTemplateDedicatedChart config : dedicatedChartsConfig) {
            List<BigDecimal> values = timestamps.stream()
                .map(ts -> timeSeriesData.get(ts).get(config.getCode()))
                .toList();

            BigDecimal max = values.stream()
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
            BigDecimal min = values.stream()
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

            String range = calculateRange(max, min, config.getColor());

            if (!values.isEmpty()) {
                Series series = Series.ofWithChartOption(
                    config.getColor(),
                    config.getColor(),
                    RenderAs.CLOSE,
                    values,
                    true,
                    config.getColor(),
                    config.getDtick(),
                    range);
                allSeries.add(series);
            }
        }

        return NewsletterDetailRes.ChartData.of(timestamps, allSeries);
    }

    private String calculateRange(BigDecimal max, BigDecimal min, String color) {
        BigDecimal difference = max.subtract(min);
        BigDecimal compare = BigDecimal.valueOf(color.equals("blue") ? 4 : 2);
        double range = color.equals("blue") ? 1 : 0.5;

        BigDecimal adjustedMax = BigDecimal.valueOf(Math.ceil(max.doubleValue()));
        BigDecimal adjustedMin = BigDecimal.valueOf(Math.floor(min.doubleValue()));

        BigDecimal resultMax;
        BigDecimal resultMin;

        if (difference.compareTo(compare) < 0) {
            resultMax = adjustedMax.add(BigDecimal.valueOf(range));
            resultMin = adjustedMin.subtract(BigDecimal.valueOf(range));
        } else {
            resultMax = adjustedMax;
            resultMin = adjustedMin;
        }

        return String.format("[%s, %s]", resultMax, resultMin);
    }

    private NewsletterDetailRes.ChartData getCltnChartData(
        String dataCode, NewsletterCollectionChart newsletterCollectionChart) {

        List<NewsletterTemplateCollectionChart> cltnChartConfigs =
            newsletterTemplateCollectionChartRepository.findByCollectionChartTypeAndIsDeleted(
                dataCode, false);

        if (cltnChartConfigs.isEmpty()) {
            log.warn("No collection chart config found for dataCode: {}", dataCode);
            return null;
        }

        // 1. 모든 데이터를 timestamp 기준으로 수집
        Map<String, Map<String, BigDecimal>> timeSeriesData = new TreeMap<>();
        List<Series> allSeries = new ArrayList<>();
        DateTimeFormatter f3 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (NewsletterTemplateCollectionChart config : cltnChartConfigs) {
            try {
                String col1 = config.getTableColumnName().split(",")[0];
                String col2 = config.getTableColumnName().split(",")[1];

                String additionalWhereClause = CombineUtil.createBetweenClause(col1,
                    newsletterCollectionChart.getStartDate().format(f3),
                    newsletterCollectionChart.getEndDate().format(f3));

                DynamicDataReq dynamicDataReq = DynamicDataReq.builder()
                    .targetDataSource(config.getTargetDatabaseName())     // tgt_db_nm
                    .tableName(config.getTargetTableName())               // tgt_tbl_nm
                    .columns(config.getTableColumnName())                 // tbl_colm_nm
                    .whereClause(config.getQueryConditionContent())       // qry_cdtn_cntt
                    .additionalWhereClause(additionalWhereClause)
                    .build();

                // 동적 쿼리 실행
                List<Map<String, Object>> dataQuery = mybatisDynamicQueryService.getDynamicData(
                    dynamicDataReq);

                if (dataQuery.isEmpty()) {
                    log.warn("No chart data found for collection chart type: {}",
                        config.getCollectionChartType());
                    continue;
                }

                // timestamp별로 데이터 정리
                for (Map<String, Object> data : dataQuery) {
                    LocalDateTime dateTime = data.get(col1) != null
                        ? ((java.sql.Timestamp) data.get(col1)).toLocalDateTime()
                        : null;
                    BigDecimal value = (BigDecimal) data.get(col2);

                    if (dateTime != null && value != null) {
                        String timestamp = String.valueOf(
                            Timestamp.valueOf(dateTime).getTime());

                        timeSeriesData.computeIfAbsent(timestamp, k -> new HashMap<>())
                            .put(config.getColor(), value);
                    }
                }

            } catch (Exception e) {
                log.error("Error processing chart config - color: {}", config.getColor(), e);
            }
        }

        if (timeSeriesData.isEmpty()) {
            log.warn("No valid time series data found for dataCode: {}", dataCode);
            return null;
        }

        // 2. 정렬된 timestamp로 Series 생성
        List<String> timestamps = new ArrayList<>(timeSeriesData.keySet());

        for (NewsletterTemplateCollectionChart config : cltnChartConfigs) {
            List<BigDecimal> values = timestamps.stream()
                .map(ts -> timeSeriesData.get(ts).get(config.getColor()))
                .filter(Objects::nonNull)
                .toList();

            if (!values.isEmpty()) {
                Series series = Series.ofWithChartOption(
                    config.getColor(),  // seriesId
                    config.getColor(),  // label
                    RenderAs.CLOSE,
                    values,
                    true,
                    config.getColor(),
                    config.getDtick(),
                    config.getRange()
                );
                allSeries.add(series);
            }
        }

        return NewsletterDetailRes.ChartData.of(timestamps, allSeries);
    }

    private String[] calculateMarketTimes(LocalDateTime publishDateTime) {
        try {
            ZoneId newYorkZone = ZoneId.of("America/New_York");

            LocalDate today = publishDateTime.toLocalDate();

            // today를 뉴욕 시간대로 변환하여 DST 확인
            ZonedDateTime todayInNY = today.atStartOfDay(newYorkZone);

            // 개장 시간
            ZonedDateTime startTime = todayInNY
                .withHour(9)
                .withMinute(30)
                .withSecond(0)
                .withNano(0);

            // 폐장 시간
            ZonedDateTime endTime = todayInNY
                .withHour(17)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

            // 그래프 x축 범위 계산
            // 전일 20시
            ZonedDateTime chartStartTime = todayInNY
                .minusDays(1)
                .withHour(20)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

            // 당일 17시
            ZonedDateTime chartEndTime = todayInNY
                .withHour(17)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

            // Unix timestamp (milliseconds)로 변환
            return new String[]{
                String.valueOf(startTime.toInstant().toEpochMilli()),      // 개장 시간
                String.valueOf(endTime.toInstant().toEpochMilli()),        // 폐장 시간
                String.valueOf(chartStartTime.toInstant().toEpochMilli()), // 그래프 시작 (전일 20시)
                String.valueOf(chartEndTime.toInstant().toEpochMilli())    // 그래프 종료 (당일 18시)
            };

        } catch (Exception e) {
            log.error("주식 시장 시간 계산 중 오류 발생", e);
            // 기본값 반환 (EST 기준)
            return new String[]{"1704110200000", "1704133200000", "1704110200000", "1704133200000"};
        }
    }
}
