package com.sk.common.config.security.oauth2;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.BaseResponse;
import com.sk.common.utils.JsonUtil;
import com.sk.domain.shared.dto.response.OAuth2Res;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAccessTokenResponseHandler implements AuthenticationSuccessHandler {

    private final CustomAccessTokenResponseConverter customAccessTokenResponseConverter;

    public CustomAccessTokenResponseHandler(CustomAccessTokenResponseConverter customAccessTokenResponseConverter) {
        this.customAccessTokenResponseConverter = customAccessTokenResponseConverter;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {

        BaseResponse<OAuth2Res> res = customAccessTokenResponseConverter.convert((OAuth2AccessTokenAuthenticationToken) authentication);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        try {
            response.getWriter().write(JsonUtil.getMapper().writeValueAsString(res));
        } catch (IOException e) {
            throw AppErrorException.of(ResultCode.Error.SYS_UNKNOWN);
        }

    }

}
