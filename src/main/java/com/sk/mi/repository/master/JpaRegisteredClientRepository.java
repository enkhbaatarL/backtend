package com.sk.mi.repository.master;

import com.sk.mi.entity.master.OAuth2Client;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JpaRegisteredClientRepository implements RegisteredClientRepository {
    private final OAuth2ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void save(RegisteredClient registeredClient) {
        Assert.notNull(registeredClient, "registeredClient cannot be null");
        OAuth2Client client = toEntity(registeredClient);
        clientRepository.save(client);
    }

    @Override
    public RegisteredClient findById(String id) {
        Assert.hasText(id, "id cannot be empty");
        return clientRepository.findById(id)
                .map(this::toObject)
                .orElse(null);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        Assert.hasText(clientId, "clientId cannot be empty");
        return clientRepository.findByClientId(clientId)
                .map(this::toObject)
                .orElse(null);
    }

    private OAuth2Client toEntity(RegisteredClient registeredClient) {
        OAuth2Client entity = new OAuth2Client();
        entity.setId(registeredClient.getId());
        entity.setClientId(registeredClient.getClientId());
        
        // scopes 설정
        Set<String> scopes = new HashSet<>(registeredClient.getScopes());
        entity.setScopes(scopes);
        
        // ClientSettings에서 tenant와 permissions 가져오기
        ClientSettings clientSettings = registeredClient.getClientSettings();
        if (clientSettings.getSettings().containsKey("tenant")) {
            entity.setTenant((String) clientSettings.getSetting("tenant"));
        }
        
        if (clientSettings.getSettings().containsKey("permissions")) {
            @SuppressWarnings("unchecked")
            Set<String> permissions = (Set<String>) clientSettings.getSetting("permissions");
            entity.setPermissions(permissions);
        }
        
        // 비밀번호 설정
        String rawSecret = registeredClient.getClientSecret();
        entity.setClientSecret(rawSecret);

        return entity;
    }

    private RegisteredClient toObject(OAuth2Client client) {
        return RegisteredClient.withId(client.getId())
                .clientId(client.getClientId())
                .clientSecret(client.getClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scopes(scopes -> scopes.addAll(client.getScopes()))
                .clientSettings(ClientSettings.builder()
                    .setting("tenant", client.getTenant())
                    .setting("permissions", client.getPermissions())
                    .build())
                .build();
    }
} 