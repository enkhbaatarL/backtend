package com.sk.domain.shared.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.UserResolve;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.utils.DateUtil;
import com.sk.domain.shared.dto.request.common.UserFeedbackReq;
import com.sk.domain.shared.dto.response.common.CommonCodeDetailRes;
import com.sk.domain.shared.dto.response.common.UiComponentSettingRes;
import com.sk.domain.shared.dto.response.common.UserAccessPageRes;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.service.CommonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @ApiOrder(4)
    @Operation(summary = "공통코드상세 목록 조회 API", description = "코드그룹ID로 해당하는 공통코드상세 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @Parameter(name = "codeGroupId", description = "코드그룹ID", required = true, example = "nws_mrkt_ctgr_cd")
    @GetMapping("/codes/{codeGroupId}")
    public BaseResponse<List<CommonCodeDetailRes>> getCommonCodeDetails(
            @PathVariable("codeGroupId") String codeGroupId) {
        return BaseResponseFactory.create(commonService.getCommonCodeDetails(codeGroupId));
    }

    @ApiOrder(5)
    @Operation(summary = "UI 설정 목록 조회 API", description = "고객회사번호와 선택 파라미터로 UI 설정 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @Parameter(name = "companyNo", description = "고객회사번호", required = true, example = "2020")
    @Parameter(name = "screenCode", description = "화면코드", example = "MI_FORECAST")
    @Parameter(name = "componentTypeCode", description = "컴포넌트타입코드", example = "TABLE")
    @Parameter(name = "componentCode", description = "컴포넌트코드", example = "RECORDS")
    @GetMapping("/ui-component-settings")
    public BaseResponse<List<UiComponentSettingRes>> getUiComponentSettings(
        @RequestParam Integer companyNo,
        @RequestParam(required = false) String screenCode,
        @RequestParam(required = false) String componentTypeCode,
        @RequestParam(required = false) String componentCode) {
        return BaseResponseFactory.create(
            commonService.getUiComponentSettings(companyNo, screenCode, componentTypeCode,
                componentCode));
    }

    @ApiOrder(6)
    @Operation(
        summary = "사용자 피드백 등록 API",
        description = """
            사용자 피드백을 등록합니다. <br>
            여러 페이지에서 공통으로 사용되는 피드백 수집 API입니다.

            <br><br>
            ### 요청 필드
             - feedbackPageNm: 피드백 대상 페이지명 (선택) / example - "Next Gen"
             - feedbackSectionNm: 피드백 대상 섹션명 (선택) / example - "Overview", "Design"
             - feedbackTypeCd: 피드백 유형 코드 (필수)
             - feedbackContent: 피드백 내용 (선택, 최대 2000자)

            <br><br>
            ### 피드백 유형 코드 (feedbackTypeCd)
            - `agent_output`: Agent Output
            - `ux_ui`: UX/UI
            - `feature_suggestion`: 기능 제안
            - `etc`: 기타
            """
    )
    @ApiResponse(responseCode = "200", description = "등록 성공")
    @PostMapping("/user-feedback")
    public BaseResponse<Long> createUserFeedback(@UserResolve User user, @Valid @RequestBody UserFeedbackReq request) {
        return BaseResponseFactory.create(commonService.createUserFeedback(user, request));
    }

}
