package com.sk.domain.shared.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.UserResolve;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.shared.dto.response.common.UserDetailRes;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.service.CommonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "04. Common API", description = "공통 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/common")
@RequiredArgsConstructor
public class CommonController {

    private final CommonService commonService;

    @ApiOrder(1)
    @Operation(summary = "사용자 정보 조회 API", description = "인증된 사용자 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/user-detail")
    public BaseResponse<UserDetailRes> getUserInfo(@UserResolve User user) {
        return BaseResponseFactory.create(commonService.getUserDetail(user));
    }

}
