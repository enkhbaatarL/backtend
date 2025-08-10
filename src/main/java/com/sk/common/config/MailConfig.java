package com.sk.common.config;

import java.util.Properties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import com.sk.common.properties.MailProperties;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {
    private static final String MAIL_TRANSPORT_PROTOCOL = "mail.transport.protocol";
    private static final String MAIL_SMTP_AUTH = "mail.smtp.auth";
    private static final String MAIL_SMTP_SSL_ENABLE = "mail.smtp.ssl.enable";
    private static final String MAIL_SMTP_STARTTLS_ENABLE = "mail.smtp.starttls.enable";
    private static final String MAIL_SMTP_SSL_PROTOCOLS = "mail.smtp.ssl.protocols";
    private static final String MAIL_CONNECTION_TIMEOUT = "mail.smtp.connectiontimeout";
    private static final String MAIL_DEBUG = "mail.smtp.debug";

    private final MailProperties mailProperties;

    @Bean
    public JavaMailSender javaMailService() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailProperties.getHost());
        mailSender.setPort(mailProperties.getPort());
        mailSender.setUsername(mailProperties.getUsername());
        mailSender.setPassword(mailProperties.getPassword());

        Properties props = mailSender.getJavaMailProperties();
        props.put(MAIL_TRANSPORT_PROTOCOL, mailProperties.getProperties().getTransportProtocol());
        props.put(MAIL_SMTP_AUTH, mailProperties.getProperties().getAuth());
        props.put(MAIL_SMTP_SSL_ENABLE, mailProperties.getProperties().getSslEnable()); // SSL 활성화
        props.put(MAIL_SMTP_STARTTLS_ENABLE, mailProperties.getProperties().getStarttlsEnable()); // STARTTLS 비활성화 (포트
                                                                                                  // 465에서는 불필요)
        props.put(MAIL_SMTP_SSL_PROTOCOLS, mailProperties.getProperties().getSslProtocols()); // TLS 버전 명시
        props.put(MAIL_DEBUG, mailProperties.getProperties().getDebug()); // 디버깅 활성화
        props.put(MAIL_CONNECTION_TIMEOUT, mailProperties.getProperties().getConnectiontimeout()); // 타임아웃 10초로 증가

        mailSender.setDefaultEncoding("UTF-8");
        mailSender.setJavaMailProperties(props);

        return mailSender;
    }
}
