package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.enums.ResultCode.Success;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.response.economiccalendar.EconomicCalendarDetailRes;
import com.sk.domain.mi.dto.response.economiccalendar.EconomicCalendarHistoryRes;
import com.sk.domain.mi.dto.response.economiccalendar.EconomicCalendarListRes;
import com.sk.domain.mi.dto.response.economiccalendar.EconomicCalendarWidgetRes;
import com.sk.domain.mi.dto.response.economiccalendar.KeyEconomicIndicatorsRes;
import com.sk.domain.mi.dto.response.economiccalendar.RecentViewedEconomicCalendarRes;
import com.sk.domain.mi.service.MiEconomicCalendarService;
import com.sk.domain.shared.enums.EconomicCalendarDateRange;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("!dedicated")
@Tag(name = "16. Economic Calendar API", description = "경제 지표 달력 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/econ-cal")
@RequiredArgsConstructor
public class MiEconomicCalendarController {

    private final MiEconomicCalendarService miEconomicCalendarService;
    private final AuthorizationService authorizationService;

    @ApiOrder(1)
    @Parameter(name = "page", description = "페이지 번호 (기본값: 1)")
    @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 20)")
    @Parameter(name = "dateRange", description = "날짜 범위 (TODAY, YESTERDAY, TOMORROW, THISWEEK, NEXTWEEK)", schema = @Schema(implementation = EconomicCalendarDateRange.class))
    @Parameter(name = "keyword", description = "검색 키워드 (선택사항)")
    @Parameter(name = "categories", description = "카테고리 (선택사항)")
    @Parameter(name = "importance", description = "중요도 (선택사항, HIGH=3, MEDIUM=2, LOW=1)")
    @Operation(
            summary = "경제 지표 달력 목록 조회",
            description = "경제 지표 달력 목록을 페이징하여 조회합니다. 날짜 범위를 지정하여 조회할 수 있습니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    @RequiredPermission(
            roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR, Role.API_SUB_ADMIN},
            condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY
    )
    public BaseResponse<PaginationRes<EconomicCalendarListRes>> getEconomicCalendarList(
            @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다") int page,
            @RequestParam(value = "size", defaultValue = "20") @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다") int size,
            @RequestParam(value = "dateRange") @NotNull(message = "날짜 범위는 필수입니다") String dateRangeStr,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "categories", required = false) String categories,
            @RequestParam(value = "importance", required = false) String importance
    ) {

        log.debug("경제 지표 달력 목록 조회 요청 - page: {}, size: {}, dateRange: {}, keyword: {}, categories: {}, importances: {}",
                page, size, dateRangeStr, keyword, categories, importance);

        if (dateRangeStr == null || dateRangeStr.isBlank()) {
            throw new IllegalArgumentException("날짜 범위는 필수입니다. (TODAY, YESTERDAY, TOMORROW, THISWEEK, NEXTWEEK 중 하나를 선택해주세요)");
        }

        EconomicCalendarDateRange dateRange = EconomicCalendarDateRange.findByCode(dateRangeStr);
        if (dateRange == null) {
            throw new IllegalArgumentException("유효하지 않은 날짜 범위입니다. (TODAY, YESTERDAY, TOMORROW, THISWEEK, NEXTWEEK 중 하나를 선택해주세요)");
        }

        return BaseResponseFactory.create(
                miEconomicCalendarService.getList(page, size, dateRange, keyword, categories, importance)
        );
    }

    @ApiOrder(2)
    @Parameter(name = "code", description = "RIC 코드 (예: DEZEWC=ECI)")
    @Operation(
            summary = "경제 지표 달력 상세 조회",
            description = "RIC 코드를 기준으로 가장 최신의 경제 지표 달력 데이터를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{code}")
    @RequiredPermission(
            roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR, Role.API_SUB_ADMIN},
            condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY
    )
    public BaseResponse<EconomicCalendarDetailRes> getEconomicCalendarDetail(
            @PathVariable("code") String code
    ) {
        log.debug("경제 지표 달력 상세 조회 요청 - code: {}", code);
        int userNo = authorizationService.getUserNo();

        return BaseResponseFactory.create(
                miEconomicCalendarService.getDetail(code, userNo)
        );
    }

    @ApiOrder(3)
    @Parameter(name = "code", description = "RIC 코드 (예: DEZEWC=ECI)")
    @Parameter(name = "page", description = "페이지 번호 (기본값: 1)")
    @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 20)")
    @Operation(
            summary = "경제 지표 달력 히스토리 조회",
            description = "RIC 코드를 기준으로 해당 경제 지표의 전체 히스토리를 최신순으로 페이징하여 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{code}/history")
    @RequiredPermission(
            roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR, Role.API_SUB_ADMIN},
            condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY
    )
    public BaseResponse<PaginationRes<EconomicCalendarHistoryRes>> getEconomicCalendarHistory(
            @PathVariable("code") String code,
            @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다") int page,
            @RequestParam(value = "size", defaultValue = "20") @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다") int size
    ) {
        log.debug("경제 지표 달력 히스토리 조회 요청 - code: {}, page: {}, size: {}", code, page, size);

        return BaseResponseFactory.create(
                miEconomicCalendarService.getHistory(code, page, size)
        );
    }

    @ApiOrder(4)
    @Parameter(name = "criteriaDate", description = "기준 일자 (yyyyMMdd 형식)", required = true, example = "20250730")
    @Parameter(name = "category", description = "뉴스 시장 카테고리 코드", required = true, example = "CROIL")
    @Operation(
        summary = "주요 경제 지표 조회 (날짜/카테고리)",
        description = "기준일자와 카테고리 코드로 주요 경제지표와 분석내용을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/key")
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR, Role.API_SUB_ADMIN},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY
    )
    public BaseResponse<KeyEconomicIndicatorsRes> getKeyEconomicCalendarList(
        @RequestParam(value = "criteriaDate") String criteriaDate,
        @RequestParam(value = "category") String category
    ) {

        log.debug("주요 경제 지표 조회 요청 - criteriaDate: {}, category: {}", criteriaDate, category);

        return BaseResponseFactory.create(
            miEconomicCalendarService.getKeyList(criteriaDate, category)
        );
    }

    @ApiOrder(5)
    @Parameter(name = "criteriaDate", description = "기준 일자 (yyyyMMdd 형식)", example = "20250730")
    @Parameter(name = "categories", description = "카테고리 (선택사항)")
    @Parameter(name = "importance", description = "중요도 (선택사항, HIGH=3, MEDIUM=2, LOW=1)")
    @Operation(
        summary = "경제 지표 달력 위젯용 조회",
        description = "대시보드 위젯용 경제 지표 달력 목록을 조회합니다. 오늘 날짜 기준, 페이징 없이 전체 데이터를 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/widget")
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR, Role.API_SUB_ADMIN},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY
    )
    public BaseResponse<EconomicCalendarWidgetRes> getEconomicCalendarWidget(
        @RequestParam(value = "criteriaDate", required = false) String criteriaDate,
        @RequestParam(value = "categories", required = false) String categories,
        @RequestParam(value = "importance", required = false) String importance
    ) {
        log.debug("경제 지표 달력 위젯 조회 요청 - criteriaDate: {}, categories: {}, importance: {}",
            criteriaDate, categories, importance);

        return BaseResponseFactory.create(
            miEconomicCalendarService.getWidgetList(criteriaDate, categories, importance)
        );
    }

    @ApiOrder(6)
    @Operation(
        summary = "최근 조회 경제 지표 목록 조회",
        description = "사용자가 최근에 상세 조회한 경제 지표 목록 5개를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/recent-viewed")
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR, Role.API_SUB_ADMIN},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY
    )
    public BaseResponse<List<RecentViewedEconomicCalendarRes>> getRecentViewedList() {
        // AuthorizationService를 통해 현재 로그인한 사용자의 userNo를 가져옵니다.
        int userNo = authorizationService.getUserNo();
        log.debug("최근 조회 경제 지표 목록 요청 - userNo: {}", userNo);

        return BaseResponseFactory.create(
            miEconomicCalendarService.getRecentViewedList(userNo)
        );
    }

    @ApiOrder(7)
    @Operation(
        summary = "최근 조회 경제 지표 전체 삭제",
        description = "사용자의 최근 조회 경제 지표 로그를 모두 논리 삭제합니다."
    )
    @DeleteMapping("/recent-viewed")
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR, Role.API_SUB_ADMIN},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY
    )
    public BaseResponse<Void> deleteAllRecentViewed() {
        int userNo = authorizationService.getUserNo();
        log.debug("최근 조회 경제 지표 전체 삭제 요청 - userNo: {}", userNo);

        miEconomicCalendarService.deleteAllRecentViewed(userNo);
        return BaseResponseFactory.create(Success.OK);
    }
}
