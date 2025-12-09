package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode.Error;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.DateUtil;
import com.sk.domain.mi.dto.response.news.MarketNewsDetailRes;
import com.sk.domain.mi.dto.response.news.MarketNewsListRes;
import com.sk.domain.mi.dto.response.news.MarketRecapRes;
import com.sk.domain.mi.dto.response.news.MarketRecapRes.Narrative;
import com.sk.domain.mi.dto.response.news.NewsfeedListRes;
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
import com.sk.domain.shared.repository.master.tmd.NewsEventRelationshipRepository;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
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
    private final SignificantNewsRepository significantNewsRepository;
    private final NewsEventRelationshipRepository newsEventRelationshipRepository;


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

        List<String> categoryList = categories != null ? Arrays.asList(categories.split(",")) : null;
        List<String> regionList = regions != null ? Arrays.asList(regions.split(",")) : null;
        List<String> industryList = industries != null ? Arrays.asList(industries.split(",")) : null;

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

        List<String> sentimentList = sentiments != null ? Arrays.asList(sentiments.split(",")) : null;
        List<String> impactScoreList = impactScores != null ? Arrays.asList(impactScores.split(",")) : null;
        List<String> regionList = regions != null ? Arrays.asList(regions.split(",")) : null;

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

        return results.stream()
            .map(result -> {
                Long nwsSigId = ((Number) result[0]).longValue();
                Long dataRegSn = ((Number) result[1]).longValue();
                LocalDateTime firstCreated = result[2] != null
                    ? ((java.sql.Timestamp) result[2]).toLocalDateTime()
                    : null;
                String nwsMrktCtgrCd = (String) result[3];
                java.math.BigDecimal score = result[4] != null
                    ? (java.math.BigDecimal) result[4]
                    : null;

                java.math.BigDecimal sgtScore = result[6] != null
                    ? (java.math.BigDecimal) result[6]
                    : null;

                Integer newsSummaryId = result[7] != null
                    ? ((Number) result[7]).intValue()
                    : null;
                String briefSummary = (String) result[8];
                String summary = (String) result[9];
                String providerId = (String) result[10];  // provider → providerId
                String title = (String) result[11];
                String regionsStr = (String) result[12];

                List<String> regionsList = parseGroupConcatResult(regionsStr);

                // directionality 계산 (score 기반)
                String directionality = calculateDirectionality(sgtScore);

                return new NewsfeedListRes(
                    nwsSigId,
                    dataRegSn,
                    firstCreated,
                    nwsMrktCtgrCd,
                    newsSummaryId,
                    briefSummary,
                    summary != null ? Arrays.asList(summary.split("\n")) : null,
                    title,
                    providerId,  // provider → providerId
                    score,
                    directionality,
                    sgtScore,
                    regionsList
                );
            })
            .toList();
    }

    /**
     * 점수 기반 방향성 계산
     */
    private String calculateDirectionality(BigDecimal score) {
        if (score == null) {
            return "Neutral";
        }
        if (score.compareTo(BigDecimal.valueOf(7.0)) >= 0) {
            return "Upward";
        } else if (score.compareTo(BigDecimal.valueOf(4.0)) <= 0) {
            return "Downward";
        } else {
            return "Neutral";
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

        MarketRecap marketRecap = marketRecapRepository
            .findByCrtrDtAndNwsMrktCtgrCd(criteriaDate, category)
            .orElseThrow(() -> AppErrorException.of(Error.RSC_DATA_NOT_FOUND,
                "MarketRecap not found for criteriaDate: %s, category: %s"
                    .formatted(criteriaDate, category)));

        return toMarketRecapRes(marketRecap);
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

        List<MarketRecapRes.RelatedNewsRes> hrList = new ArrayList<>();

        // 하이라이트 이벤트 DTO 변환
        List<MarketRecapRes.HighlightEventRes> highlightEventResList = highlightEvents.stream()
            .map(highlightEvent -> {
                List<MarketRecapHighlightEventRelationship> relationships =
                    marketRecapHighlightEventRelationshipRepository
                        .findByMarketRecapHighlightEvent(highlightEvent);

                List<MarketRecapRes.RelatedNewsRes> relatedNewsList = relationships.stream()
                    .map(rel -> {
                        NewsEvent newsEvent = rel.getNewsEvent();
                        NewsSummary representativeNews = newsEvent.getRepresentativeNewsSummary();

                        if (representativeNews == null) {
                            return null;
                        }

                        return new MarketRecapRes.RelatedNewsRes(
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
                String momentum = calculateDirectionality(highlightEvent.getScore());

                return new MarketRecapRes.HighlightEventRes(
                    highlightEvent.getMrktRcapHlEvntId(),
                    highlightEvent.getNameKr(),
                    highlightEvent.getNameEn(),
                    momentum,
                    relatedNewsList
                );
            })
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
        MarketRecapRes.SignalNewsCount current = calculateSentimentCount(
            marketRecap.getCrtrDt(), marketRecap.getNwsMrktCtgrCd());

        // TODO: previous, last7days, last30days 계산 로직 추가 필요
        MarketRecapRes.SignalNewsCount previous = new MarketRecapRes.SignalNewsCount(
            0, 0, 0, "Previous Period");
        MarketRecapRes.SignalNewsCount last7days = new MarketRecapRes.SignalNewsCount(
            0, 0, 0, "Last 7 Days");
        MarketRecapRes.SignalNewsCount last30days = new MarketRecapRes.SignalNewsCount(
            0, 0, 0, "Last 30 Days");

        MarketRecapRes.Sentiments sentiments = new MarketRecapRes.Sentiments(
            current, previous, last7days, last30days);

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
     * Sentiment 카운트 계산 (SignificantNews 점수 기반)
     */
    private MarketRecapRes.SignalNewsCount calculateSentimentCount(
        String criteriaDate, String categoryCode) {

        // TODO: 실제 계산 로직 구현 필요
        // SignificantNews에서 해당 기준일자와 카테고리의 뉴스들을 가져와서
        // 점수 기반으로 upward/neutral/downward 카운트

        return new MarketRecapRes.SignalNewsCount(
            0, 0, 0,
            "Period for " + criteriaDate
        );
    }

    /**
     * News Timeline 조회 (영향도 8.0 이상, 최대 10개)
     */
    public List<NewsfeedListRes> getNewsTimeline(
        String criteriaDate, String category) {

        List<Object[]> results = newsSignificanceRepository.findNewsfeedWithGroupConcat(
            criteriaDate, category, null, false, null, false, null, false,
            "firstCreated", "ASC");

        // impctScre 필터링 및 limit 적용 후 DTO 변환
        return results.stream()
            .filter(result -> {
                // score가 8.0 이상인 뉴스만 필터링
                java.math.BigDecimal score = result[4] != null
                    ? (java.math.BigDecimal) result[4]
                    : null;
                return score != null &&
                    score.compareTo(new java.math.BigDecimal("8.0")) >= 0;
            })
            .limit(10)
            .map(result -> {
                Long nwsSigId = ((Number) result[0]).longValue();
                Long dataRegSn = ((Number) result[1]).longValue();
                LocalDateTime firstCreated = result[2] != null
                    ? ((java.sql.Timestamp) result[2]).toLocalDateTime()
                    : null;
                String nwsMrktCtgrCd = (String) result[3];

                java.math.BigDecimal score = result[4] != null
                    ? (java.math.BigDecimal) result[4]
                    : null;

                java.math.BigDecimal sgtScore = result[6] != null
                    ? (java.math.BigDecimal) result[6]
                    : null;

                Integer newsSummaryId = result[7] != null
                    ? ((Number) result[7]).intValue()
                    : null;
                String briefSummary = (String) result[8];
                String summary = (String) result[9];
                String providerId = (String) result[10];  // provider → providerId
                String title = (String) result[11];

                String regionsStr = (String) result[12];

                List<String> regionList = parseGroupConcatResult(regionsStr);

                String directionality = calculateDirectionality(sgtScore);

                return new NewsfeedListRes(
                    nwsSigId,
                    dataRegSn,
                    firstCreated,
                    nwsMrktCtgrCd,
                    newsSummaryId,
                    briefSummary,
                    summary != null ? Arrays.asList(summary.split("\n")) : null,
                    title,
                    providerId,  // provider → providerId
                    score,
                    directionality,
                    sgtScore,
                    regionList
                );
            })
            .toList();
    }
}
