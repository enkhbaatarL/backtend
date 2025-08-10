package com.sk.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secretKey;
    private Saml2 saml2;
    private Oauth2 oauth2;

    @Getter
    @Setter
    public static class Saml2 {
        private Duration accessTokenExpiry;
        private Duration refreshTokenExpiry;
    }

    @Getter
    @Setter
    public static class Oauth2 {
        private Duration accessTokenExpiry;
        private Duration refreshTokenExpiry;
    }

}
