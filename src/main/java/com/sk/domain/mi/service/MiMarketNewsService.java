package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode.Error;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.response.news.MarketNewsDetailRes;
import com.sk.domain.mi.dto.response.news.MarketNewsListRes;
import com.sk.domain.mi.dto.response.news.NewsfeedListRes;
import com.sk.domain.shared.entity.master.tmd.NewsSignificance;
import com.sk.domain.shared.entity.master.tmd.NewsSummary;
import com.sk.domain.shared.entity.master.tmd.SignificantNews;
import com.sk.domain.shared.enums.news.SortType;
import com.sk.domain.shared.repository.master.tmd.NewsSignificanceRepository;
import com.sk.domain.shared.repository.master.tmd.NewsSummaryRepository;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    public PaginationRes<NewsfeedListRes> getNewsfeedList(
            int page,
            int size,
            SortType sort,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            String category) {

        // 페이지 설정 및 정렬
        Sort.Direction direction = (sort == SortType.OLDEST)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page - 1, size,
                Sort.by(direction, "refinitivNewsData.id.firstCreated", "refinitivNewsData.id.dataRegistrationSn"));

        // 검색 조건 생성
        Specification<NewsSignificance> spec = createNewsfeedSpecification(
                startDateTime, endDateTime, category);

        // 데이터 조회
        Page<NewsSignificance> newsPage = newsSignificanceRepository.findAll(spec, pageable);

        // DTO 변환
        Page<NewsfeedListRes> newsfeedPage = newsPage.map(this::toNewsfeedListRes);

        return PaginationRes.toRes(newsfeedPage);
    }

    /**
     * 뉴스피드 검색 조건 Specification 생성
     */
    private Specification<NewsSignificance> createNewsfeedSpecification(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            String category) {

        return (root, query, cb) -> {
            Specification<NewsSignificance> spec = Specification.where(null);

            // firstCreated 기간 필터 (시작일시)
            if (startDateTime != null) {
                spec = spec.and((r, q, criteriaBuilder) ->
                        criteriaBuilder.greaterThanOrEqualTo(
                                r.get("firstCreated"),
                                startDateTime
                        )
                );
            }

            // firstCreated 기간 필터 (종료일시)
            if (endDateTime != null) {
                spec = spec.and((r, q, criteriaBuilder) ->
                        criteriaBuilder.lessThanOrEqualTo(
                                r.get("firstCreated"),
                                endDateTime
                        )
                );
            }

            spec = spec.and((r, q, criteriaBuilder) -> {
                Subquery<Long> subquery = q.subquery(Long.class);
                Root<SignificantNews> significantNews = subquery.from(SignificantNews.class);
                subquery.select(significantNews.get("sgtNwsId"));
                subquery.where(
                        criteriaBuilder.equal(
                                significantNews.get("nwsSigId"),
                                r.get("nwsSigId")
                        )
                );
                return criteriaBuilder.exists(subquery);
            });

            // 카테고리 필터
            if (StringUtils.hasText(category)) {
                spec = spec.and((r, q, criteriaBuilder) ->
                        criteriaBuilder.equal(r.get("nwsMrktCtgrCd"), category)
                );
            }

            return spec.toPredicate(root, query, cb);
        };
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
                    // EXISTS 서브쿼리로 NewsSignificance와 조인
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
     * NewsSignificance -> NewsfeedListRes 변환
     */
    private NewsfeedListRes toNewsfeedListRes(NewsSignificance newsSignificance) {
        return new NewsfeedListRes(
                newsSignificance.getNwsSigId(),
                newsSignificance.getDataRegistrationSn(),
                newsSignificance.getFirstCreated(),
                newsSignificance.getNwsMrktCtgrCd(),
                newsSignificance.getSig()
        );
    }
}
