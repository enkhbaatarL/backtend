package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode.Error;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.response.news.MarketNewsDetailRes;
import com.sk.domain.mi.dto.response.news.MarketNewsListRes;
import com.sk.domain.mi.dto.response.news.MarketRecapRes;
import com.sk.domain.mi.dto.response.news.NewsfeedListRes;
import com.sk.domain.shared.entity.master.tmd.MarketRecap;
import com.sk.domain.shared.entity.master.tmd.MarketRecapCheckpointItem;
import com.sk.domain.shared.entity.master.tmd.MarketRecapHighlightItem;
import com.sk.domain.shared.entity.master.tmd.MarketRecapHighlightItemRelationship;
import com.sk.domain.shared.entity.master.tmd.NewsSignificance;
import com.sk.domain.shared.entity.master.tmd.NewsSummary;
import com.sk.domain.shared.entity.master.tmd.SignificantNews;
import com.sk.domain.shared.enums.news.SortType;
import com.sk.domain.shared.repository.master.tmd.MarketRecapCheckpointItemRepository;
import com.sk.domain.shared.repository.master.tmd.MarketRecapHighlightItemRelationshipRepository;
import com.sk.domain.shared.repository.master.tmd.MarketRecapHighlightItemRepository;
import com.sk.domain.shared.repository.master.tmd.MarketRecapRepository;
import com.sk.domain.shared.repository.master.tmd.NewsSignificanceRepository;
import com.sk.domain.shared.repository.master.tmd.NewsSummaryRepository;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    public PaginationRes<MarketNewsListRes> getMarketNewsList(
            int page,
            int size,
            SortType sort,
            String keyword,
            LocalDate startDate,
            LocalDate endDate,
            String category) {

        // 페이지 설정 및 정렬
        Sort.Direction direction = (sort == SortType.OLDEST)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page - 1, size,
                Sort.by(direction, "refinitivNewsData.id.firstCreated", "refinitivNewsData.id.dataRegistrationSn"));

        // 검색 조건 생성
        Specification<NewsSummary> spec = createSpecification(keyword, startDate, endDate, category);

        // 데이터 조회
        Page<NewsSummary> newsPage = newsSummaryRepository.findAll(spec, pageable);

        // DTO 변환
        Page<MarketNewsListRes> newsListPage = newsPage.map(this::toMarketNewsListRes);

        return PaginationRes.toRes(newsListPage);
    }

    public List<NewsfeedListRes> getNewsfeedList(
            SortType sort,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            String category) {

        // 정렬 방향 설정
        Sort sortBy = Sort.by(Direction.DESC, "ns.firstCreated", "ns.dataRegistrationSn");

        switch (sort) {
            case OLDEST:
                sortBy = Sort.by(Direction.ASC, "ns.firstCreated", "ns.dataRegistrationSn");
                break;
            case NEWEST:
                sortBy = Sort.by(Direction.DESC, "ns.firstCreated", "ns.dataRegistrationSn");
                break;
            case HIGHEST:
                sortBy = Sort.by(Direction.DESC, "sn.impctScre");
                break;
            case LOWEST:
                sortBy = Sort.by(Direction.ASC, "sn.impctScre");
                break;
            default:
        }


        List<Object[]> results = newsSignificanceRepository.findNewsfeedWithFetchJoin(
                startDateTime, endDateTime, category, sortBy);

        return results.stream()
                .map(result -> {
                    NewsSignificance newsSignificance = (NewsSignificance) result[0];
                    SignificantNews significantNews = (SignificantNews) result[1];
                    NewsSummary newsSummary = (NewsSummary) result[2];

                    return toNewsfeedListRes(newsSignificance, newsSummary, significantNews);
                })
                .toList();
    }

    /**
     * 날짜와 카테고리로 MarketRecap 조회
     */
    public MarketRecapRes getMarketRecapByCriteriaDateAndCategory(String crtrDt, String nwsMrktCtgrCd) {
        MarketRecap marketRecap = marketRecapRepository.findByCrtrDtAndNwsMrktCtgrCd(crtrDt, nwsMrktCtgrCd)
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

    /**
     * 검색 조건 Specification 생성
     */
    private Specification<NewsSummary> createSpecification(
            String keyword,
            LocalDate startDate,
            LocalDate endDate,
            String category) {

        return (root, query, cb) -> {
            Specification<NewsSummary> spec = Specification.where(null);

            // keyword 검색 (title 또는 story에서 검색)
            if (StringUtils.hasText(keyword)) {
                spec = spec.and((r, q, criteriaBuilder) ->
                        criteriaBuilder.or(
                                criteriaBuilder.like(
                                        r.get("refinitivNewsData").get("title"),
                                        "%" + keyword + "%"
                                ),
                                criteriaBuilder.like(
                                        r.get("refinitivNewsData").get("story"),
                                        "%" + keyword + "%"
                                )
                        )
                );
            }

            // startDate 필터
            if (startDate != null) {
                LocalDateTime startDateTime = startDate.atStartOfDay();
                spec = spec.and((r, q, criteriaBuilder) ->
                        criteriaBuilder.greaterThanOrEqualTo(
                                r.get("refinitivNewsData").get("id").get("firstCreated"),
                                startDateTime
                        )
                );
            }

            // endDate 필터
            if (endDate != null) {
                LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
                spec = spec.and((r, q, criteriaBuilder) ->
                        criteriaBuilder.lessThanOrEqualTo(
                                r.get("refinitivNewsData").get("id").get("firstCreated"),
                                endDateTime
                        )
                );
            }

            // category 필터
            if (StringUtils.hasText(category)) {
                spec = spec.and((r, q, criteriaBuilder) -> {
                    Subquery<Long> subquery = q.subquery(Long.class);
                    Root<NewsSignificance> newsSignificance = subquery.from(NewsSignificance.class);
                    subquery.select(newsSignificance.get("nwsSigId"));
                    subquery.where(
                            criteriaBuilder.and(
                                    criteriaBuilder.equal(
                                            newsSignificance.get("dataRegistrationSn"),
                                            r.get("dataRegistrationSn")
                                    ),
                                    criteriaBuilder.equal(
                                            newsSignificance.get("firstCreated"),
                                            r.get("firstCreated")
                                    ),
                                    criteriaBuilder.equal(
                                            newsSignificance.get("nwsMrktCtgrCd"),
                                            category
                                    )
                            )
                    );
                    return criteriaBuilder.exists(subquery);
                });
            }

            return spec.toPredicate(root, query, cb);
        };
    }

    public MarketNewsDetailRes getMarketNewsDetail(String newsSummaryId) {

        NewsSummary newsData = newsSummaryRepository.findById(Integer.parseInt(newsSummaryId))
                .orElseThrow(() -> AppErrorException.of(Error.RSC_DATA_NOT_FOUND,
                        "뉴스를 찾을 수 없습니다. newsSummaryId: " + newsSummaryId));

        return toMarketNewsDetailRes(newsData);
    }

    /**
     * NewsSummary -> MarketNewsListRes 변환
     */
    private MarketNewsListRes toMarketNewsListRes(NewsSummary newsSummary) {

        return new MarketNewsListRes(
                newsSummary.getNewsSummaryId(),
                newsSummary.getRefinitivNewsData().getId().getDataRegistrationSn(),
                newsSummary.getRefinitivNewsData().getId().getFirstCreated(),
                newsSummary.getRefinitivNewsData().getTitle(),
                newsSummary.getProvider()
        );
    }

    /**
     * NewsSummary -> MarketNewsDetailRes 변환
     */
    private MarketNewsDetailRes toMarketNewsDetailRes(NewsSummary newsSummary) {
        return new MarketNewsDetailRes(
                newsSummary.getNewsSummaryId(),
                newsSummary.getRefinitivNewsData().getId().getDataRegistrationSn(),
                newsSummary.getRefinitivNewsData().getId().getFirstCreated(),
                newsSummary.getRefinitivNewsData().getTitle(),
                newsSummary.getRefinitivNewsData().getStory(),
                newsSummary.getProvider()
        );
    }

    /**
     * NewsSignificance, NewsSummary, SignificantNews -> NewsfeedListRes 변환
     */
    private NewsfeedListRes toNewsfeedListRes(
            NewsSignificance newsSignificance,
            NewsSummary newsSummary,
            SignificantNews significantNews) {

        return new NewsfeedListRes(
                newsSignificance.getNwsSigId(),
                newsSignificance.getDataRegistrationSn().intValue(),
                newsSignificance.getFirstCreated(),
                newsSignificance.getNwsMrktCtgrCd(),
                newsSummary != null ? newsSummary.getNewsSummaryId() : null,
                newsSummary != null ? newsSummary.getBriefSummary() : null,
                newsSummary != null ? Arrays.asList(newsSummary.getSummary().split("\n")) : null,
                newsSummary != null && newsSummary.getRefinitivNewsData() != null
                        ? newsSummary.getRefinitivNewsData().getTitle() : null,
                newsSummary != null ? newsSummary.getProvider() : null,
                significantNews != null ? significantNews.getImpctScre() : null,
                significantNews != null ? significantNews.getDrctnlt() : null
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

        // 하이라이트 아이템 DTO 변환
        List<MarketRecapRes.HighlightItemRes> highlightItemResList = highlightItems.stream()
                .map(highlightItem -> {
                    // 관련 뉴스 조회
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

                    return new MarketRecapRes.HighlightItemRes(
                            highlightItem.getMrktRcapHlItmId(),
                            highlightItem.getTitl(),
                            highlightItem.getCntt(),
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
                        checkpointItem.getCntt()
                ))
                .toList();

        return new MarketRecapRes(
                marketRecap.getMrktRcapId(),
                marketRecap.getCrtrDt(),
                marketRecap.getNwsMrktCtgrCd(),
                marketRecap.getUpwSgnlNwsCnt(),
                marketRecap.getNtrlSgnlNwsCnt(),
                marketRecap.getDwnSgnlNwsCnt(),
                marketRecap.getHiTitl(),
                marketRecap.getHiCntt(),
                marketRecap.getCkptTitl(),
                marketRecap.getCkptCntt(),
                highlightItemResList,
                checkpointItemResList
        );
    }

    public List<NewsfeedListRes> getNewsTimeline(
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        String category) {

        // ns.firstCreated, ns.dataRegistrationSn 기준 내림차순 정렬
        Sort sortBy = Sort.by(Sort.Direction.DESC, "ns.firstCreated", "ns.dataRegistrationSn");

        List<Object[]> results = newsSignificanceRepository.findNewsfeedWithFetchJoin(
            startDateTime, endDateTime, category, sortBy);

        return results.stream()
            .filter(result -> {
                SignificantNews significantNews = (SignificantNews) result[1];
                // impctScre가 8.0 이상인 뉴스만 필터링
                return significantNews != null &&
                    significantNews.getImpctScre() != null &&
                    significantNews.getImpctScre().compareTo(new java.math.BigDecimal("8.0")) >= 0;
            })
            .limit(10)  // 최대 10개로 제한
            .map(result -> {
                NewsSignificance newsSignificance = (NewsSignificance) result[0];
                SignificantNews significantNews = (SignificantNews) result[1];
                NewsSummary newsSummary = (NewsSummary) result[2];

                return toNewsfeedListRes(newsSignificance, newsSummary, significantNews);
            })
            .toList();
    }
}
