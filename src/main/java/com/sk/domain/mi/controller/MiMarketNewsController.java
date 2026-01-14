package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.response.news.MarketNewsDetailRes;
import com.sk.domain.mi.dto.response.news.MarketNewsListRes;
import com.sk.domain.mi.dto.response.news.MarketRecapListRes;
import com.sk.domain.mi.dto.response.news.MarketRecapRes;
import com.sk.domain.mi.dto.response.news.NewsTimelineRes;
import com.sk.domain.mi.dto.response.news.NewsTrendDailyRes;
import com.sk.domain.mi.dto.response.news.NewsfeedListRes;
import com.sk.domain.mi.service.MiMarketNewsService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.enums.news.DateRangeOption;
import com.sk.domain.shared.enums.news.SortType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("!dedicated")
@Tag(name = "12. Market News API", description = "시장 뉴스 API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/market-news")
@RequiredArgsConstructor
public class MiMarketNewsController {

    private final MiMarketNewsService miMarketNewsService;

    @ApiOrder(1)
    @Parameter(name = "page", description = "페이지 번호 (기본값: 1)")
    @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 30)")
    @Parameter(name = "sort", description = "정렬기준", schema = @Schema(implementation = SortType.class))
    @Parameter(name = "keyword", description = "검색 키워드 (선택사항)")
    @Parameter(name = "startDate", description = "조회 시작일 (선택사항)")
    @Parameter(name = "endDate", description = "조회 종료일 (선택사항)")
    @Parameter(name = "categories", description = "카테고리 (선택사항)")
    @Parameter(name = "regions", description = "지역 (선택사항)")
    @Parameter(name = "industries", description = "산업 (선택사항)")
    @Operation(
        summary = "시장 뉴스 목록 조회",
        description = "시장 뉴스 목록을 페이징하여 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<PaginationRes<MarketNewsListRes>> getMarketNewsList(
        @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다") int page,
        @RequestParam(value = "size", defaultValue = "30") @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다") int size,
        @RequestParam(value = "sort", defaultValue = "NEWEST") SortType sort,
        @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
        @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyyMMdd") LocalDate startDate,
        @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyyMMdd") LocalDate endDate,
        @RequestParam(value = "categories", required = false) String categories,
        @RequestParam(value = "regions", required = false) String regions,
        @RequestParam(value = "industries", required = false) String industries
    ) {

        log.debug("시장 뉴스 목록 조회 요청 - page: {}, size: {}, keyword: {}", page, size, keyword);

        return BaseResponseFactory.create(
            miMarketNewsService.getMarketNewsList(page, size, sort, keyword, startDate, endDate,
                categories, regions, industries)
        );
    }

    @ApiOrder(2)
    @Parameter(name = "newsSummaryId", description = "뉴스 요약 ID", required = true)
    @Operation(
        summary = "시장 뉴스 상세 조회",
        description = "특정 시장 뉴스의 상세 정보를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{newsSummaryId}")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<MarketNewsDetailRes> getMarketNewsDetail(
        @PathVariable("newsSummaryId") String newsSummaryId) {

        log.debug("시장 뉴스 상세 조회 요청 - newsSummaryId: {}", newsSummaryId);

        return BaseResponseFactory.create(
            miMarketNewsService.getMarketNewsDetail(newsSummaryId)
        );
    }


    @ApiOrder(3)
    @Parameter(name = "criteriaDate", description = "기준 일자 (yyyyMMdd 형식)", required = true, example = "20250730")
    @Parameter(name = "sort", description = "정렬 방향 (기본값: NEWEST)")
    @Parameter(name = "category", description = "뉴스 시장 카테고리 코드 (기본값: CROIL)", required = true)
    @Parameter(name = "sentiments", description = "방향성 (복수 선택 가능)")
    @Parameter(name = "impactScores", description = "영향도 (복수 선택 가능)")
    @Parameter(name = "regions", description = "지역 (복수 선택 가능)")
    @Operation(
        summary = "시장 뉴스 피드 조회",
        description = "시장 뉴스를 피드 형태로 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/feed")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<List<NewsfeedListRes>> getMarketNewsFeed(
        @RequestParam(value = "criteriaDate") String criteriaDate,
        @RequestParam(value = "sort", required = false, defaultValue = "NEWEST") SortType sort,
        @RequestParam(value = "category", defaultValue = "CROIL") String category,
        @RequestParam(value = "sentiments", required = false) String sentiments,
        @RequestParam(value = "impactScores", required = false) String impactScores,
        @RequestParam(value = "regions", required = false) String regions) {

        log.debug("시장 뉴스 피드 조회 요청 - sort: {}, category: {}", sort, category);

        return BaseResponseFactory.create(
            miMarketNewsService.getNewsfeedList(sort, criteriaDate, category, sentiments, impactScores, regions)
        );
    }

    @ApiOrder(4)
    @Parameter(name = "criteriaDate", description = "기준 일자 (yyyyMMdd 형식)", required = true, example = "20250730")
    @Parameter(name = "category", description = "뉴스 시장 카테고리 코드", required = true, example = "CROIL")
    @Operation(
        summary = "마켓 리캡 조회 (날짜/카테고리)",
        description = "기준일자와 카테고리 코드로 마켓 리캡을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/recap")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<MarketRecapRes> getMarketRecap(
        @RequestParam(value = "criteriaDate") String criteriaDate,
        @RequestParam(value = "category") String category) {

        log.debug("마켓 리캡 조회 요청 - criteriaDate: {}, category: {}", criteriaDate, category);

        return BaseResponseFactory.create(
            miMarketNewsService.getMarketRecapByCriteriaDateAndCategory(criteriaDate, category)
        );
    }

    @ApiOrder(5)
    @Parameter(name = "mrktRcapId", description = "마켓 리캡 ID", required = true)
    @Operation(
        summary = "마켓 리캡 상세 조회",
        description = "마켓 리캡 ID로 상세 정보를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/recaps/{mrktRcapId}")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<MarketRecapRes> getMarketRecapById(
        @PathVariable("mrktRcapId") Long mrktRcapId) {

        log.debug("마켓 리캡 상세 조회 요청 - mrktRcapId: {}", mrktRcapId);

        return BaseResponseFactory.create(
            miMarketNewsService.getMarketRecapById(mrktRcapId)
        );
    }

    @ApiOrder(6)
    @Parameter(name = "criteriaDate", description = "기준 일자 (yyyyMMdd 형식)", required = true, example = "20250730")
    @Parameter(name = "category", description = "뉴스 시장 카테고리 코드 (기본값: CROIL)", required = true)
    @Operation(
        summary = "시장 뉴스 타임라인 조회",
        description = "시장 뉴스를 특정 점수 이상 최대 10개의 뉴스를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/timeline")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<List<NewsTimelineRes>> getMarketNewsTimeline(
       @RequestParam(value = "criteriaDate") String criteriaDate,
        @RequestParam(value = "category", defaultValue = "CROIL") String category) {

        log.debug("시장 뉴스 타임라인 조회 요청 - criteriaDate: {}, category: {}", criteriaDate, category);

        return BaseResponseFactory.create(miMarketNewsService.getNewsTimeline(criteriaDate, category));
    }

    @ApiOrder(7)
    @Parameter(name = "companyNo", description = "고객사 번호", required = true)
    @Parameter(name = "dateRangeOption", description = "날짜 범위 옵션", schema = @Schema(implementation = DateRangeOption.class))
    @Operation(
        summary = "일별 뉴스 트렌드 조회",
        description = "고객사의 시장 카테고리 기준으로 일별 뉴스 트렌드 데이터를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/daily-trend")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<NewsTrendDailyRes> getDailyNewsTrend(
        @RequestParam(value = "companyNo") Integer companyNo,
        @RequestParam(value = "dateRangeOption", defaultValue = "LAST_30_DAYS") DateRangeOption dateRangeOption
    ) {

        log.debug("일별 뉴스 트렌드 조회 요청 - companyNo: {}, dateRangeOption: {}",
            companyNo, dateRangeOption);

        return BaseResponseFactory.create(
            miMarketNewsService.getDailyTrend(companyNo, dateRangeOption)
        );
    }

    @ApiOrder(8)
    @Parameter(name = "page", description = "페이지 번호 (기본값: 1)")
    @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 20)")
    @Parameter(name = "sort", description = "정렬 기준", schema = @Schema(implementation = SortType.class))
    @Parameter(name = "keyword", description = "검색 키워드 (선택사항)")
    @Parameter(name = "dateRangeOption", description = "날짜 범위 옵션", schema = @Schema(implementation = DateRangeOption.class))
    @Parameter(name = "recapCategories", description = "리캡 카테고리")
    @Operation(
        summary = "마켓 리캡 목록 조회",
        description = "마켓 리캡 목록을 페이징하여 조회합니다. recapCategories 따라 HIGHLIGHT 또는 CHECKPOINT 아이템이 반환됩니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/recaps")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<PaginationRes<MarketRecapListRes>> getMarketRecaps(
        @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다") int page,
        @RequestParam(value = "size", defaultValue = "20") @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다") int size,
        @RequestParam(value = "sort", defaultValue = "NEWEST") SortType sort,
        @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
        @RequestParam(value = "dateRangeOption", defaultValue = "LAST_30_DAYS") DateRangeOption dateRangeOption,
        @RequestParam(value = "recapCategories", required = false) String recapCategories
    ) {

        log.debug("마켓 리캡 목록 조회 요청 - page: {}, size: {}, sort: {}, keyword: {}, dateRangeOption: {}, recapCategories: {}",
            page, size, sort, keyword, dateRangeOption, recapCategories);

        return BaseResponseFactory.create(
            miMarketNewsService.getMarketRecaps(page, size, sort, keyword, dateRangeOption, recapCategories)
        );
    }
}
