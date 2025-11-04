package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode.Error;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.response.news.MarketNewsDetailRes;
import com.sk.domain.mi.dto.response.news.MarketNewsListRes;
import com.sk.domain.mi.dto.response.news.MarketRecapRes;
import com.sk.domain.mi.dto.response.news.MarketRecapRes.Narrative;
import com.sk.domain.mi.dto.response.news.NewsfeedListRes;
import com.sk.domain.shared.entity.master.tmd.MarketRecap;
import com.sk.domain.shared.entity.master.tmd.MarketRecapCheckpointItem;
import com.sk.domain.shared.entity.master.tmd.MarketRecapHighlightItem;
import com.sk.domain.shared.entity.master.tmd.MarketRecapHighlightItemRelationship;
import com.sk.domain.shared.entity.master.tmd.NewsSignificance;
import com.sk.domain.shared.entity.master.tmd.NewsSummary;
import com.sk.domain.shared.enums.news.SortType;
import com.sk.domain.shared.repository.master.tmd.MarketRecapCheckpointItemRepository;
import com.sk.domain.shared.repository.master.tmd.MarketRecapHighlightItemRelationshipRepository;
import com.sk.domain.shared.repository.master.tmd.MarketRecapHighlightItemRepository;
import com.sk.domain.shared.repository.master.tmd.MarketRecapRepository;
import com.sk.domain.shared.repository.master.tmd.NewsSignificanceRepository;
import com.sk.domain.shared.repository.master.tmd.NewsSummaryRepository;
import com.sk.domain.shared.repository.master.tmd.SignificantNewsRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
    private final MarketRecapHighlightItemRepository marketRecapHighlightItemRepository;
    private final MarketRecapCheckpointItemRepository marketRecapCheckpointItemRepository;
    private final MarketRecapHighlightItemRelationshipRepository marketRecapHighlightItemRelationshipRepository;
    private final SignificantNewsRepository significantNewsRepository;

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
                String provider = (String) result[4];
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
                    provider,
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

        boolean filterBySentiment = sentimentList != null && !sentimentList.isEmpty();
        boolean filterByImpactScore = impactScoreList != null && !impactScoreList.isEmpty();
        boolean filterByRegion = regionList != null && !regionList.isEmpty();

        List<Object[]> results = newsSignificanceRepository.findNewsfeedWithGroupConcat(
            criteriaDate, category,
            filterBySentiment ? sentimentList : null, filterBySentiment,
            filterByImpactScore ? impactScoreList : null, filterByImpactScore,
            filterByRegion ? regionList : null, filterByRegion,
            sortField, sortDirection);

        return results.stream()
            .map(result -> {
                Long nwsSigId = ((Number) result[0]).longValue();
                Long dataRegSn = ((Number) result[1]).longValue();
                LocalDateTime firstCreated = result[2] != null
                    ? ((java.sql.Timestamp) result[2]).toLocalDateTime()
                    : null;
                String nwsMrktCtgrCd = (String) result[3];

                java.math.BigDecimal impctScre = result[5] != null
                    ? (java.math.BigDecimal) result[5]
                    : null;
                String drctnlt = (String) result[6];

                Integer newsSummaryId = result[7] != null
                    ? ((Number) result[7]).intValue()
                    : null;
                String briefSummary = (String) result[8];
                String summary = (String) result[9];
                String provider = (String) result[10];
                String title = (String) result[11];

                String regionsStr = (String) result[12];

                List<String> rList = parseGroupConcatResult(regionsStr);

                return new NewsfeedListRes(
                    nwsSigId,
                    dataRegSn,
                    firstCreated,
                    nwsMrktCtgrCd,
                    newsSummaryId,
                    briefSummary,
                    summary != null ? Arrays.asList(summary.split("\n")) : null,
                    title,
                    provider,
                    impctScre,
                    drctnlt,
                    rList
                );
            })
            .toList();
    }

    /**
     * GROUP_CONCAT 결과 문자열을 List로 변환
     */
    private List<String> parseGroupConcatResult(String groupConcatStr) {
        if (groupConcatStr == null || groupConcatStr.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(groupConcatStr.split(","));
    }

    /**
     * 날짜와 카테고리로 MarketRecap 조회
     */
    public MarketRecapRes getMarketRecapByCriteriaDateAndCategory(String crtrDt,
        String nwsMrktCtgrCd) {
        MarketRecap marketRecap = marketRecapRepository.findByCrtrDtAndNwsMrktCtgrCd(crtrDt,
                nwsMrktCtgrCd)
            .orElseThrow(() -> AppErrorException.of(Error.RSC_DATA_NOT_FOUND,
                "마켓 리캡을 찾을 수 없습니다. crtrDt: " + crtrDt + ", nwsMrktCtgrCd: " + nwsMrktCtgrCd));

        return toMarketRecapRes(marketRecap);
    }

    /**
     * ID로 MarketRecap 조회
     */
    public MarketRecapRes getMarketRecapById(Long mrktRcapId) {
        MarketRecap marketRecap = marketRecapRepository.findById(mrktRcapId)
            .orElseThrow(() -> AppErrorException.of(Error.RSC_DATA_NOT_FOUND,
                "마켓 리캡을 찾을 수 없습니다. mrktRcapId: " + mrktRcapId));

        return toMarketRecapRes(marketRecap);
    }

    public MarketNewsDetailRes getMarketNewsDetail(String newsSummaryId) {

        NewsSummary newsData = newsSummaryRepository.findById(Integer.parseInt(newsSummaryId))
            .orElseThrow(() -> AppErrorException.of(Error.RSC_DATA_NOT_FOUND,
                "뉴스를 찾을 수 없습니다. newsSummaryId: " + newsSummaryId));

        List<String> tagList = new ArrayList<>();
        List<NewsSignificance> newsSignificances = newsSignificanceRepository.findByFirstCreatedAndDataRegistrationSn(
            newsData.getFirstCreated(),
            newsData.getDataRegistrationSn()
        );

        if (newsSignificances != null && !newsSignificances.isEmpty()) {
            newsSignificances.forEach(newsSignificance -> {
                tagList.add(newsSignificance.getNwsMrktCtgrCd().getName());

                significantNewsRepository.findByNwsSigId(newsSignificance.getNwsSigId())
                .ifPresent(significantNews -> {
                    if (significantNews.getEvntDtctn() != null) {
                        tagList.add(significantNews.getEvntDtctn());
                    }
                });
            });
        }

        return toMarketNewsDetailRes(newsData, tagList);
    }

    /**
     * NewsSummary -> MarketNewsDetailRes 변환
     */
    private MarketNewsDetailRes toMarketNewsDetailRes(NewsSummary newsSummary, List<String> tagList) {
        return new MarketNewsDetailRes(
            newsSummary.getNewsSummaryId(),
            newsSummary.getRefinitivNewsData().getId().getDataRegistrationSn(),
            newsSummary.getRefinitivNewsData().getId().getFirstCreated(),
            newsSummary.getRefinitivNewsData().getTitle(),
            newsSummary.getRefinitivNewsData().getStory(),
            newsSummary.getProvider(),
            tagList
        );
    }

    /**
     * MarketRecap -> MarketRecapRes 변환
     */
    private MarketRecapRes toMarketRecapRes(MarketRecap marketRecap) {
        // 하이라이트 아이템 조회
        List<MarketRecapHighlightItem> highlightItems =
            marketRecapHighlightItemRepository.findByMarketRecap(marketRecap);

        // 체크포인트 아이템 조회
        List<MarketRecapCheckpointItem> checkpointItems =
            marketRecapCheckpointItemRepository.findByMarketRecap(marketRecap);

        List<MarketRecapRes.RelatedNewsRes> hrList = new ArrayList<>();

        // 하이라이트 아이템 DTO 변환
        List<MarketRecapRes.HighlightItemRes> highlightItemResList = highlightItems.stream()
            .map(highlightItem -> {
                List<MarketRecapHighlightItemRelationship> relationships =
                    marketRecapHighlightItemRelationshipRepository
                        .findByMarketRecapHighlightItem(highlightItem);

                List<MarketRecapRes.RelatedNewsRes> relatedNewsList = relationships.stream()
                    .map(rel -> {
                        NewsSummary newsSummary = rel.getNewsSummary();
                        return new MarketRecapRes.RelatedNewsRes(
                            newsSummary.getNewsSummaryId(),
                            newsSummary.getRefinitivNewsData() != null
                                ? newsSummary.getRefinitivNewsData().getTitle() : null,
                            newsSummary.getProvider()
                        );
                    })
                    .toList();

                hrList.addAll(relatedNewsList);
                return new MarketRecapRes.HighlightItemRes(
                    highlightItem.getMrktRcapHlItmId(),
                    highlightItem.getTitl(),
                    Arrays.asList(highlightItem.getCntt().split("\n")),
                    highlightItem.getMntm(),
                    relatedNewsList
                );
            })
            .toList();

        // 체크포인트 아이템 DTO 변환
        List<MarketRecapRes.CheckpointItemRes> checkpointItemResList = checkpointItems.stream()
            .map(checkpointItem -> new MarketRecapRes.CheckpointItemRes(
                checkpointItem.getMrktRcapCkptItmId(),
                checkpointItem.getTitl(),
                Arrays.asList(checkpointItem.getCntt().split("\n"))
            ))
            .toList();

        MarketRecapRes.SignalNewsCount current = new MarketRecapRes.SignalNewsCount(
            marketRecap.getUpwSgnlNwsCnt(),
            marketRecap.getNtrlSgnlNwsCnt(),
            marketRecap.getDwnSgnlNwsCnt(),
            "Sep 12 07:00 - Sep 13 07:00"
        );
        MarketRecapRes.SignalNewsCount previous = new MarketRecapRes.SignalNewsCount(20, 30, 40 , "Sep 12 07:00 - Sep 13 07:00");
        MarketRecapRes.SignalNewsCount last7days = new MarketRecapRes.SignalNewsCount(10, 50, 30, "Sep 12 07:00 - Sep 13 07:00");
        MarketRecapRes.SignalNewsCount last30days = new MarketRecapRes.SignalNewsCount(22, 33, 11, "Sep 12 07:00 - Sep 13 07:00");
        MarketRecapRes.Sentiments sentiments = new MarketRecapRes.Sentiments(current, previous,
            last7days, last30days);

        MarketRecapRes.Highlights highlights = new MarketRecapRes.Highlights(
            new Narrative(marketRecap.getHiTitl(),
                Arrays.asList(marketRecap.getHiCntt().split("\n"))), highlightItemResList, hrList

        );

        MarketRecapRes.Checkpoints checkpoints = new MarketRecapRes.Checkpoints(
            new Narrative(marketRecap.getCkptTitl(),
                Arrays.asList(marketRecap.getCkptCntt().split("\n"))), checkpointItemResList
        );

        return new MarketRecapRes(
            marketRecap.getMrktRcapId(),
            marketRecap.getCrtrDt(),
            marketRecap.getNwsMrktCtgrCd(),
            sentiments,
            highlights,
            checkpoints
        );
    }

    public List<NewsfeedListRes> getNewsTimeline(
        String criteriaDate, String category) {

        List<Object[]> results = newsSignificanceRepository.findNewsfeedWithGroupConcat(
            criteriaDate, category, null, false, null, false, null, false, "firstCreated", "ASC");

        // impctScre 필터링 및 limit 적용 후 DTO 변환
        return results.stream()
            .filter(result -> {
                // impctScre가 8.0 이상인 뉴스만 필터링
                java.math.BigDecimal impctScre = result[5] != null
                    ? (java.math.BigDecimal) result[5]
                    : null;
                return impctScre != null &&
                    impctScre.compareTo(new java.math.BigDecimal("8.0")) >= 0;
            })
            .limit(10)
            .map(result -> {
                // Native Query 결과 매핑
                Long nwsSigId = ((Number) result[0]).longValue();
                Long dataRegSn = ((Number) result[1]).longValue();
                LocalDateTime firstCreated = result[2] != null
                    ? ((java.sql.Timestamp) result[2]).toLocalDateTime()
                    : null;
                String nwsMrktCtgrCd = (String) result[3];

                java.math.BigDecimal impctScre = result[5] != null
                    ? (java.math.BigDecimal) result[5]
                    : null;
                String drctnlt = (String) result[6];

                Integer newsSummaryId = result[7] != null
                    ? ((Number) result[7]).intValue()
                    : null;
                String briefSummary = (String) result[8];
                String summary = (String) result[9];
                String provider = (String) result[10];
                String title = (String) result[11];

                String regionsStr = (String) result[12];

                List<String> regionList = parseGroupConcatResult(regionsStr);

                return new NewsfeedListRes(
                    nwsSigId,
                    dataRegSn,
                    firstCreated,
                    nwsMrktCtgrCd,
                    newsSummaryId,
                    briefSummary,
                    summary != null ? Arrays.asList(summary.split("\n")) : null,
                    title,
                    provider,
                    impctScre,
                    drctnlt,
                    regionList
                );
            })
            .toList();
    }
}
