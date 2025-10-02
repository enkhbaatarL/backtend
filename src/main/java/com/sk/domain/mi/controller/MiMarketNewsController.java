package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.response.news.MarketNewsDetailRes;
import com.sk.domain.mi.dto.response.news.MarketNewsListRes;
import com.sk.domain.mi.service.MiMarketNewsService;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
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

    private final AuthorizationService authorizationService;
    private final MiMarketNewsService miMarketNewsService;

    /**
     * 시장 뉴스 목록 조회 - 페이징 및 키워드 검색 지원
     */
    @ApiOrder(1)
    @Parameter(name = "page", description = "페이지 번호 (기본값: 1)")
    @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 10)")
    @Parameter(name = "keyword", description = "검색 키워드 (선택사항)")
    @Operation(
        summary = "시장 뉴스 목록 조회",
        description = "시장 뉴스 목록을 페이징하여 조회합니다. 키워드 검색이 가능합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<PaginationRes<MarketNewsListRes>> getMarketNewsList(
        @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다") int page,
        @RequestParam(value = "size", defaultValue = "10") @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다") int size,
        @RequestParam(value = "keyword", required = false) String keyword) {

        log.debug("시장 뉴스 목록 조회 요청 - page: {}, size: {}, keyword: {}", page, size, keyword);

        return BaseResponseFactory.create(
            miMarketNewsService.getMarketNewsList(page, size, keyword)
        );
    }

    /**
     * 시장 뉴스 상세 조회 - 특정 뉴스의 전체 정보 조회
     */
    @ApiOrder(2)
    @Parameter(name = "newsSummaryId", description = "뉴스 요약 ID", required = true)
    @Operation(
        summary = "시장 뉴스 상세 조회",
        description = "특정 시장 뉴스의 상세 정보를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{newsSummaryId}")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<MarketNewsDetailRes> getMarketNewsDetail(
        @PathVariable("newsSummaryId") String newsSummaryId) {

        log.debug("시장 뉴스 상세 조회 요청 - newsSummaryId: {}", newsSummaryId);

        return BaseResponseFactory.create(
            miMarketNewsService.getMarketNewsDetail(newsSummaryId)
        );
    }

    /**
     * 시장 뉴스 피드 조회 - 최신 뉴스를 피드 형태로 제공 - 카테고리 필터링 지원
     */
    @ApiOrder(3)
    @Parameter(name = "page", description = "페이지 번호 (기본값: 1)")
    @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 20)")
    @Parameter(name = "category", description = "뉴스 카테고리 (예: STOCK, BOND, COMMODITY 등, 선택사항)")
    @Parameter(name = "fromDate", description = "조회 시작일 (형식: yyyy-MM-dd, 선택사항)")
    @Parameter(name = "toDate", description = "조회 종료일 (형식: yyyy-MM-dd, 선택사항)")
    @Operation(
        summary = "시장 뉴스 피드 조회",
        description = "시장 뉴스를 피드 형태로 조회합니다. 카테고리 및 날짜 범위로 필터링할 수 있습니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/feed")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<PaginationRes<MarketNewsListRes>> getMarketNewsFeed(
        @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다") int page,
        @RequestParam(value = "size", defaultValue = "20") @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다") int size,
        @RequestParam(value = "category", required = false) String category,
        @RequestParam(value = "fromDate", required = false) String fromDate,
        @RequestParam(value = "toDate", required = false) String toDate) {

        log.debug("시장 뉴스 피드 조회 요청 - page: {}, size: {}, category: {}, fromDate: {}, toDate: {}",
            page, size, category, fromDate, toDate);

        return BaseResponseFactory.create(
            miMarketNewsService.getMarketNewsList(page, size, "")
        );
    }
}
