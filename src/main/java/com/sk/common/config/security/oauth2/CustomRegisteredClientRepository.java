package com.sk.common.config.security.oauth2;

import com.sk.domain.shared.entity.master.Oauth2RegisteredClient;
import com.sk.domain.shared.enums.Role;
//import com.sk.domain.shared.repository.master.Oauth2RegisteredClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


import java.time.Duration;
import java.util.UUID;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomRegisteredClientRepository implements RegisteredClientRepository {

//    private final Oauth2RegisteredClientRepository oauth2RegisteredClientRepository;

    @Override
    public void save(RegisteredClient registeredClient) {
        // todo 필요 시
    }

    @Override
    public RegisteredClient findById(String id) {
        return null;
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {

//        Oauth2RegisteredClient client = oauth2RegisteredClientRepository.findByClientId(clientId)
//            .orElseThrow(() -> new AuthenticationServiceException("clientId: %s".formatted(clientId)));
//
//        return RegisteredClient.withId(UUID.randomUUID().toString())
//            .clientId(client.getClientId())
//            .clientSecret(client.getClientSecret())
//            .clientAuthenticationMethod(new ClientAuthenticationMethod(client.getClientAuthenticationMethod()))
//            .authorizationGrantType(new AuthorizationGrantType(client.getAuthorizationGrantTypes()))
//            .scope(client.getScopes())
//            .clientSettings(ClientSettings.builder()
//                .setting("tenant", client.getTenant())
//                .setting("role", Role.CLIENT)
//                .requireProofKey(false)  // PKCE(Proof Key for Code Exchange) 필요 없음 - 클라이언트 크리덴셜에서는 사용 안함
//                .requireAuthorizationConsent(false)  // 명시적 동의 필요 없음
//                .build())
//            .tokenSettings(TokenSettings.builder()
//                // 토큰 수명 설정
//                .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)  // JWT 토큰 형식 사용
//                .reuseRefreshTokens(false)
//                .build())
//            .build();
        return null;
    }

}
