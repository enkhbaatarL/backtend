package com.sk.common.utils.Properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "mail.smtp")
public class MailProperties {
    private String host;
    private int port;
    private String username;
    private String password;
    private String from;

    private Properties properties = new Properties(); // Properties 객체로 통합

    @Getter
    @Setter
    public static class Properties {
        private String transportProtocol;
        private String auth;
        private String sslEnable;
        private String starttlsEnable;
        private String sslProtocols;
        private String connectiontimeout;
        private String debug;
    }
}