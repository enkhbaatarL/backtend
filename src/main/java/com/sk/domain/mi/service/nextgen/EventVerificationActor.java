package com.sk.domain.mi.service.nextgen;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppHttpStatusException;
import com.sk.domain.shared.enums.Claim;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import org.springframework.http.HttpStatus;

final class EventVerificationActor {
    private EventVerificationActor() {}

    static String require(AuthorizationService authorizationService) {
        var jwt = authorizationService.getCurrentJwt();
        // client_credentials의 subject는 사용자 ID가 아닌 client ID다.
        if (jwt.getClaim(Claim.USER_NO.getCode()) == null) {
            throw AppHttpStatusException.of(HttpStatus.FORBIDDEN, ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS,
                "로그인 사용자 토큰으로 요청해 주세요.");
        }
        String actor = jwt.getSubject();
        if (actor == null || actor.isBlank() || actor.length() > 100) {
            throw AppHttpStatusException.of(HttpStatus.UNAUTHORIZED, ResultCode.Error.AUTH_INVALID_CREDENTIALS,
                "로그인 사용자 ID를 확인할 수 없습니다.");
        }
        return actor;
    }
}
