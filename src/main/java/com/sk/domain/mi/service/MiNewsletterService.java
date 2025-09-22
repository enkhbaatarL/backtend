package com.sk.domain.mi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.model.PaginationRes;
import com.sk.common.model.TableRes;
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
import com.sk.domain.shared.entity.master.tmd.NewsletterEvent;
import com.sk.domain.shared.entity.master.tmd.NewsletterMaster;
import com.sk.domain.shared.entity.master.tmd.NewsletterPublishHistory;
import com.sk.domain.shared.entity.master.tmd.NewsletterTemplate;
import com.sk.domain.shared.entity.master.tmd.NewsletterTemplateDedicatedChart;
import com.sk.domain.shared.entity.master.tmd.PreferenceTable;
import com.sk.domain.shared.enums.chart.RenderAs;
import com.sk.domain.shared.enums.newsletter.PublishStatusCode;
import com.sk.domain.shared.repository.master.tif.NewsletterChartRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterEventRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterMasterRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterPublishHistoryRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterTemplateDedicatedChartRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterTemplateRepository;
import com.sk.domain.shared.repository.master.tmd.PreferenceTableRepository;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
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
import java.util.Set;
import java.util.TreeSet;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final NewsletterMasterRepository newsletterMasterRepository;
    private final NewsletterPublishHistoryRepository newsletterPublishHistoryRepository;
    private final NewsletterTemplateRepository newsletterTemplateRepository;
    private final NewsletterEventRepository newsletterEventRepository;
    private final NewsletterChartRepository newsletterChartRepository;
    private final PreferenceTableRepository preferenceTableRepository;
    private final NewsletterTemplateDedicatedChartRepository newsletterTemplateDedicatedChartRepository;

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
     * 뉴스레터 발행 리스트 조회
     */
    public NewsletterIssueListRes getNewsletterIssueList(String newsletterId) {
        log.info("뉴스레터 발행 리스트 조회 - newsletterId: {}", newsletterId);

        try {
            Integer parsedNewsletterId = parseInteger(newsletterId, "newsletterId");

            Optional<NewsletterMaster> newsletterMaster = newsletterMasterRepository
                .findByNewsletterMasterIdAndIsDeletedFalse(parsedNewsletterId);

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

    public NewsletterDetailRes getNewsletterDetail(String newsletterId, String issueNo) {
        log.info("뉴스레터 상세 조회 - newsletterId: {}, issueNo: {}", newsletterId, issueNo);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");
        DateTimeFormatter f2 = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try {
            // 1. 입력 파라미터 검증 및 변환
            Integer parsedNewsletterId = parseInteger(newsletterId, "newsletterId");
            Integer parsedIssueNo = parseInteger(issueNo, "issueNo");

            // 2. 뉴스레터 마스터 조회
            NewsletterMaster nm = newsletterMasterRepository
                .findByNewsletterMasterIdAndIsDeletedFalse(parsedNewsletterId)
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

            NewsletterDetailRes.ChartData chartData = convertNewsletterChartsToChartData(flowChart);
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
                    // 어떤 타입의 reference인지 먼저 판별
                    if (referenceMap.containsKey("RERNC_TABLE_NM")) {
                        // TABLE 타입 처리
                        return createTableReference(referenceMap, nph);
                    } else if (referenceMap.containsKey("DEDI_CHRT_NM")) {
                        // DEDI 차트 타입 처리
                        return createDediChartReference(referenceMap, nph);
                    } else if (referenceMap.containsKey("CLTN_CHRT_NM")) {
                        // CLTN 차트 타입 처리
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
                nph.getPublishDateTime().format(formatter),
                nph.getAiSummary(),
                newsFlowChart,
                relatedNews,
                references
            );

            return new NewsletterDetailRes(
                nm.getNewsletterMasterId().toString(),
                nm.getPeriodType().getCode(),
                nm.getMasterName(),
                nm.getExplanation(),
                false,
                encodeImageToBase64(nm.getImageUrl(), "image/png"),
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
        List<NewsletterChart> flowCharts) {
        List<String> timestamps = new ArrayList<>();
        List<BigDecimal> highValues = new ArrayList<>();
        List<BigDecimal> lowValues = new ArrayList<>();
        List<BigDecimal> openValues = new ArrayList<>();
        List<BigDecimal> closeValues = new ArrayList<>();
        List<BigDecimal> volumeValues = new ArrayList<>();

        try {
            for (NewsletterChart chart : flowCharts) {
                Long timestamp = Timestamp.valueOf(chart.getTimestamp()).getTime();
                BigDecimal high = chart.getHigh1();           // 고가
                BigDecimal low = chart.getLow1();             // 저가
                BigDecimal open = chart.getOpenPrice();       // 시가
                BigDecimal close = chart.getTradePrice1();    // 종가
                BigDecimal volume = chart.getNumMoves();      // 거래량

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
            return "뉴욕시간 전일 20:00부터 당일 17:00까지"; // 오류 시 기본값
        }
    }

    private TableRes readTableDataFromJsonString(String jsonString) {
        try {
            // JSON 문자열을 Map으로 파싱
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {
            };
            Map<String, Object> jsonData = objectMapper.readValue(jsonString, typeRef);

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

        } catch (JsonProcessingException e) {
            log.error("JSON 문자열을 TableRes로 변환하는 중 오류 발생", e);
            // 오류 시 빈 테이블 반환
            return TableRes.of(Collections.emptyList(), Collections.emptyList());
        }
    }

    private NewsletterDetailRes.ChartData convertTwoChartsToChartData(
        List<NewsletterChart> flowCharts1,
        List<NewsletterChart> flowCharts2,
        String series1Name,
        String series2Name) {

        List<String> timestamps = new ArrayList<>();
        List<BigDecimal> closeValues1 = new ArrayList<>();
        List<BigDecimal> closeValues2 = new ArrayList<>();

        try {
            // flowCharts1에서 timestamp와 close 값 추출
            for (NewsletterChart chart : flowCharts1) {
                Long timestamp = chart.getTimestamp().toInstant(ZoneOffset.UTC).toEpochMilli();
                BigDecimal close = chart.getTradePrice1();    // 종가

                if (timestamp != null && close != null) {
                    timestamps.add(timestamp.toString());
                    closeValues1.add(close);
                }
            }

            // flowCharts2에서 close 값만 추출 (timestamp는 같다고 가정)
            for (NewsletterChart chart : flowCharts2) {
                BigDecimal close = chart.getTradePrice1();    // 종가
                if (close != null) {
                    closeValues2.add(close);
                }
            }

            // 두 리스트의 크기가 다르면 작은 쪽에 맞춤
            int minSize = Math.min(closeValues1.size(), closeValues2.size());
            if (timestamps.size() > minSize) {
                timestamps = timestamps.subList(0, minSize);
                closeValues1 = closeValues1.subList(0, minSize);
                closeValues2 = closeValues2.subList(0, minSize);
            }

        } catch (Exception e) {
            log.error("두 NewsletterChart 데이터 변환 중 오류 발생", e);
            return new NewsletterDetailRes.ChartData(
                Collections.emptyList(),
                Collections.emptyList()
            );
        }

        // Series 2개만 생성
        List<Series> seriesList = Arrays.asList(
            Series.of("close1", series1Name, RenderAs.CLOSE, closeValues1, true),
            Series.of("close2", series2Name, RenderAs.CLOSE, closeValues2, true)
        );

        return new NewsletterDetailRes.ChartData(timestamps, seriesList);
    }

    private Reference createTableReference(Map<String, String> referenceMap,
        NewsletterPublishHistory nph) {
        String title = referenceMap.get("RERNC_TABLE_NM");
        String dataCode = referenceMap.get("RERNC_TABLE_CD");

        try {
            Optional<PreferenceTable> preferenceTableOpt =
                preferenceTableRepository.findByNewsletterPublishHistoryAndPreferenceTableCode(nph,
                    dataCode);

            if (preferenceTableOpt.isPresent()) {
                TableRes tableData = readTableDataFromJsonString(
                    preferenceTableOpt.get().getJsonValue());
                return Reference.ofTable(title, "", tableData);
            } else {
                log.warn("PreferenceTable not found for code: {}", dataCode);
                return Reference.ofTable(title, "데이터를 찾을 수 없습니다", null);
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
            // 기존 로직을 그대로 사용하거나 dataCode에 따라 다르게 처리
            if ("CLc1".equals(dataCode)) {
                List<NewsletterChart> c1 = newsletterChartRepository
                    .findByNewsletterPublishHistoryAndCode(nph, "CLc1");
                List<NewsletterChart> c2 = newsletterChartRepository
                    .findByNewsletterPublishHistoryAndCode(nph, ".DXY");

                NewsletterDetailRes.ChartData chartData = convertTwoChartsToChartData(c1, c2, "WTI",
                    "DI");
                return Reference.ofChart(title, description, chartData);
            } else {
                // 다른 CLTN 차트 코드에 대한 처리
                NewsletterDetailRes.ChartData chartData = getCltnChartData(nph, dataCode);
                return Reference.ofChart(title, description, chartData);
            }
        } catch (Exception e) {
            log.error("CLTN 차트 데이터 조회 중 오류 발생 - code: {}", dataCode, e);
            return Reference.ofChart(title, description, null);
        }
    }

    private NewsletterDetailRes.ChartData getDediChartData(NewsletterPublishHistory nph,
        String dataCode) {
        List<NewsletterTemplateDedicatedChart> dedicatedChartsConfig =
            newsletterTemplateDedicatedChartRepository.findByDedicatedChartTypeAndDeleteYn(dataCode,
                "N");

        if (dedicatedChartsConfig.isEmpty()) {
            log.warn("No dedicated chart config found for dataCode: {}", dataCode);
            return null;
        }

        Set<String> timestampSet = new TreeSet<>();
        List<Series> allSeries = new ArrayList<>();

                for (NewsletterTemplateDedicatedChart config : dedicatedChartsConfig) {
            try {
                List<NewsletterChart> chartData = newsletterChartRepository
                    .findByNewsletterPublishHistoryAndCode(nph, config.getCode());

                if (chartData.isEmpty()) {
                    log.warn("No chart data found for code: {}", config.getCode());
                    continue;
                }


                Series series = convertNewsletterChartToSeries(
                    chartData,
                    config.getCode(),
                    config.getColor()
                );

                if (series != null) {
                    allSeries.add(series);


                    chartData.forEach(chart -> {
                        String timestamp = extractTimestampFromChart(chart);
                        if (timestamp != null) {
                            timestampSet.add(timestamp);
                        }
                    });
                }

            } catch (Exception e) {
                log.error("Error processing chart config - code: {}", config.getCode(), e);
            }
        }

        if (allSeries.isEmpty()) {
            log.warn("No valid series data found for dataCode: {}", dataCode);
            return null;
        }

        List<String> timestamps = new ArrayList<>(timestampSet);

        return NewsletterDetailRes.ChartData.of(timestamps, allSeries);
    }


    private Series convertNewsletterChartToSeries(
        List<NewsletterChart> chartData,
        String label,
        String color) {

        if (chartData == null || chartData.isEmpty()) {
            return null;
        }

        List<BigDecimal> values = chartData.stream()
            .map(this::extractValueFromChart)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if (values.isEmpty()) {
            return null;
        }

        return Series.of(
            color,             // seriesId
            label,             // label
            RenderAs.CLOSE,     //
            values,            // values
            true               // isVisible
        );
    }

    private String extractTimestampFromChart(NewsletterChart chart) {
        return String.valueOf(chart.getTimestamp().toInstant(ZoneOffset.UTC).toEpochMilli());
    }

    // NewsletterChart에서 value 추출 (실제 구조에 맞게 구현 필요)
    private BigDecimal extractValueFromChart(NewsletterChart chart) {
        return chart.getTradePrice1();
    }


    private NewsletterDetailRes.ChartData getCltnChartData(NewsletterPublishHistory nph,
        String dataCode) {
        // TODO: CLTN 차트 데이터 조회 로직 구현
        return null;
    }
}
