package com.sk;

import com.sk.common.utils.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class MiPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(MiPlatformApplication.class, args);
	}

}
