package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.response.news.MarketNewsDetailRes;
import com.sk.domain.mi.dto.response.news.MarketNewsListRes;
import com.sk.domain.mi.dto.response.news.NewsfeedListRes;
import com.sk.domain.mi.service.MiMarketNewsService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.enums.news.SortType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 10)")
    @Parameter(name = "sort", description = "정렬기준", schema = @Schema(implementation = SortType.class))
    @Parameter(name = "keyword", description = "검색 키워드 (선택사항)")
    @Parameter(name = "startDate", description = "조회 시작일 (선택사항)")
    @Parameter(name = "endDate", description = "조회 종료일 (선택사항)")
    @Parameter(name = "category", description = "카테고리 (선택사항)")
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
        @RequestParam(value = "size", defaultValue = "10") @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다") int size,
        @RequestParam(value = "sort", required = false, defaultValue = "NEWEST") SortType sort,
        @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
        @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyyMMdd") LocalDate startDate,
        @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyyMMdd") LocalDate endDate,
        @RequestParam(value = "category", required = false, defaultValue = "") String category
    ) {

        log.debug("시장 뉴스 목록 조회 요청 - page: {}, size: {}, keyword: {}", page, size, keyword);

        return BaseResponseFactory.create(
            miMarketNewsService.getMarketNewsList(page, size, sort, keyword, startDate, endDate,
                category)
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
    @Parameter(name = "page", description = "페이지 번호 (기본값: 1)")
    @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 30)")
    @Parameter(name = "category", description = "뉴스 카테고리 )")
    @Operation(
        summary = "시장 뉴스 피드 조회",
        description = "시장 뉴스를 피드 형태로 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/feed")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<PaginationRes<NewsfeedListRes>> getMarketNewsFeed(
        @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다") int page,
        @RequestParam(value = "size", defaultValue = "30") @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다") int size,
        @RequestParam(value = "category") String category) {

        log.debug("시장 뉴스 피드 조회 요청 - page: {}, size: {}, category: {}",
            page, size, category);

        return BaseResponseFactory.create(
            miMarketNewsService.getNewsfeedList(page, size, SortType.NEWEST, LocalDateTime.parse("2025-07-29 07:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), LocalDateTime.parse("2025-09-15 07:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), category)
        );
    }
}
