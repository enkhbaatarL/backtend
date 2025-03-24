package com.sk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

import com.sk.common.utils.Properties.JwtProperties;

@EnableAsync
@EnableCaching
@EnableJpaAuditing
@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class MiPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(MiPlatformApplication.class, args);
	}

}
