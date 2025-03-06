package com.sk.common.config.security.oauth2;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

public class CustomClientAuthenticationProvider implements AuthenticationProvider {

    private final RegisteredClientRepository registeredClientRepository;

    public CustomClientAuthenticationProvider(RegisteredClientRepository registeredClientRepository) {
        this.registeredClientRepository = registeredClientRepository;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        OAuth2ClientAuthenticationToken clientAuthentication =
            (OAuth2ClientAuthenticationToken) authentication;

        String clientId = clientAuthentication.getPrincipal().toString();
        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);

        if (registeredClient == null) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("invalid_client!!!!!!!!!!!!!!!!!", "Invalid client ID: " + clientId, null)
            );
        }

        String clientSecret = clientAuthentication.getCredentials() != null ?
            clientAuthentication.getCredentials().toString() : null;
        if (clientSecret == null || !registeredClient.getClientSecret().equals(clientSecret)) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("invalid_client!!!!!!!!!!!!!!!!", "Invalid client secret", null)
            );
        }

        return new OAuth2ClientAuthenticationToken(
            registeredClient,
            ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
            clientSecret
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
