package com.sk.domain.mi.controller;

import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.GoodsAuthorityRes;
import com.sk.domain.mi.service.MiUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "01. 사용자 API", description = "사용자 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/user-info")
@RequiredArgsConstructor
public class MiUserController {

    private final MiUserService miUserService;

    @Operation(summary = "사용자 상품권한 목록 조회 API", description = "사용자 로그인 ID를 통해 사용자의 상품권한 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/user/{userLoginId}/permissions")
    public BaseResponse<List<GoodsAuthorityRes>> getUserGoodsAuthorities(
        @PathVariable(value = "userLoginId") String userLoginId
    ) {
        return BaseResponseFactory.create(miUserService.getUserGoodsAuthorities(userLoginId));
    }

}
