package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.response.news.NewsTopicListRes;
import com.sk.domain.mi.dto.response.news.TopicInfoRes;
import com.sk.domain.mi.dto.response.news.TopicTrackerListRes;
import com.sk.domain.mi.service.MiTopicTrackerService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.enums.topic.DateRangeOption;
import com.sk.domain.shared.enums.topic.SortType;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("!dedicated")
@Tag(name = "17. Topic Tracker API", description = "Topic Tracker API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/topic-tracker")
@RequiredArgsConstructor
public class MiTopicTrackerController {

    private final MiTopicTrackerService miTopicTrackerService;
    private final AuthorizationService authorizationService;

    @ApiOrder(1)
    @Parameter(name = "page", description = "페이지 번호 (기본값: 1)")
    @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 20)")
    @Parameter(name = "sort", description = "정렬기준", schema = @Schema(implementation = SortType.class))
    @Parameter(name = "keyword", description = "검색 키워드 (선택사항)")
    @Parameter(name = "dateRangeOption", description = "날짜 범위 옵션", schema = @Schema(implementation = DateRangeOption.class))
    @Parameter(name = "topics", description = "토픽 (선택사항)")
    @Parameter(name = "regions", description = "지역 (선택사항)")
    @Operation(
        summary = "Topic Tracker 목록 조회",
        description = "Topic Tracker 목록을 페이징하여 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<PaginationRes<TopicTrackerListRes>> getTopicTrackerList(
        @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다") int page,
        @RequestParam(value = "size", defaultValue = "20") @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다") int size,
        @RequestParam(value = "sort", defaultValue = "NEWEST") SortType sort,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "dateRangeOption", defaultValue = "LAST_7_DAYS") DateRangeOption dateRangeOption,
        @RequestParam(value = "topics", required = false) String topics,
        @RequestParam(value = "regions", required = false) String regions
    ) {

        log.debug("Topic Tracker 목록 조회 요청 - page: {}, size: {}, keyword: {}", page, size, keyword);

        return BaseResponseFactory.create(
            miTopicTrackerService.getTopicTrackerList(page, size, sort, keyword, dateRangeOption,
                topics, regions)
        );
    }


    @ApiOrder(2)
    @Parameter(name = "criteriaDate", description = "기준 일자 (yyyyMMdd 형식)", example = "20251210")
    @Parameter(name = "newsTopicId", description = "뉴스 토픽 ID (선택, 값이 없으면 전체 조회)")
    @Operation(
        summary = "토픽 트래커 위젯용 조회",
        description = "대시보드 위젯용 토픽 트래커 목록을 조회합니다. 오늘 날짜 기준, 페이징 없이 전체 데이터를 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/widget")
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR, Role.API_SUB_ADMIN},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY
    )
    public BaseResponse<List<NewsTopicListRes>> getTopicTrackerWidget(
        @RequestParam(value = "criteriaDate") String criteriaDate,
        @RequestParam(value = "newsTopicId", required = false) Long newsTopicId
    ) {
        log.debug("토픽 트래커 위젯용 조회 요청 - criteriaDate: {}, newsTopicId: {}",
            criteriaDate, newsTopicId);

        return BaseResponseFactory.create(miTopicTrackerService.getNewsTopicList(criteriaDate, newsTopicId));
    }

    @ApiOrder(3)
    @Operation(
        summary = "토픽 목록 조회",
        description = "활성화된 토픽 목록을 조회합니다. (del_yn='N', use_yn='Y')"
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/topics")
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR, Role.API_SUB_ADMIN},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY
    )
    public BaseResponse<List<TopicInfoRes>> getTopicList() {
        log.debug("토픽 목록 조회 요청");
        int userNo = authorizationService.getUserNo();

        return BaseResponseFactory.create(miTopicTrackerService.getTopicList(userNo));
    }


}
