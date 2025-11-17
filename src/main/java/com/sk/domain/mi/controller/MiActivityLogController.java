package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.activitylog.ActivityLogReq;
import com.sk.domain.mi.dto.response.activitylog.ActivityLogListRes;
import com.sk.domain.mi.service.MiActivityLogService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("!dedicated")
@Tag(name = "15. Activity Log API", description = "활동 기록 API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/activity-log")
@RequiredArgsConstructor
public class MiActivityLogController {

    private final MiActivityLogService miActivityLogService;

    @ApiOrder(1)
    @Parameter(name = "page", description = "페이지 번호 (기본값: 1)")
    @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 12)")
    @Parameter(name = "customerCompanyNo", description = "고객회사번호")
    @Parameter(name = "keyword", description = "검색 키워드 (선택사항)")
    @Parameter(name = "eventTypes", description = "이벤트타입 (복수개일 경우, 쉼표로 구분)")
    @Operation(
            summary = "활동 기록 목록 조회",
            description = "활동 기록 목록을 페이징하여 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    @RequiredPermission(
            roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR, Role.API_SUB_ADMIN},
            condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY
    )
    public BaseResponse<PaginationRes<ActivityLogListRes>> getActivityLogList(
            @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다") int page,
            @RequestParam(value = "size", defaultValue = "20") @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다") int size,
            @RequestParam(value = "customerCompanyNo") Integer customerCompanyNo,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "eventTypes", required = false) String eventTypes
    ) {

        log.debug("활동 기록 목록 조회 요청 - page: {}, size: {}, customerCompanyNo: {}, eventTypes: {}",
                page, size, customerCompanyNo, eventTypes);

        return BaseResponseFactory.create(
                miActivityLogService.getActivityLogList(page, size, customerCompanyNo, keyword, eventTypes)
        );
    }

    @ApiOrder(2)
    @Operation(
            summary = "활동 기록 추가",
            description = "새로운 활동 기록을 추가합니다."
    )
    @ApiResponse(responseCode = "200", description = "추가 성공")
    @PostMapping
    @RequiredPermission(
            roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR, Role.API_SUB_ADMIN},
            condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY
    )
    public BaseResponse<Long> createActivityLog(@Valid @RequestBody ActivityLogReq.Insert request) {

        log.debug("활동 기록 추가 요청 - eventType: {}, accountName: {}",
                request.eventType(), request.accountName());

        Long actLogId = miActivityLogService.createActivityLog(request);

        return BaseResponseFactory.create(actLogId);
    }
}
