package com.sk.domain.admin.controller;

import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.admin.dto.request.TestReq;
import com.sk.domain.mi.dto.response.GoodsAuthorityRes;
import com.sk.domain.admin.dto.response.TestRes;
import com.sk.domain.admin.service.TestService;
import com.sk.domain.shared.entity.master.tam.AuthManageGoodsMapping;
import com.sk.domain.shared.repository.master.tam.AuthManageGoodsMappingRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "00. 테스트 샘플 API", description = "테스트 샘플 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/sample")
@RequiredArgsConstructor
@Transactional
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
        List<AuthManageGoodsMapping> goodsMappings = authManageGoodsMappingRepository.getUserGoodsAuthoritiesByUserLoginId(param);
        return BaseResponseFactory.create(goodsMappings.stream().map(GoodsAuthorityRes::of).toList());
    }

}
