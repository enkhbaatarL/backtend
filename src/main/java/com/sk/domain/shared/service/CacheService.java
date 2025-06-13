package com.sk.domain.shared.service;

import com.sk.common.config.redis.CacheType;
import com.sk.domain.mi.dto.response.PermissionRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 사용자 권한 목록을 기준으로 Company, Provider, Goods 태그 엔티티에 사용자를 추가한다.
     *
     * @param userNo      사용자 번호
     * @param permissions 권한 객체
     */
    public void updateReverseIndices(Integer userNo, PermissionRes permissions) {

        String userKey = userNo.toString();

        permissions.getPermittedCompanies().forEach(companyNo -> {
            String createdKey = "%s%s%s".formatted(CacheType.TAG_COMPANY.getCachePrefix(), CacheType.TAG_COMPANY.getCacheName(), companyNo);
            log.info("Redis key(company) has been created. {}", createdKey);
            redisTemplate.opsForSet().add(createdKey, userKey);
        });

        permissions.getPermittedProviders().forEach(providerNo -> {
            String createdKey = "%s%s%s".formatted(CacheType.TAG_PROVIDER.getCachePrefix(), CacheType.TAG_PROVIDER.getCacheName(), providerNo);
            log.info("Redis key(provider) has been created. {}", createdKey);
            redisTemplate.opsForSet().add(createdKey, userKey);
        });

        permissions.getPermittedGoods().forEach(goodsNo -> {
            String createdKey = "%s%s%s".formatted(CacheType.TAG_GOODS.getCachePrefix(), CacheType.TAG_GOODS.getCacheName(), goodsNo);
            log.info("Redis key(goods) has been created. {}", createdKey);
            redisTemplate.opsForSet().add(createdKey, userKey);
        });

    }

    /**
     * 태그 엔티티에 포함되어 있는 사용자 목록을 대상으로 캐시를 무효화한다.
     *
     * @param cacheType TAG_COMPANY, TAG_PROVIDER, TAG_GOODS
     * @param entityId  companyNo, providerNo, goodsNo
     */
    public void invalidateUsersForTagEntity(CacheType cacheType, Integer entityId) {

        String lookupKey = "%s%s%s".formatted(cacheType.getCachePrefix(), cacheType.getCacheName(), entityId);
        Set<String> affectedUsers = redisTemplate.opsForSet().members(lookupKey);


        if (ObjectUtils.isNotEmpty(affectedUsers)) {
            affectedUsers.forEach(userNo -> {
                // 1. 캐시에서 사용자 권한 삭제
                String permissionKey = "%s%s::%s".formatted(CacheType.PERMISSIONS.getCachePrefix(), CacheType.PERMISSIONS.getCacheName(), userNo);
                redisTemplate.delete(permissionKey);
                log.info("Redis key(permissions) has been removed. {}", permissionKey);
                // 2. 역방향 인덱스에서 해당 사용자 제거
                redisTemplate.opsForSet().remove(lookupKey, userNo);
                log.info("Element(userNo) '{}' has been removed from Redis key '{}'.", userNo, lookupKey);
            });
        }
    }

    /**
     * 사용자의 권한 캐시를 무효화한다.
     *
     * @param userNo 사용자 번호
     */
    public void invalidateUsers(Integer userNo) {
        String permissionKey = "%s%s::%s".formatted(CacheType.PERMISSIONS.getCachePrefix(), CacheType.PERMISSIONS.getCacheName(), userNo);
        redisTemplate.delete(permissionKey);
        log.info("Redis key(permissions) has been removed. {}", permissionKey);
    }

}
