package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode.Error;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.DateUtil;
import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.mi.dto.response.news.MarketNewsDetailRes;
import com.sk.domain.mi.dto.response.news.MarketNewsListRes;
import com.sk.domain.mi.dto.response.news.MarketRecapListRes;
import com.sk.domain.mi.dto.response.news.MarketRecapRes;
import com.sk.domain.mi.dto.response.news.MarketRecapRes.Narrative;
import com.sk.domain.mi.dto.response.news.MarketRecapRes.SignalNewsCount;
import com.sk.domain.mi.dto.response.news.NewsCriteriaDateRes;
import com.sk.domain.mi.dto.response.news.NewsTimelineRes;
import com.sk.domain.mi.dto.response.news.NewsTrendDailyRes;
import com.sk.domain.mi.dto.response.news.NewsTrendDailyRes.NewsTrendDailyItemRes;
import com.sk.domain.mi.dto.response.news.NewsfeedListRes;
import com.sk.domain.mi.dto.response.news.NewsfeedListRes.SimilarNewsItem;
import com.sk.domain.mi.dto.response.news.NewsfeedListRowProjection;
import com.sk.domain.shared.entity.master.tmd.CustomerCompanyMarketCategory;
import com.sk.domain.shared.entity.master.tmd.MarketRecap;
import com.sk.domain.shared.entity.master.tmd.MarketRecapCheckpoint;
import com.sk.domain.shared.entity.master.tmd.MarketRecapHighlightEvent;
import com.sk.domain.shared.entity.master.tmd.MarketRecapHighlightEventRelationship;
import com.sk.domain.shared.entity.master.tmd.NewsEvent;
import com.sk.domain.shared.entity.master.tmd.NewsSignificance;
import com.sk.domain.shared.entity.master.tmd.NewsSummary;
import com.sk.domain.shared.enums.news.DateRangeOption;
import com.sk.domain.shared.enums.news.RecapCategory;
import com.sk.domain.shared.enums.news.SortType;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyMarketCategoryRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsRepository;
import com.sk.domain.shared.repository.master.tmd.MarketRecapCheckpointRepository;
import com.sk.domain.shared.repository.master.tmd.MarketRecapHighlightEventRelationshipRepository;
import com.sk.domain.shared.repository.master.tmd.MarketRecapHighlightEventRepository;
import com.sk.domain.shared.repository.master.tmd.MarketRecapListRow;
import com.sk.domain.shared.repository.master.tmd.MarketRecapRepository;
import com.sk.domain.shared.repository.master.tmd.NewsEventRelationshipRepository;
import com.sk.domain.shared.repository.master.tmd.NewsEventRepository;
import com.sk.domain.shared.repository.master.tmd.NewsSignificanceRepository;
import com.sk.domain.shared.repository.master.tmd.NewsSummaryRepository;
import com.sk.domain.shared.repository.master.tmd.SignificantNewsRepository;
import com.sk.domain.shared.service.permissions.AuthorizationService;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
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
    private final NewsEventRelationshipRepository newsEventRelationshipRepository;
    private final NewsEventRepository newsEventRepository;
    private final SignificantNewsRepository significantNewsRepository;
    private final CustomerCompanyMarketCategoryRepository customerCompanyMarketCategoryRepository;
    private final GoodsRepository goodsRepository;
    private final AuthorizationService authorizationService;

    private static LocalDate resolveStartDate(DateRangeOption dateRangeOption, LocalDate endDate) {
        int businessDays = switch (dateRangeOption) {
            case LAST_7_DAYS -> 7;
            case LAST_14_DAYS -> 14;
            case LAST_30_DAYS -> 30;
            case LAST_60_DAYS -> 60;
            default -> -1;
        };

        if (businessDays > 0) {
            return subtractWeekdays(endDate, businessDays - 1);
        }
        return dateRangeOption.getStartDate(endDate);
    }

    private static LocalDate adjustToPreviousWeekday(LocalDate date) {
        LocalDate adjusted = date;
        while (isWeekend(adjusted)) {
            adjusted = adjusted.minusDays(1);
        }
        return adjusted;
    }

    private static LocalDate subtractWeekdays(LocalDate date, int weekdaysToSubtract) {
        LocalDate current = date;
        int remaining = weekdaysToSubtract;
        while (remaining > 0) {
            current = current.minusDays(1);
            if (!isWeekend(current)) {
                remaining--;
            }
        }
        return current;
    }

    private static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY
            || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

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
        String sentiments,
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
        List<String> sentimentList =
            (sentiments != null && !sentiments.isBlank()) ? Arrays.stream(sentiments.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList() : null;
        List<String> industryList =
            (industries != null && !industries.isBlank()) ? Arrays.stream(industries.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList() : null;

        boolean filterByCategory = categories != null && !categoryList.isEmpty();
        boolean filterBySentiment = sentimentList != null && !sentimentList.isEmpty();
        boolean filterByRegion = regionList != null && !regionList.isEmpty();
        boolean filterByIndustry = industries != null && !industryList.isEmpty();

        List<Object[]> results = newsSignificanceRepository.findMarketNewsListWithGroupConcat(
            keyword, startDateTime, endDateTime,
            filterByCategory ? categoryList : null, filterByCategory,
            filterBySentiment ? sentimentList : null, filterBySentiment,
            filterByRegion ? regionList : null, filterByRegion,
            filterByIndustry ? industryList : null, filterByIndustry,
            sortDirection, size, offset);

        long totalCount = newsSignificanceRepository.countMarketNewsList(
            keyword, startDateTime, endDateTime,
            filterByCategory ? categoryList : null, filterByCategory,
            filterBySentiment ? sentimentList : null, filterBySentiment,
            filterByRegion ? regionList : null, filterByRegion,
            filterByIndustry ? industryList : null, filterByIndustry);

        LinkedHashSet<String> goodsIds = new LinkedHashSet<>();
        for (Object[] result : results) {
            String goodsId = (String) result[5];
            if (goodsId != null && !goodsId.isBlank()) {
                goodsIds.add(goodsId);
            }
        }

        Map<String, Integer> goodsNoByGoodsId = new HashMap<>();
        if (!goodsIds.isEmpty()) {
            List<Object[]> goodsRows = goodsRepository.findGoodsNoByGoodsIdIn(
                new ArrayList<>(goodsIds)
            );
            for (Object[] row : goodsRows) {
                goodsNoByGoodsId.putIfAbsent((String) row[0], (Integer) row[1]);
            }
        }

        PermissionRes permissions = authorizationService.getPermissions();
        Map<Integer, Boolean> goodsViewableMap = permissions.getGoodsViewableMap(
            authorizationService.getCompanyNoWithPriority()
        );

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
                String goodsId = (String) result[5];
                String categoryStr = (String) result[6];
                String regionsStr = (String) result[7];
                String industriesStr = (String) result[8];

                List<String> cList = parseGroupConcatResult(categoryStr);
                List<String> rList = parseGroupConcatResult(regionsStr);
                List<String> iList = parseGroupConcatResult(industriesStr);

                Integer goodsNo = goodsId != null ? goodsNoByGoodsId.get(goodsId) : null;
                boolean isViewable = newsSummaryId != null && goodsNo != null
                    && goodsViewableMap.getOrDefault(goodsNo, Boolean.FALSE);

                return new MarketNewsListRes(
                    newsSummaryId,
                    dataRegistrationSn,
                    firstCreated,
                    title,
                    providerId,  // provider → providerId
                    isViewable,
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

        BigDecimal minScore = filterBySentiment ? null : BigDecimal.valueOf(5.0);
        List<NewsfeedListRowProjection> results = newsSignificanceRepository.findNewsfeedWithGroupConcat(
            criteriaDate, category,
            minScore,
            filterBySentiment ? sentimentList : null, filterBySentiment,
            filterByImpactScore ? impactScoreList : null, filterByImpactScore,
            filterByRegion ? regionList : null, filterByRegion,
            sortField, sortDirection,
            PageRequest.of(0, 1000)
        );

        if (!filterBySentiment && results.isEmpty()) {
            results = newsSignificanceRepository.findNewsfeedWithGroupConcat(
                criteriaDate, category,
                null,
                null, false,
                filterByImpactScore ? impactScoreList : null, filterByImpactScore,
                filterByRegion ? regionList : null, filterByRegion,
                sortField, sortDirection,
                PageRequest.of(0, 100)
            );
        }

        if (results.isEmpty()) {
            return Collections.emptyList();
        }

        LinkedHashSet<Long> headNewsSummaryIds = new LinkedHashSet<>();
        LinkedHashSet<Long> newsSummaryIds = new LinkedHashSet<>();
        LinkedHashSet<String> goodsIds = new LinkedHashSet<>();
        for (NewsfeedListRowProjection row : results) {
            Long newsSummaryId = row.getNewsSummaryId();
            if (newsSummaryId == null) {
                continue;
            }
            newsSummaryIds.add(newsSummaryId);
            if ("HEAD".equals(row.getSimilarityGroupStatusCode())) {
                headNewsSummaryIds.add(newsSummaryId);
            }
            String goodsId = row.getGoodsId();
            if (goodsId != null && !goodsId.isBlank()) {
                goodsIds.add(goodsId);
            }
        }

        List<Object[]> similarResults = Collections.emptyList();
        if (!headNewsSummaryIds.isEmpty()) {
            similarResults = newsSignificanceRepository
                .findSimilarNewsByHeadIds(new ArrayList<>(headNewsSummaryIds));
            for (Object[] row : similarResults) {
                String goodsId = (String) row[7];
                if (goodsId != null && !goodsId.isBlank()) {
                    goodsIds.add(goodsId);
                }
            }
        }

        Map<String, Integer> goodsNoByGoodsId = new HashMap<>();
        if (!goodsIds.isEmpty()) {
            List<Object[]> goodsRows = goodsRepository.findGoodsNoByGoodsIdIn(
                new ArrayList<>(goodsIds)
            );
            for (Object[] row : goodsRows) {
                goodsNoByGoodsId.putIfAbsent((String) row[0], (Integer) row[1]);
            }
        }

        PermissionRes permissions = authorizationService.getPermissions();
        Map<Integer, Boolean> goodsViewableMap = permissions.getGoodsViewableMap(
            authorizationService.getCompanyNoWithPriority()
        );

        Map<Long, List<SimilarNewsItem>> similarNewsMap = new HashMap<>();
        if (!similarResults.isEmpty()) {
            // headNewsSummaryId별로 그룹핑
            for (Object[] row : similarResults) {
                Long headNewsSummaryId = ((Number) row[1]).longValue();
                String goodsId = (String) row[7];
                Integer goodsNo = goodsId != null ? goodsNoByGoodsId.get(goodsId) : null;
                boolean isViewable = goodsNo != null
                    && goodsViewableMap.getOrDefault(goodsNo, Boolean.FALSE);

                SimilarNewsItem item = new SimilarNewsItem(
                    ((Number) row[2]).longValue(), // incl_nws_smry_id
                    (String) row[3],                // title
                    ((java.sql.Timestamp) row[4]).toLocalDateTime(), // first_created
                    ((Number) row[5]).longValue(),  // data_reg_sn
                    isViewable
                );

                similarNewsMap
                        .computeIfAbsent(headNewsSummaryId, k -> new ArrayList<>())
                        .add(item);
            }
        }

        Map<Long, List<String>> eventsByNewsSummaryId = new HashMap<>();
        if (!newsSummaryIds.isEmpty()) {
            List<Object[]> eventRows =
                newsEventRelationshipRepository.findEventNamesByNewsSummaryIds(
                    new ArrayList<>(newsSummaryIds),
                    criteriaDate
                );
            for (Object[] row : eventRows) {
                Long newsSummaryId = ((Number) row[0]).longValue();
                String eventName = (String) row[1];
                if (eventName == null || eventName.isBlank()) {
                    continue;
                }
                eventsByNewsSummaryId
                    .computeIfAbsent(newsSummaryId, k -> new ArrayList<>())
                    .add(eventName);
            }
        }

        List<NewsfeedListRes> response = new ArrayList<>(results.size());
        for (NewsfeedListRowProjection row : results) {
            response.add(convertToNewsfeedListRes(
                row,
                similarNewsMap,
                eventsByNewsSummaryId,
                goodsNoByGoodsId,
                goodsViewableMap
            ));
        }
        return response;
    }

    /**
     * 점수 기반 방향성 계산 (significantNewsScore)
     */
    private String calculateDirectionality(BigDecimal score) {
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
        if (score.compareTo(new BigDecimal("2.0")) >= 0) {
            return "UPWARD";
        } else if (score.compareTo(new BigDecimal("-2.0")) <= 0) {
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
        if (score.compareTo(new BigDecimal("2.0")) >= 0) {
            return "UPWARD";
        } else if (score.compareTo(new BigDecimal("-2.0")) <= 0) {
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
    public MarketNewsDetailRes getMarketNewsDetail(Long newsSummaryId) {
        Long id = Long.valueOf(newsSummaryId);

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
            "본문",
            newsSummary.getRefinitivNewsData().getStory(),
            "요약",
            newsSummary.getSummary(),
            newsSummary.getProviderId(),  // provider → providerId
            tagList
        );
    }

    /**
     * MarketRecap -> MarketRecapRes 변환
     */
    private MarketRecapRes toMarketRecapRes(MarketRecap marketRecap) {
        Map<String, Boolean> viewableByGoodsId = new HashMap<>();
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

                        String providerId = representativeNews.getProviderId();
                        String goodsId = representativeNews.getGoodsId();
                        boolean isViewable = goodsId != null && viewableByGoodsId.computeIfAbsent(
                            goodsId,
                            key -> goodsRepository.queryGoodsNoByGoodsId(key)
                                .map(authorizationService::hasGoodsViewPermission)
                                .orElse(false)
                        );

                        return new MarketRecapRes.RelatedEventRepresentativeNewsRes(
                            representativeNews.getNewsSummaryId(),
                            representativeNews.getRefinitivNewsData() != null
                                ? representativeNews.getRefinitivNewsData().getTitle() : null,
                            providerId,
                            isViewable
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
        boolean monday = criteria.getDayOfWeek() == DayOfWeek.MONDAY;
        LocalDate yesterday = monday ? criteria.minusDays(3) : criteria.minusDays(1);
        LocalDate last7DaysStart = criteria.minusDays(7);
        LocalDate last30DaysStart = criteria.minusDays(30);

        // 단일 쿼리로 전체 기간 조회 (30일 전 ~ 오늘)
        String startDate = last30DaysStart.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        List<Object[]> results = significantNewsRepository.countSentimentsByDateRange(
            categoryCode,
            startDate,
            criteriaDate
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
        SignalNewsCount current = buildSignalNewsCount(sentimentMap, criteria, criteria, "current");
        SignalNewsCount previous = buildSignalNewsCount(sentimentMap, yesterday, yesterday,
            "previous");
        SignalNewsCount last7Days = buildSignalNewsCount(sentimentMap, last7DaysStart, criteria,
            "last7days");
        SignalNewsCount last30Days = buildSignalNewsCount(sentimentMap, last30DaysStart, criteria,
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
        Map<String, Boolean> viewableByGoodsId = new HashMap<>();

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
                String goodsId = (String) row[11];
                BigDecimal score = (BigDecimal) row[12];
                String regionsStr = (String) row[13];
                // summary 변환: String → List<String> (개행 구분)
                List<String> summary = summaryStr != null && !summaryStr.isBlank()
                    ? Arrays.asList(summaryStr.split("\n"))
                    : Collections.emptyList();

                boolean isViewable = newsSummaryId != null && goodsId != null
                    && viewableByGoodsId.computeIfAbsent(
                        goodsId,
                        key -> goodsRepository.queryGoodsNoByGoodsId(key)
                            .map(authorizationService::hasGoodsViewPermission)
                            .orElse(false)
                    );

                // directionality 계산
                String directionality = calculateDirectionality(score);
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
                    isViewable,
                    score,
                    directionality,
                    rList
                );
            })
            .toList();
    }

    /**
     * 일별 뉴스 트렌드 데이터 조회
     */
    public NewsTrendDailyRes getDailyTrend(Integer companyNo, DateRangeOption dateRangeOption) {
        CustomerCompanyMarketCategory marketCategory =
            customerCompanyMarketCategoryRepository
                .findFirstByCustomerCompanyNoAndIsDeletedFalse(companyNo)
                .orElseThrow(() -> AppErrorException.of(
                    Error.RSC_DATA_NOT_FOUND,
                    "Customer company market category not found. companyNo: %s".formatted(companyNo)
                ));

        String categoryCode = marketCategory.getNwsMrktCtgrCd().getCode();
        String criteriaItemName = buildCriteriaItemName(categoryCode);

        LocalDate endDate = adjustToPreviousWeekday(LocalDate.now());
        LocalDate startDate = resolveStartDate(dateRangeOption, endDate);

        String startDateStr = DateUtil.toYmd(startDate);
        String endDateStr = DateUtil.toYmd(endDate);

        List<Object[]> results = significantNewsRepository.countSentimentsByDateRange(
            categoryCode,
            startDateStr,
            endDateStr
        );

        Map<String, SentimentCount> sentimentMap = results.stream()
            .collect(Collectors.toMap(
                row -> (String) row[0],
                row -> new SentimentCount(
                    ((Number) row[1]).intValue(),
                    ((Number) row[2]).intValue(),
                    ((Number) row[3]).intValue()
                )
            ));

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<NewsTrendDailyItemRes> data = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (isWeekend(date)) {
                continue;
            }
            String dateKey = DateUtil.toYmd(date);
            SentimentCount count = sentimentMap.getOrDefault(dateKey, new SentimentCount(0, 0, 0));
            data.add(new NewsTrendDailyItemRes(
                date.format(dateFormatter),
                count.upward,
                count.neutral,
                count.downward
            ));
        }

        return new NewsTrendDailyRes(criteriaItemName, data);
    }

    /**
     * 시장 뉴스 기준일자 계산
     */
    public NewsCriteriaDateRes getMarketNewsCriteriaDate() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        // 몇 시 기준으로 날짜를 변경할지는 DB 조회로 변경예정
        LocalTime cutoff = LocalTime.of(9, 0);
        DayOfWeek dayOfWeek = today.getDayOfWeek();

        LocalDate criteriaDate;
        LocalDateTime nextUpdate;
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY ||
            (dayOfWeek == DayOfWeek.MONDAY && now.toLocalTime().isBefore(cutoff))) {
            criteriaDate = getLastFriday(today);
            nextUpdate = getNextMondayUpdate(today);
        } else if (now.toLocalTime().isBefore(cutoff)) {
            criteriaDate = today.minusDays(1);
            nextUpdate = today.atTime(cutoff);
        } else {
            criteriaDate = today;
            nextUpdate = getNextUpdateAfterCutoff(today);
        }

        String criteriaDateStr = DateUtil.toYmd(criteriaDate);
        String nextUpdateStr = nextUpdate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        return new NewsCriteriaDateRes(criteriaDateStr, nextUpdateStr);
    }

    private LocalDate getLastFriday(LocalDate referenceDate) {
        return switch (referenceDate.getDayOfWeek()) {
            case SATURDAY -> referenceDate.minusDays(1);
            case SUNDAY -> referenceDate.minusDays(2);
            case MONDAY -> referenceDate.minusDays(3);
            default -> referenceDate;
        };
    }

    private LocalDateTime getNextMondayUpdate(LocalDate referenceDate) {
        LocalDate monday = switch (referenceDate.getDayOfWeek()) {
            case SATURDAY -> referenceDate.plusDays(2);
            case SUNDAY -> referenceDate.plusDays(1);
            case MONDAY -> referenceDate;
            default -> referenceDate;
        };
        return monday.atTime(9, 0);
    }

    private LocalDateTime getNextUpdateAfterCutoff(LocalDate today) {
        if (today.getDayOfWeek() == DayOfWeek.FRIDAY) {
            return today.plusDays(3).atTime(9, 0);
        }
        return today.plusDays(1).atTime(9, 0);
    }

    private String buildCriteriaItemName(String categoryCode) {
        if ("CROIL".equals(categoryCode)) {
            return "Brent Crude Price";
        }
        return categoryCode + " Price";
    }

    /**
     * Object[] → NewsfeedListRes 변환
     */
    private NewsfeedListRes convertToNewsfeedListRes(
        NewsfeedListRowProjection row,
        Map<Long, List<SimilarNewsItem>> similarNewsMap,
        Map<Long, List<String>> eventsByNewsSummaryId,
        Map<String, Integer> goodsNoByGoodsId,
        Map<Integer, Boolean> goodsViewableMap) {

        Long newsSignificanceId = row.getNewsSignificanceId();
        Long dataRegistrationSerialNo = row.getDataRegistrationSerialNo();
        LocalDateTime firstCreated = row.getFirstCreated();
        String newsMarketCategoryCode = row.getNewsMarketCategoryCode();
        BigDecimal newsSignificanceScore = row.getNewsSignificanceScore();
        Long newsSignalId = row.getNewsSignalId();
        BigDecimal significantNewsScore = row.getSignificantNewsScore();
        Long newsSummaryId = row.getNewsSummaryId();
        String similarityGroupStatusCode = row.getSimilarityGroupStatusCode();
        String briefSummary = row.getBriefSummary();
        String summaryStr = row.getSummary();
        String provider = row.getProvider();
        String goodsId = row.getGoodsId();
        String title = row.getTitle();
        String regionsStr = row.getRegions();

        // summary 파싱 (쉼표로 구분된 문자열 → List)
        List<String> summary = summaryStr != null
            ? Arrays.asList(summaryStr.split("\n"))
            : Collections.emptyList();

        // 유사 뉴스 목록 (HEAD인 경우에만)
        List<SimilarNewsItem> similarNewsList = "HEAD".equals(similarityGroupStatusCode)
            ? similarNewsMap.getOrDefault(newsSummaryId, Collections.emptyList())
            : null;

        List<String> regionsList = parseGroupConcatResult(regionsStr);
        List<String> events = eventsByNewsSummaryId.getOrDefault(
            newsSummaryId,
            Collections.emptyList()
        );

        Integer goodsNo = goodsId != null ? goodsNoByGoodsId.get(goodsId) : null;
        boolean isViewable = newsSummaryId != null && goodsNo != null
            && goodsViewableMap.getOrDefault(goodsNo, Boolean.FALSE);

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
            isViewable,
            newsSignificanceScore,
            significantNewsScore,
            directionality,
            regionsList,
            events,
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
        boolean startIsMonday = startDate.getDayOfWeek() == DayOfWeek.MONDAY;
        LocalDate sd = startIsMonday ? startDate.minusDays(3) : startDate.minusDays(1);
        LocalDateTime start = sd.atTime(0, 0);
        LocalDateTime end = endDate.atTime(0, 0);

        return String.format("%s - %s",
            start.format(formatter),
            end.format(formatter)
        );
    }

    /**
     * 마켓 리캡 목록 조회
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param sort 정렬 기준
     * @param keyword 검색 키워드
     * @param dateRangeOption 날짜 범위 옵션
     * @param recapCategories 리캡 카테고리 (HIGHLIGHT/CHECKPOINT)
     * @return 마켓 리캡 목록 페이징 응답
     */
    public PaginationRes<MarketRecapListRes> getMarketRecaps(
        int page,
        int size,
        SortType sort,
        String keyword,
        DateRangeOption dateRangeOption,
        String recapCategories,
        int companyNo
    ) {

        CustomerCompanyMarketCategory customerCompanyMarketCategory =
            customerCompanyMarketCategoryRepository
                .findFirstByCustomerCompanyNoAndIsDeletedFalse(companyNo)
                .orElse(null);
        String marketCategoryCode = customerCompanyMarketCategory != null
            ? customerCompanyMarketCategory.getNwsMrktCtgrCd().getCode()
            : null;

        // 날짜 범위 계산
        LocalDate endDate = LocalDate.now();
        String endDateStr = DateUtil.toYmd(endDate);
        String startDateStr = null;

        // ALL_TIME이 아닌 경우에만 시작일 계산
        if (dateRangeOption != DateRangeOption.ALL_TIME) {
            LocalDate startDate = dateRangeOption.getStartDate(endDate);
            startDateStr = DateUtil.toYmd(startDate);
        }

        // 페이징 및 정렬 설정
        Pageable pageable = PageRequest.of(page - 1, size);

        List<RecapCategory> recapCategoryList = parseRecapCategories(recapCategories);
        boolean includeHighlight = recapCategoryList.contains(RecapCategory.HIGHLIGHT);
        boolean includeCheckpoint = recapCategoryList.contains(RecapCategory.CHECKPOINT);
        boolean isNewest = sort == SortType.NEWEST;

        boolean hasKeyword = keyword != null && !keyword.isBlank();

        Page<MarketRecapListRow> marketRecapPage;
        if (hasKeyword) {
            marketRecapPage = isNewest
                ? marketRecapRepository.findMarketRecapRowsNewest(
                    startDateStr,
                    endDateStr,
                    keyword,
                    includeHighlight,
                    includeCheckpoint,
                    marketCategoryCode,
                    pageable
                )
                : marketRecapRepository.findMarketRecapRowsOldest(
                    startDateStr,
                    endDateStr,
                    keyword,
                    includeHighlight,
                    includeCheckpoint,
                    marketCategoryCode,
                    pageable
                );
        } else {
            marketRecapPage = isNewest
                ? marketRecapRepository.findMarketRecapRowsNewestWithoutKeyword(
                    startDateStr,
                    endDateStr,
                    includeHighlight,
                    includeCheckpoint,
                    marketCategoryCode,
                    pageable
                )
                : marketRecapRepository.findMarketRecapRowsOldestWithoutKeyword(
                    startDateStr,
                    endDateStr,
                    includeHighlight,
                    includeCheckpoint,
                    marketCategoryCode,
                    pageable
                );
        }

        List<Long> recapIds = marketRecapPage.getContent().stream()
            .map(MarketRecapListRow::getMrktRcapId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        Map<Long, List<MarketRecapHighlightEvent>> highlightEventsByRecap = new HashMap<>();
        Map<Long, List<MarketRecapHighlightEventRelationship>> relationshipsByEventId = new HashMap<>();
        if (includeHighlight && !recapIds.isEmpty()) {
            List<MarketRecapHighlightEvent> highlightEvents =
                marketRecapHighlightEventRepository.findByMarketRecapIdInOrderByScoreDesc(recapIds);

            highlightEventsByRecap.putAll(highlightEvents.stream()
                .collect(Collectors.groupingBy(MarketRecapHighlightEvent::getMrktRcapId)));

            List<Long> highlightEventIds = highlightEvents.stream()
                .map(MarketRecapHighlightEvent::getMrktRcapHlEvntId)
                .filter(Objects::nonNull)
                .toList();

            if (!highlightEventIds.isEmpty()) {
                List<MarketRecapHighlightEventRelationship> relationships =
                    marketRecapHighlightEventRelationshipRepository.findByHighlightEventIds(highlightEventIds);
                relationshipsByEventId.putAll(relationships.stream()
                    .collect(Collectors.groupingBy(MarketRecapHighlightEventRelationship::getMrktRcapHlEvntId)));
            }
        }

        Map<Long, List<MarketRecapCheckpoint>> checkpointsByRecap = new HashMap<>();
        if (includeCheckpoint && !recapIds.isEmpty()) {
            List<MarketRecapCheckpoint> checkpoints =
                marketRecapCheckpointRepository.findByMarketRecapIdInOrderById(recapIds);
            checkpointsByRecap.putAll(checkpoints.stream()
                .collect(Collectors.groupingBy(MarketRecapCheckpoint::getMrktRcapId)));
        }

        Map<String, Integer> goodsNoByGoodsId = new HashMap<>();
        Map<Integer, Boolean> goodsViewableMap = new HashMap<>();
        if (includeHighlight) {
            LinkedHashSet<String> goodsIds = new LinkedHashSet<>();
            for (List<MarketRecapHighlightEventRelationship> relationships : relationshipsByEventId.values()) {
                for (MarketRecapHighlightEventRelationship relationship : relationships) {
                    if (relationship.getNewsEvent() == null ||
                        relationship.getNewsEvent().getRepresentativeNewsSummary() == null) {
                        continue;
                    }
                    String goodsId =
                        relationship.getNewsEvent().getRepresentativeNewsSummary().getGoodsId();
                    if (goodsId != null && !goodsId.isBlank()) {
                        goodsIds.add(goodsId);
                    }
                }
            }

            if (!goodsIds.isEmpty()) {
                List<Object[]> goodsRows = goodsRepository.findGoodsNoByGoodsIdIn(
                    new ArrayList<>(goodsIds)
                );
                for (Object[] row : goodsRows) {
                    goodsNoByGoodsId.putIfAbsent((String) row[0], (Integer) row[1]);
                }
            }

            PermissionRes permissions = authorizationService.getPermissions();
            goodsViewableMap.putAll(permissions.getGoodsViewableMap(
                authorizationService.getCompanyNoWithPriority()
            ));
        }

        List<MarketRecapListRes> resultList =
            marketRecapPage.getContent().stream()
                .map(row -> {
                    RecapCategory recapCategory = RecapCategory.valueOf(row.getRecapCategory());
                    return recapCategory == RecapCategory.HIGHLIGHT
                        ? buildHighlightResponse(
                            row,
                            highlightEventsByRecap.getOrDefault(row.getMrktRcapId(), Collections.emptyList()),
                            relationshipsByEventId,
                            goodsNoByGoodsId,
                            goodsViewableMap
                        )
                        : buildCheckpointResponse(
                            row,
                            checkpointsByRecap.getOrDefault(row.getMrktRcapId(), Collections.emptyList())
                        );
                })
                .toList();

        Page<MarketRecapListRes> resultPage =
            new org.springframework.data.domain.PageImpl<>(
                resultList,
                pageable,
                marketRecapPage.getTotalElements()
            );

        return PaginationRes.toRes(resultPage);
    }

    private List<RecapCategory> parseRecapCategories(String recapCategories) {
        if (recapCategories == null || recapCategories.isBlank()) {
            return List.of(RecapCategory.HIGHLIGHT, RecapCategory.CHECKPOINT);
        }

        List<RecapCategory> parsedCategories = Arrays.stream(recapCategories.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .map(value -> RecapCategory.valueOf(value.toUpperCase(Locale.ENGLISH)))
            .distinct()
            .toList();

        if (parsedCategories.isEmpty()) {
            return List.of(RecapCategory.HIGHLIGHT, RecapCategory.CHECKPOINT);
        }

        return parsedCategories;
    }

    /**
     * HIGHLIGHT 응답 생성
     */
    private MarketRecapListRes buildHighlightResponse(
        MarketRecapListRow row,
        List<MarketRecapHighlightEvent> highlightEvents,
        Map<Long, List<MarketRecapHighlightEventRelationship>> relationshipsByEventId,
        Map<String, Integer> goodsNoByGoodsId,
        Map<Integer, Boolean> goodsViewableMap
    ) {
        List<MarketRecapListRes.HighlightEventRes> highlightItems =
            highlightEvents.stream()
                .map(event -> {
                    List<MarketRecapHighlightEventRelationship> relationships =
                        relationshipsByEventId.getOrDefault(event.getMrktRcapHlEvntId(), Collections.emptyList());
                    List<MarketRecapListRes.RelatedEventRepresentativeNewsRes> relatedNews =
                        relationships.stream()
                            .map(rel -> {
                                if (rel.getNewsEvent() == null ||
                                    rel.getNewsEvent().getRepresentativeNewsSummary() == null) {
                                    return null;
                                }
                                NewsSummary representativeNews =
                                    rel.getNewsEvent().getRepresentativeNewsSummary();
                                String providerId = representativeNews.getProviderId();
                                String goodsId = representativeNews.getGoodsId();
                                Integer goodsNo =
                                    goodsId != null ? goodsNoByGoodsId.get(goodsId) : null;
                                boolean isViewable = goodsNo != null
                                    && goodsViewableMap.getOrDefault(goodsNo, Boolean.FALSE);
                                return new MarketRecapListRes.RelatedEventRepresentativeNewsRes(
                                    representativeNews.getNewsSummaryId(),
                                    representativeNews.getRefinitivNewsData() != null
                                        ? representativeNews.getRefinitivNewsData().getTitle()
                                        : null,
                                    providerId,
                                    isViewable
                                );
                            })
                            .filter(Objects::nonNull)
                            .toList();

                    return new MarketRecapListRes.HighlightEventRes(
                        event.getMrktRcapHlEvntId(),
                        event.getNameKr(),
                        event.getNameEn(),
                        null,
                        relatedNews
                    );
                })
                .toList();

        return MarketRecapListRes.ofHighlight(
            row.getMrktRcapId(),
            row.getCrtrDt(),
            row.getNwsMrktCtgrCd(),
            highlightItems
        );
    }

    /**
     * CHECKPOINT 응답 생성
     */
    private MarketRecapListRes buildCheckpointResponse(
        MarketRecapListRow row,
        List<MarketRecapCheckpoint> checkpoints
    ) {
        List<com.sk.domain.mi.dto.response.news.MarketRecapListRes.CheckpointRes> checkpointItems =
            checkpoints.stream()
                .map(checkpoint -> new com.sk.domain.mi.dto.response.news.MarketRecapListRes.CheckpointRes(
                    checkpoint.getMrktRcapCkptId(),
                    checkpoint.getTitle(),
                    checkpoint.getContent() != null
                        ? Arrays.asList(checkpoint.getContent().split("\n"))
                        : Collections.emptyList()
                ))
                .toList();

        return MarketRecapListRes.ofCheckpoint(
            row.getMrktRcapId(),
            row.getCrtrDt(),
            row.getNwsMrktCtgrCd(),
            checkpointItems
        );
    }

    /**
     * Sentiment 카운트를 담는 내부 DTO
     */
    @lombok.Value
    private static class SentimentCount {

        int upward;
        int neutral;
        int downward;
    }
}
