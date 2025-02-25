package com.sk.mi;

import com.sk.mi.util.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class MiPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(MiPlatformApplication.class, args);
	}

}
