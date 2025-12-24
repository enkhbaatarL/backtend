package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode.Error;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.DateUtil;
import com.sk.domain.mi.dto.response.news.MarketNewsDetailRes;
import com.sk.domain.mi.dto.response.news.MarketNewsListRes;
import com.sk.domain.mi.dto.response.news.MarketRecapRes;
import com.sk.domain.mi.dto.response.news.MarketRecapRes.Narrative;
import com.sk.domain.mi.dto.response.news.MarketRecapRes.SignalNewsCount;
import com.sk.domain.mi.dto.response.news.NewsTimelineRes;
import com.sk.domain.mi.dto.response.news.NewsfeedListRes;
import com.sk.domain.mi.dto.response.news.NewsfeedListRes.SimilarNewsItem;
import com.sk.domain.shared.entity.master.tmd.MarketRecap;
import com.sk.domain.shared.entity.master.tmd.MarketRecapCheckpoint;
import com.sk.domain.shared.entity.master.tmd.MarketRecapHighlightEvent;
import com.sk.domain.shared.entity.master.tmd.MarketRecapHighlightEventRelationship;
import com.sk.domain.shared.entity.master.tmd.NewsEvent;
import com.sk.domain.shared.entity.master.tmd.NewsSignificance;
import com.sk.domain.shared.entity.master.tmd.NewsSummary;
import com.sk.domain.shared.enums.news.SortType;
import com.sk.domain.shared.repository.master.tmd.MarketRecapCheckpointRepository;
import com.sk.domain.shared.repository.master.tmd.MarketRecapHighlightEventRelationshipRepository;
import com.sk.domain.shared.repository.master.tmd.MarketRecapHighlightEventRepository;
import com.sk.domain.shared.repository.master.tmd.MarketRecapRepository;
import com.sk.domain.shared.repository.master.tmd.NewsEventRepository;
import com.sk.domain.shared.repository.master.tmd.NewsSignificanceRepository;
import com.sk.domain.shared.repository.master.tmd.NewsSummaryRepository;
import com.sk.domain.shared.repository.master.tmd.SignificantNewsRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MiMarketNewsService {

    private final NewsSummaryRepository newsSummaryRepository;
    private final NewsSignificanceRepository newsSignificanceRepository;
    private final MarketRecapRepository marketRecapRepository;
    private final MarketRecapHighlightEventRepository marketRecapHighlightEventRepository;
    private final MarketRecapCheckpointRepository marketRecapCheckpointRepository;
    private final MarketRecapHighlightEventRelationshipRepository marketRecapHighlightEventRelationshipRepository;
    private final NewsEventRepository newsEventRepository;
    private final SignificantNewsRepository significantNewsRepository;


    private boolean isMonday(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            throw new IllegalArgumentException("날짜 문자열이 null이거나 비어있습니다.");
        }

        LocalDate date = DateUtil.fromYmd(dateStr);

        return date.getDayOfWeek() == DayOfWeek.MONDAY;
    }

    private String toUtcEpochMillis(LocalDateTime localDateTime) {
        ZonedDateTime utcZonedDateTime = localDateTime.atZone(ZoneId.of("UTC"));
        return String.valueOf(utcZonedDateTime.toInstant().toEpochMilli());
    }

    private List<String> getTimestampRange(LocalDate date, boolean isMonday) {
        if (date == null) {
            throw new IllegalArgumentException("날짜가 null입니다.");
        }

        if (isMonday) {
            LocalDate friday = date.minusDays(3);
            return List.of(
                toUtcEpochMillis(friday.atTime(LocalTime.MIDNIGHT)),
                toUtcEpochMillis(date.atTime(LocalTime.MIDNIGHT))
            );
        } else {
            LocalDate previousDay = date.minusDays(1);
            return List.of(
                toUtcEpochMillis(previousDay.atTime(LocalTime.MIDNIGHT)),
                toUtcEpochMillis(date.atTime(LocalTime.MIDNIGHT))
            );
        }
    }

    private List<String> getTimestampRangeFromDateString(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            throw new IllegalArgumentException("날짜 문자열이 null이거나 비어있습니다.");
        }

        LocalDate date = DateUtil.fromYmd(dateStr);

        boolean monday = date.getDayOfWeek() == DayOfWeek.MONDAY;
        return getTimestampRange(date, monday);
    }

    /**
     * Market News 목록 조회
     */
    public PaginationRes<MarketNewsListRes> getMarketNewsList(
        int page,
        int size,
        SortType sort,
        String keyword,
        LocalDate startDate,
        LocalDate endDate,
        String categories,
        String regions,
        String industries) {

        String sortDirection = (sort == SortType.OLDEST) ? "ASC" : "DESC";

        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : null;

        int offset = (page - 1) * size;
        List<String> categoryList = (categories != null && !categories.isBlank())
            ? Arrays.stream(categories.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList()
            : null;
        List<String> regionList =
            (regions != null && !regions.isBlank()) ? Arrays.stream(regions.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList() : null;
        List<String> industryList =
            (industries != null && !industries.isBlank()) ? Arrays.stream(industries.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList() : null;

        boolean filterByCategory = categories != null && !categoryList.isEmpty();
        boolean filterByRegion = regionList != null && !regionList.isEmpty();
        boolean filterByIndustry = industries != null && !industryList.isEmpty();

        List<Object[]> results = newsSignificanceRepository.findMarketNewsListWithGroupConcat(
            keyword, startDateTime, endDateTime,
            filterByCategory ? categoryList : null, filterByCategory,
            filterByRegion ? regionList : null, filterByRegion,
            filterByIndustry ? industryList : null, filterByIndustry,
            sortDirection, size, offset);

        long totalCount = newsSignificanceRepository.countMarketNewsList(
            keyword, startDateTime, endDateTime,
            filterByCategory ? categoryList : null, filterByCategory,
            filterByRegion ? regionList : null, filterByRegion,
            filterByIndustry ? industryList : null, filterByIndustry);

        List<MarketNewsListRes> content = results.stream()
            .map(result -> {
                Integer newsSummaryId = result[0] != null
                    ? ((Number) result[0]).intValue()
                    : null;
                Integer dataRegistrationSn = result[1] != null
                    ? ((Number) result[1]).intValue()
                    : null;
                LocalDateTime firstCreated = result[2] != null
                    ? ((java.sql.Timestamp) result[2]).toLocalDateTime()
                    : null;
                String title = (String) result[3];
                String providerId = (String) result[4];  // provider → providerId
                String categoryStr = (String) result[5];
                String regionsStr = (String) result[6];
                String industriesStr = (String) result[7];

                List<String> cList = parseGroupConcatResult(categoryStr);
                List<String> rList = parseGroupConcatResult(regionsStr);
                List<String> iList = parseGroupConcatResult(industriesStr);

                return new MarketNewsListRes(
                    newsSummaryId,
                    dataRegistrationSn,
                    firstCreated,
                    title,
                    providerId,  // provider → providerId
                    cList,
                    rList,
                    iList
                );
            })
            .toList();

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<MarketNewsListRes> newsListPage = new org.springframework.data.domain.PageImpl<>(
            content, pageable, totalCount);

        return PaginationRes.toRes(newsListPage);
    }

    /**
     * Newsfeed 목록 조회
     */
    public List<NewsfeedListRes> getNewsfeedList(
        SortType sort,
        String criteriaDate,
        String category,
        String sentiments,
        String impactScores,
        String regions
    ) {

        String sortField = "firstCreated";
        String sortDirection = "DESC";

        switch (sort) {
            case OLDEST:
                sortField = "firstCreated";
                sortDirection = "ASC";
                break;
            case NEWEST:
                sortField = "firstCreated";
                sortDirection = "DESC";
                break;
            case HIGHEST:
                sortField = "impctScre";
                sortDirection = "DESC";
                break;
            case LOWEST:
                sortField = "impctScre";
                sortDirection = "ASC";
                break;
            default:
        }

        List<String> sentimentList = sentiments != null ? Arrays.stream(sentiments.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList(): null;
        List<String> impactScoreList = impactScores != null ? Arrays.stream(impactScores.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList(): null;
        List<String> regionList = regions != null ? Arrays.stream(regions.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList(): null;

        boolean filterBySentiment = sentiments != null && !sentimentList.isEmpty();
        boolean filterByImpactScore = impactScores != null && !impactScoreList.isEmpty();
        boolean filterByRegion = regions != null && !regionList.isEmpty();

        List<Object[]> results = newsSignificanceRepository.findNewsfeedWithGroupConcat(
            criteriaDate, category,
            filterBySentiment ? sentimentList : null, filterBySentiment,
            filterByImpactScore ? impactScoreList : null, filterByImpactScore,
            filterByRegion ? regionList : null, filterByRegion,
            sortField, sortDirection
        );

        if (results.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> headNewsSummaryIds = results.stream()
                .filter(row -> "HEAD".equals(String.valueOf(row[8])))
                .map(row -> ((Number) row[7]).longValue()) // nws_smry_id
                .toList();

        Map<Long, List<SimilarNewsItem>> similarNewsMap = new HashMap<>();
        if (!headNewsSummaryIds.isEmpty()) {
            List<Object[]> similarResults = newsSignificanceRepository
                    .findSimilarNewsByHeadIds(headNewsSummaryIds);

            // headNewsSummaryId별로 그룹핑
            for (Object[] row : similarResults) {
                Long headNewsSummaryId = ((Number) row[1]).longValue();

                SimilarNewsItem item = new SimilarNewsItem(
                    ((Number) row[2]).longValue(), // incl_nws_smry_id
                    (String) row[3],                // title
                    ((java.sql.Timestamp) row[4]).toLocalDateTime(), // first_created
                    ((Number) row[5]).longValue()   // data_reg_sn
                );

                similarNewsMap
                        .computeIfAbsent(headNewsSummaryId, k -> new ArrayList<>())
                        .add(item);
            }
        }

        return results.stream()
                .map(row -> convertToNewsfeedListRes(row, similarNewsMap))
                .toList();
    }

    /**
     * 점수 기반 방향성 계산 (significantNewsScore)
     */
    private String calculateDirectionality(BigDecimal score) {
        // 0.3 <= Upward <= 1.0
        // -1.0 <= Downward <= -0.3
        // -0.3 < Neutral < 0.3
        // 그 외: Out of Range (규격 외 값)

        if (score == null) {
            return "NULL";
        }

        // 1. 규격 외 값 체크 (절대값이 1.0을 초과하는 경우)
        if (score.compareTo(new BigDecimal("1.0")) > 0 ||
            score.compareTo(new BigDecimal("-1.0")) < 0) {
            return "OUTOFRANGE";
        }

        // 2. 방향성 판단
        if (score.compareTo(new BigDecimal("0.3")) >= 0) {
            return "UPWARD";
        } else if (score.compareTo(new BigDecimal("-0.3")) <= 0) {
            return "DOWNWARD";
        } else {
            return "NEUTRAL";
        }
    }

    /**
     * 이벤트 점수 기반 방향성 계산 (Event Score)
     */
    private String calculateEventDirectionality(BigDecimal score) {

        // 기준은 나중에 전달받아서 변경 예정
        // 3.0 <= Upward <= 10.0
        // -10.0 <= Downward <= -3.0
        // -3.0 < Neutral < 3.0
        // 그 외: Out of Range (규격 외 값)

        if (score == null) {
            return "NULL";
        }

        // 1. 규격 외 값 체크 (절대값이 1.0을 초과하는 경우)
        if (score.compareTo(new BigDecimal("10.0")) > 0 ||
            score.compareTo(new BigDecimal("-10.0")) < 0) {
            return "OUTOFRANGE";
        }

        // 2. 방향성 판단
        if (score.compareTo(new BigDecimal("3.0")) >= 0) {
            return "UPWARD";
        } else if (score.compareTo(new BigDecimal("-3.0")) <= 0) {
            return "DOWNWARD";
        } else {
            return "NEUTRAL";
        }
    }

    /**
     * GROUP_CONCAT 결과 파싱
     */
    private List<String> parseGroupConcatResult(String groupConcatStr) {
        if (groupConcatStr == null || groupConcatStr.isBlank()) {
            return List.of();
        }
        return Arrays.stream(groupConcatStr.split(","))
            .map(String::trim)
            .distinct()
            .toList();
    }

    /**
     * Market Recap 조회 (기준일자 + 카테고리)
     */
    public MarketRecapRes getMarketRecapByCriteriaDateAndCategory(
        String criteriaDate, String category) {

        return marketRecapRepository
            .findByCrtrDtAndNwsMrktCtgrCd(criteriaDate, category)
            .map(this::toMarketRecapRes)
            .orElseGet(MarketRecapRes::empty);
    }

    /**
     * Market Recap 조회 (ID)
     */
    public MarketRecapRes getMarketRecapById(Long mrktRcapId) {
        MarketRecap marketRecap = marketRecapRepository.findById(mrktRcapId)
            .orElseThrow(() -> AppErrorException.of(Error.RSC_DATA_NOT_FOUND,
                "MarketRecap not found for id: %s".formatted(mrktRcapId)));

        return toMarketRecapRes(marketRecap);
    }

    /**
     * Market News 상세 조회
     */
    public MarketNewsDetailRes getMarketNewsDetail(String newsSummaryId) {
        Long id = Long.parseLong(newsSummaryId);

        NewsSummary newsData = newsSummaryRepository.findById(id)
            .orElseThrow(() -> AppErrorException.of(Error.RSC_DATA_NOT_FOUND,
                "NewsSummary not found for id: %s".formatted(newsSummaryId)));

        List<String> tagList = new ArrayList<>();

        List<NewsSignificance> newsSignificances = newsSignificanceRepository.findByNewsSummaryId(
            id
        );

        if (newsSignificances != null && !newsSignificances.isEmpty()) {
            newsSignificances.forEach(newsSignificance ->
                tagList.add(newsSignificance.getNwsMrktCtgrCd().getName())
            );
        }

        return toMarketNewsDetailRes(newsData, tagList);
    }

    /**
     * NewsSummary -> MarketNewsDetailRes 변환
     */
    private MarketNewsDetailRes toMarketNewsDetailRes(NewsSummary newsSummary,
        List<String> tagList) {
        return new MarketNewsDetailRes(
            newsSummary.getNewsSummaryId(),
            newsSummary.getRefinitivNewsData().getId().getDataRegistrationSn(),
            newsSummary.getRefinitivNewsData().getId().getFirstCreated(),
            newsSummary.getRefinitivNewsData().getTitle(),
            newsSummary.getRefinitivNewsData().getStory(),
            newsSummary.getProviderId(),  // provider → providerId
            tagList
        );
    }

    /**
     * MarketRecap -> MarketRecapRes 변환
     */
    private MarketRecapRes toMarketRecapRes(MarketRecap marketRecap) {
        // 하이라이트 이벤트 조회
        List<MarketRecapHighlightEvent> highlightEvents =
            marketRecapHighlightEventRepository.findByMarketRecap(marketRecap);

        // 체크포인트 조회
        List<MarketRecapCheckpoint> checkpoints =
            marketRecapCheckpointRepository.findByMarketRecap(marketRecap);

        List<MarketRecapRes.RelatedEventRepresentativeNewsRes> hrList = new ArrayList<>();

        // 하이라이트 이벤트 DTO 변환
        List<MarketRecapRes.HighlightEventRes> highlightEventResList = highlightEvents.stream()
            .map(highlightEvent -> {
                List<MarketRecapHighlightEventRelationship> relationships =
                    marketRecapHighlightEventRelationshipRepository
                        .findByMarketRecapHighlightEvent(highlightEvent);

                List<MarketRecapRes.RelatedEventRepresentativeNewsRes> relatedNewsList = relationships.stream()
                    .map(rel -> {
                        NewsEvent newsEvent = rel.getNewsEvent();
                        NewsSummary representativeNews = newsEvent.getRepresentativeNewsSummary();

                        if (representativeNews == null) {
                            return null;
                        }

                        return new MarketRecapRes.RelatedEventRepresentativeNewsRes(
                            representativeNews.getNewsSummaryId(),
                            representativeNews.getRefinitivNewsData() != null
                                ? representativeNews.getRefinitivNewsData().getTitle() : null,
                            representativeNews.getProviderId()
                        );
                    })
                    .filter(Objects::nonNull)
                    .toList();

                hrList.addAll(relatedNewsList);

                // momentum 계산 (score 기반)
                String momentum = calculateEventDirectionality(highlightEvent.getScore());

                return new MarketRecapRes.HighlightEventRes(
                    highlightEvent.getMrktRcapHlEvntId(),
                    highlightEvent.getNameKr(),
                    highlightEvent.getNameEn(),
                    momentum,
                    relatedNewsList
                );
            })
            .filter(highlightEventRes ->
                highlightEventRes.relatedEventRepresentativeNewsResNews() != null &&
                !highlightEventRes.relatedEventRepresentativeNewsResNews().isEmpty())
            .toList();

        // 체크포인트 DTO 변환
        List<MarketRecapRes.CheckpointRes> checkpointResList = checkpoints.stream()
            .map(checkpoint -> new MarketRecapRes.CheckpointRes(
                checkpoint.getMrktRcapCkptId(),
                checkpoint.getTitle(),
                checkpoint.getContent() != null ?
                    Arrays.asList(checkpoint.getContent().split("\n")) : List.of()
            ))
            .toList();

        // Sentiment 카운트 계산 (SignificantNews 기반)
        MarketRecapRes.Sentiments sentiments = calculateSentimentCount(marketRecap.getCrtrDt(), marketRecap.getNwsMrktCtgrCd());

        MarketRecapRes.Highlights highlights = new MarketRecapRes.Highlights(
            new Narrative(marketRecap.getHlEvntTitl(),
                marketRecap.getHlEvntCntt() != null ?
                    Arrays.asList(marketRecap.getHlEvntCntt().split("\n")) : List.of()),
            highlightEventResList,
            hrList
        );

        MarketRecapRes.Checkpoints checkpointsRes = new MarketRecapRes.Checkpoints(
            new Narrative(marketRecap.getCkptTitl(),
                marketRecap.getCkptCntt() != null ?
                    Arrays.asList(marketRecap.getCkptCntt().split("\n")) : List.of()),
            checkpointResList
        );

        return new MarketRecapRes(
            marketRecap.getMrktRcapId(),
            marketRecap.getCrtrDt(),
            marketRecap.getNwsMrktCtgrCd(),
            sentiments,
            highlights,
            checkpointsRes,
            isMonday(marketRecap.getCrtrDt()) ? "72H" : "24H",
            getTimestampRangeFromDateString(marketRecap.getCrtrDt())
        );
    }

    /**
     * 기준일자와 카테고리 코드를 기반으로 sentiment 개수 계산
     *
     * @param criteriaDate 기준일자 (yyyyMMdd)
     * @param categoryCode 시장 카테고리 코드
     * @return Sentiments (금일, 전일, 최근 7일, 최근 30일)
     */
    private MarketRecapRes.Sentiments calculateSentimentCount(String criteriaDate,
        String categoryCode) {

        // 날짜 계산
        LocalDate criteria = LocalDate.parse(criteriaDate, DateTimeFormatter.ofPattern("yyyyMMdd"));
        LocalDate yesterday = criteria.minusDays(1);
        LocalDate twoDaysAgo = criteria.minusDays(2);
        LocalDate last7DaysStart = yesterday.minusDays(6); // 어제 포함 7일
        LocalDate last30DaysStart = yesterday.minusDays(29); // 어제 포함 30일

        // 단일 쿼리로 전체 기간 조회 (30일 전 ~ 오늘)
        String startDate = last30DaysStart.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        List<Object[]> results = significantNewsRepository.countSentimentsByDateRange(
            categoryCode,
            startDate,
            yesterday.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        );

        // 결과를 Map으로 변환
        Map<String, SentimentCount> sentimentMap = results.stream()
            .collect(Collectors.toMap(
                row -> (String) row[0],
                row -> new SentimentCount(
                    ((Number) row[1]).intValue(),
                    ((Number) row[2]).intValue(),
                    ((Number) row[3]).intValue()
                )
            ));

        // 각 기간별 집계
        SignalNewsCount current = buildSignalNewsCount(sentimentMap, yesterday, yesterday, "current");
        SignalNewsCount previous = buildSignalNewsCount(sentimentMap, twoDaysAgo, twoDaysAgo,
            "previous");
        SignalNewsCount last7Days = buildSignalNewsCount(sentimentMap, last7DaysStart, yesterday,
            "last7days");
        SignalNewsCount last30Days = buildSignalNewsCount(sentimentMap, last30DaysStart, yesterday,
            "last30days");

        return new MarketRecapRes.Sentiments(current, previous, last7Days, last30Days);
    }

    /**
     * News Timeline 조회
     */
    public List<NewsTimelineRes> getNewsTimeline(
        String criteriaDate, String category) {

        List<Object[]> results = newsEventRepository.findNewsTimelineData(
            criteriaDate, category);

        return results.stream()
            .map(row -> {
                Long newsEventId = ((Number) row[0]).longValue();
                String newsEventNameKr = (String) row[1];
                String newsEventNameEn = (String) row[2];
                String newsMarketCategoryCode = (String) row[3];
                Long newsSummaryId = row[4] != null ? ((Number) row[4]).longValue() : null;
                Integer dataRegistrationSn = row[5] != null ? ((Number) row[5]).intValue() : null;
                LocalDateTime firstCreated = row[6] != null
                    ? ((java.sql.Timestamp) row[6]).toLocalDateTime()
                    : null;
                String briefSummary = (String) row[7];
                String summaryStr = (String) row[8];
                String title = (String) row[9];
                String providerId = (String) row[10];
                BigDecimal score = (BigDecimal) row[11];
                String regionsStr = (String) row[12];
                // summary 변환: String → List<String> (개행 구분)
                List<String> summary = summaryStr != null && !summaryStr.isBlank()
                    ? Arrays.asList(summaryStr.split("\n"))
                    : Collections.emptyList();

                // directionality 계산
                String directionality = calculateEventDirectionality(score);
                List<String> rList = parseGroupConcatResult(regionsStr);
                return new NewsTimelineRes(
                    newsEventId,
                    newsEventNameKr,
                    newsEventNameEn,
                    newsMarketCategoryCode,
                    newsSummaryId,
                    dataRegistrationSn != null ? dataRegistrationSn.longValue() : null,
                    firstCreated,
                    briefSummary,
                    summary,
                    title,
                    providerId,
                    score,
                    directionality,
                    rList
                );
            })
            .toList();
    }

    /**
     * Object[] → NewsfeedListRes 변환
     */
    private NewsfeedListRes convertToNewsfeedListRes(
        Object[] row,
        Map<Long, List<SimilarNewsItem>> similarNewsMap) {

        Long newsSignificanceId = ((Number) row[0]).longValue();
        Long dataRegistrationSerialNo = ((Number) row[1]).longValue();
        LocalDateTime firstCreated = row[2] != null
                    ? ((java.sql.Timestamp) row[2]).toLocalDateTime()
                    : null;
        String newsMarketCategoryCode = (String) row[3];
        BigDecimal newsSignificanceScore = (BigDecimal) row[4];
        Long newsSignalId = ((Number) row[5]).longValue();
        BigDecimal significantNewsScore = (BigDecimal) row[6];
        Long newsSummaryId = ((Number) row[7]).longValue();
        String similarityGroupStatusCode = (String) row[8];
        String briefSummary = (String) row[9];
        String summaryStr = (String) row[10];
        String provider = (String) row[11];
        String title = (String) row[12];
        String regionsStr = (String) row[13];

        // summary 파싱 (쉼표로 구분된 문자열 → List)
        List<String> summary = summaryStr != null
            ? Arrays.asList(summaryStr.split(","))
            : Collections.emptyList();

        // 유사 뉴스 목록 (HEAD인 경우에만)
        List<SimilarNewsItem> similarNewsList = "HEAD".equals(similarityGroupStatusCode)
            ? similarNewsMap.getOrDefault(newsSummaryId, Collections.emptyList())
            : null;

        List<String> regionsList = parseGroupConcatResult(regionsStr);

        // directionality 계산 (significantNewsScore 기반)
        String directionality = calculateDirectionality(significantNewsScore);

        return new NewsfeedListRes(
            newsSignificanceId,
            newsSignalId,
            dataRegistrationSerialNo,
            firstCreated,
            newsMarketCategoryCode,
            newsSummaryId,
            similarityGroupStatusCode,
            briefSummary,
            summary,
            title,
            provider,
            newsSignificanceScore,
            significantNewsScore,
            directionality,
            regionsList,
            similarNewsList
        );
    }

    /**
     * 기간별 SignalNewsCount 생성
     *
     * @param sentimentMap 날짜별 sentiment 카운트 맵
     * @param startDate    시작일
     * @param endDate      종료일
     * @param periodType   기간 타입 (current/previous/last7days/last30days)
     * @return SignalNewsCount
     */
    private MarketRecapRes.SignalNewsCount buildSignalNewsCount(
        Map<String, SentimentCount> sentimentMap,
        LocalDate startDate,
        LocalDate endDate,
        String periodType
    ) {
        int upward = 0;
        int neutral = 0;
        int downward = 0;

        // 기간 내 모든 날짜를 순회하며 집계
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String dateKey = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            SentimentCount count = sentimentMap.getOrDefault(dateKey, new SentimentCount(0, 0, 0));
            upward += count.getUpward();
            neutral += count.getNeutral();
            downward += count.getDownward();
        }

        String period = formatPeriod(startDate, endDate, periodType);
        return new MarketRecapRes.SignalNewsCount(upward, neutral, downward, period);
    }

    /**
     * 기간 포맷팅 (예: "Sep 12 00:00 - Sep 13 00:00")
     *
     * @param startDate  시작일
     * @param endDate    종료일
     * @param periodType 기간 타입
     * @return 포맷된 기간 문자열
     */
    private String formatPeriod(LocalDate startDate, LocalDate endDate, String periodType) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd HH:mm", Locale.ENGLISH);

        // 시작 시간은 00:00, 종료 시간도 00:00으로 설정 (24시간 기준)
        LocalDateTime start = startDate.atTime(0, 0);
        LocalDateTime end = endDate.plusDays(1).atTime(0, 0);

        return String.format("%s - %s",
            start.format(formatter),
            end.format(formatter)
        );
    }

    /**
     * Sentiment 카운트를 담는 내부 DTO
     */
    @Value
    private static class SentimentCount {

        int upward;
        int neutral;
        int downward;
    }
}
