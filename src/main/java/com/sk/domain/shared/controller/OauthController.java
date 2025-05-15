package com.sk.domain.shared.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.enums.ResultCode;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.shared.dto.response.OAuth2Res;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "03. Oauth2 API", description = "Oauth2 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/oauth2")
@RequiredArgsConstructor
public class OauthController {

    @ApiOrder(1)
    @Operation(summary = "Oauth2 인증토큰 발급 API", description = """
        Oauth2 인증토큰 발급 API 입니다.
        - Header: Authorization Basic Base64{클라이언트_ID:클라이언트_SECRET}
        - Header: Content-Type application/x-www-form-urlencoded
        - Body: grant_type=client_credentials
        """)
    @PostMapping("/token")
    public BaseResponse<OAuth2Res> generateToken(
        @Parameter(description = "Basic 클라이언트_ID:클라이언트_SECRET") @RequestHeader(value = "Authorization") String authorization,
        @RequestBody String grant_type
    ) {
        return BaseResponseFactory.create(ResultCode.Success.OK);
    }

}
