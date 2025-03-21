package com.sk.domain.admin.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.admin.dto.request.TestReq;
import com.sk.domain.admin.dto.response.TestRes;
import com.sk.domain.admin.service.TestService;
import com.sk.domain.mi.dto.response.GoodsAuthorityRes;
import com.sk.domain.shared.entity.master.tam.AuthManageGoodsMapping;
import com.sk.domain.shared.entity.master.tlm.EmailAccessLog;
import com.sk.domain.shared.repository.master.tam.AuthManageGoodsMappingRepository;

// import com.sk.common.config.database.dataSource.DataSourceContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "00. 테스트 샘플 API", description = "테스트 샘플 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestController {

    private final TestService testService;
    private final AuthManageGoodsMappingRepository authManageGoodsMappingRepository;

    @Operation(summary = "사용자 정보 조회 API", description = "사용자 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/user-info")
    public BaseResponse<TestRes> getUserInfo(@RequestBody @Valid TestReq testReq) {
        return BaseResponseFactory.create(testService.getUserInfo(testReq));
    }

    @Operation(summary = "사용자 권한정보 조회 API", description = "사용자 권한 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/auth")
    public BaseResponse<List<GoodsAuthorityRes>> getAuthority(@RequestParam(value = "param") String param) {
        List<AuthManageGoodsMapping> goodsMappings = authManageGoodsMappingRepository
                .getUserGoodsAuthoritiesByUserLoginId(param);
        return BaseResponseFactory.create(goodsMappings.stream().map(GoodsAuthorityRes::of).toList());
    }

    @Operation(summary = "테스트 메일 발송 API", description = "테스트 메일을 발송합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/mail")
    public BaseResponse<Boolean> sendMail() {
        return BaseResponseFactory.create(testService.sendMail());
    }

    @Operation(summary = "테스트 유저 로그인", description = "테스트 유저 로그인을 진행합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/login")
    public BaseResponse<Boolean> userLogin() {
        return BaseResponseFactory.create(testService.userLogin());
    }

    @Operation(summary = "테스트 어드민 로그인", description = "테스트 어드민 로그인을 진행합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/admin-login")
    public BaseResponse<Boolean> adminLogin() {
        return BaseResponseFactory.create(testService.adminLogin());
    }

    @Operation(summary = "테스트(멀티테넌시 master mybatis) 알림 기록 조회", description = "테스트(멀티테넌시 master mybatis) 알림 기록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/master-email-access-log-mybatis")
    public BaseResponse<List<EmailAccessLog>> getMasterEmailAccessLogByMybatis() {
        return BaseResponseFactory.create(testService.getMasterEmailAccessLogByMybatis());
    }

    @Operation(summary = "테스트(멀티테넌시 secondary mybatis) 알림 기록 조회", description = "테스트(멀티테넌시 secondary mybatis) 알림 기록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/secondary-email-access-log-mybatis")
    public BaseResponse<List<EmailAccessLog>> getHynixEmailAccessLogByMybatis() {
        return BaseResponseFactory.create(testService.getHynixEmailAccessLogByMybatis());
    }
}
