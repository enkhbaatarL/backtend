package com.sk.common.config.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class RedisConfig {

    // Redis 헬스체크용
    @Bean
    public ApplicationRunner redisHealthChecker(RedisConnectionFactory cf) {
        return args -> {
            try {
                String pong = cf.getConnection().ping();
                log.info("---> Redis Health Check Success... PING = {}", pong);
            } catch (Exception e) {
                log.error("---> Redis Health Check Fail...", e);
                throw e; // 실패 시 기동 중단
            }
        };
    }

    // Redis Connection Factory 설정 (Standalone 모드)
    @Bean
    @Profile(value = "local")
    public LettuceConnectionFactory lettuceConnectionFactoryLocal(@Value("${spring.data.redis.host}") String host,
                                                                  @Value("${spring.data.redis.port}") int port,
                                                                  @Value("${spring.data.redis.password}") String password,
                                                                  @Value("${spring.data.redis.command-timeout}") Duration commandTimeout) {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(host);
        redisConfig.setPort(port);
        redisConfig.setPassword(password);

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .commandTimeout(commandTimeout)
            .useSsl()
            .build();

        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }

    @Bean
    @Profile(value = "tech")
    public LettuceConnectionFactory lettuceConnectionFactoryTech(@Value("${spring.data.redis.host}") String host,
                                                                 @Value("${spring.data.redis.port}") int port,
                                                                 @Value("${spring.data.redis.password}") String password,
                                                                 @Value("${spring.data.redis.command-timeout}") Duration commandTimeout) {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(host);
        redisConfig.setPort(port);
        // redisConfig.setPassword(password);

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .commandTimeout(commandTimeout)
            // .useSsl()
            .build();

        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }

    // Redis Connection Factory 설정 (Sentinel 모드)
    @Bean
    @Profile({"dev", "prod"})
    public LettuceConnectionFactory lettuceConnectionFactoryDev(@Value("${redis.service-name}") String serviceName,
                                                                @Value("${redis.redis-password}") String redisPassword,
                                                                @Value("${redis.sentinel-host-node}") String sentinelHostNode,
                                                                @Value("${redis.sentinel-port}") int sentinelPort,
                                                                @Value("${redis.sentinel-password}") String sentinelPassword) {
        // Sentinel 설정 (마스터 이름과 Sentinel 노드 정보 설정)
        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration()
            .master(serviceName)
            .sentinel(sentinelHostNode, sentinelPort);

        sentinelConfig.setSentinelPassword(RedisPassword.of(sentinelPassword));
        sentinelConfig.setPassword(RedisPassword.of(redisPassword));

        return new LettuceConnectionFactory(sentinelConfig);
    }

    // 객체 저장용 RedisTemplate 설정
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return redisTemplate;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate stringRedisTemplate = new StringRedisTemplate();
        stringRedisTemplate.setConnectionFactory(redisConnectionFactory);
        return stringRedisTemplate;
    }

    // 캐시 매니저 설정
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory, @Value("${cache.redis.time-to-live}") Duration cacheTimeToLive) {
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(cacheTimeToLive)
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();

        // 캐시별 TTL 설정
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        cacheConfigurations.put(CacheType.PERMISSIONS.getCacheName(),
            defaultCacheConfig
                .entryTtl(Duration.ofMinutes(CacheType.PERMISSIONS.getTimeToLiveMinutes()))
                .prefixCacheNameWith(CacheType.PERMISSIONS.getCachePrefix()));

        cacheConfigurations.put(CacheType.SCHEMA_COLUMN_TYPES.getCacheName(),
            defaultCacheConfig
                .entryTtl(Duration.ofMinutes(CacheType.SCHEMA_COLUMN_TYPES.getTimeToLiveMinutes()))
                .prefixCacheNameWith(CacheType.SCHEMA_COLUMN_TYPES.getCachePrefix()));

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultCacheConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }

    @Bean
    public DefaultRedisScript<Long> quotaLuaScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("quota.lua"));
        script.setResultType(Long.class);
        return script;
    }

}
