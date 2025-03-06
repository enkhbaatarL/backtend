package com.sk.common.config.security.oauth2;

import com.sk.domain.shared.entity.master.Oauth2RegisteredClient;
import com.sk.domain.shared.repository.master.Oauth2RegisteredClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CustomCustomRegisteredClientRepository implements RegisteredClientRepository {

    private final Oauth2RegisteredClientRepository oauth2RegisteredClientRepository;

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
        Oauth2RegisteredClient client = oauth2RegisteredClientRepository.findByClientId(clientId)
            .orElseThrow(() -> new OAuth2AuthenticationException("Invalid client ID: " + clientId));

        return RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId(client.getClientId())
            .clientSecret(client.getClientSecret())
            .clientAuthenticationMethod(new ClientAuthenticationMethod(client.getClientAuthenticationMethod()))
            .authorizationGrantType(new AuthorizationGrantType(client.getAuthorizationGrantTypes()))
            .scope(client.getScopes())
            .clientSettings(ClientSettings.builder()
                        .setting("tenant", "skinnovation")
                        .setting("permissions", Set.of("REFINITIV_VIEW", "REFINITIV_DOWNLOAD"))
                        .build())
            .tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofMinutes(600)) // todo properties 관리
                .build())
            .build();
    }

}
