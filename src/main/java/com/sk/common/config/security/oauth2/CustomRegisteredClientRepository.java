package com.sk.common.config.security.oauth2;

import com.sk.domain.shared.entity.master.tif.ClientAccessInfo;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.repository.master.tif.ClientAccessInfoRepository;
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

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomRegisteredClientRepository implements RegisteredClientRepository {

    private final ClientAccessInfoRepository clientAccessInfoRepository;

    @Override
    public void save(RegisteredClient registeredClient) {
        // todo 필요 시
    }

    @Override
    public RegisteredClient findById(String id) {
        return findByClientId(id);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {

        ClientAccessInfo client = clientAccessInfoRepository.findByClientCertIdAndIsDeletedFalse(clientId)
            .orElseThrow(() -> new AuthenticationServiceException("clientId: %s".formatted(clientId)));

        return RegisteredClient.withId(clientId)
            .clientId(client.getClientCertId())
            .clientSecret(client.getClientSecretKey())
            .clientAuthenticationMethod(new ClientAuthenticationMethod(client.getCertMethodNm()))
            .authorizationGrantType(new AuthorizationGrantType(client.getCertTypeNm()))
            .clientSettings(ClientSettings.builder()
                .setting("companyNo", client.getCustomerCompany().getCustomerCompanyNo())
                .setting("companyCd", client.getCustomerCompany().getCustomerCompanyCd())
                .setting("role", client.getCustomerCompany().getIsMipManagementCompany() ? Role.API_SUB_ADMIN : Role.API_PLUS_OPERATOR)
                .requireProofKey(false)
                .requireAuthorizationConsent(false)  // 명시적 동의 필요 없음
                .build())
            .tokenSettings(TokenSettings.builder()
                .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)  // JWT 토큰 형식 사용
                .reuseRefreshTokens(false)
                .build())
            .build();
    }

}
