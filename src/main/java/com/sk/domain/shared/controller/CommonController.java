package com.sk.domain.shared.controller;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.UserResolve;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.utils.DateUtil;
import com.sk.domain.shared.dto.response.common.UserAccessPageRes;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.service.CommonService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Profile("!dedicated")
@Tag(name = "04. Common API", description = "공통 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/common")
@RequiredArgsConstructor
public class CommonController {

    private final Environment environment;
    private final Optional<BuildProperties> build;
    private final CommonService commonService;

    @ApiOrder(1)
    @Operation(summary = "애플리케이션 Health Check API", description = "애플리케이션 Health Check를 위한 API입니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/health-check")
    public BaseResponse<?> getHealthCheck() {
        Map<String, String> info = Map.of(
            "activeProfiles", String.join(",", environment.getActiveProfiles()),
            "startedAt",
            build.map(BuildProperties::getTime)
                .map(t -> LocalDateTime.ofInstant(t, ZoneId.of("Asia/Seoul")))
                .orElse(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                .format(DateUtil.YYYY_MM_DD_HH_MM_SS)
        );
        return BaseResponseFactory.create(info);
    }

    @ApiOrder(2)
    @Operation(summary = "사용자 정보 조회 API", description = "인증된 사용자 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/user-detail")
    public BaseResponse<UserAccessPageRes> getUserInfo(@UserResolve User user, HttpServletRequest request) {
        return BaseResponseFactory.create(commonService.getUserDetail(user, request));
    }

    @ApiOrder(3)
    @Operation(summary = "Admin 페이지 접근 권한 확인 API(ADMIN, OPERATOR Token만 검증)", description = "Admin 페이지 접근 권한 확인을 위한 API입니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/admin-access-check")
    public BaseResponse<Boolean> adminAccessCheck(@UserResolve User user, HttpServletRequest request) {
        return BaseResponseFactory.create(commonService.adminAccessCheck(user, request));
    }
}
