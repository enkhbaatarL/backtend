package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode.Error;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.DateUtil;
import com.sk.domain.mi.dto.response.news.MarketNewsDetailRes;
import com.sk.domain.mi.dto.response.news.MarketNewsListRes;
import com.sk.domain.mi.dto.response.news.MarketRecapRes;
import com.sk.domain.mi.dto.response.news.MarketRecapRes.Narrative;
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
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final NewsEventRepository newsEventRepository;


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

                // summary 변환: String → List<String> (개행 구분)
                List<String> summary = summaryStr != null && !summaryStr.isBlank()
                    ? Arrays.asList(summaryStr.split("\n"))
                    : Collections.emptyList();

                // directionality 계산
                String directionality = calculateEventDirectionality(score);

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
                    directionality
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
}
