package com.sk.domain.shared.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.shared.dto.request.file.PdgImageReq;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.SchedulerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 스케쥴러용 API
 */
@Profile("!dedicated")
@Tag(name = "06. Scheduler API", description = "스케쥴러 API")
@RestController
@RequestMapping("/api/v1/scheduler-management")
@RequiredArgsConstructor
public class SchedulerController {
    private final SchedulerService schedulerService;

    @ApiOrder(1)
    @Operation(summary = "GDI 탈퇴 유저 soft delete 처리 API", description = "GDI 탈퇴 유저 soft delete 처리 API 입니다.")
    @DeleteMapping("/group-users")
    @RequiredPermission(roles = {Role.API_SUB_ADMIN, Role.API_PLUS_OPERATOR, Role.ADMIN, Role.SYSTEM})
    @ApiResponse(responseCode = "200", description = "GDI 탈퇴 유저 soft delete 처리 성공")
    public BaseResponse<Boolean> processGdiUserSoftDelete() {
        return BaseResponseFactory.create(schedulerService.processGdiUserSoftDelete());
    }

}
