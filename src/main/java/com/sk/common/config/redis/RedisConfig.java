package com.sk.common.config.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class RedisConfig {

    // Redis Connection Factory 설정
    @Bean
    public LettuceConnectionFactory lettuceConnectionFactory(@Value("${spring.data.redis.host}") String host,
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

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultCacheConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }

}
