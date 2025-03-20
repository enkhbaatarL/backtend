package com.sk.domain.shared.service;

import com.sk.common.config.redis.CacheType;
import com.sk.domain.mi.dto.response.PermissionRes;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class CacheService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 사용자 권한에 따른 태그 캐시 업데이트
     *
     * @param userNo 사용자 번호
     * @param permissions 권한 객체
     */
    public void updateReverseIndices(Integer userNo, PermissionRes permissions) {

        String userKey = userNo.toString();

        permissions.getPermittedCompanies().forEach(companyNo ->
            redisTemplate.opsForSet().add("%s%s%s".formatted(CacheType.TAG_COMPANY.getCachePrefix(), CacheType.TAG_COMPANY.getCacheName(), companyNo), userKey));

        permissions.getPermittedProviders().forEach(providerNo ->
            redisTemplate.opsForSet().add("%s%s%s".formatted(CacheType.TAG_PROVIDER.getCachePrefix(), CacheType.TAG_PROVIDER.getCacheName(), providerNo), userKey));

        permissions.getPermittedGoods().forEach(goodsNo ->
            redisTemplate.opsForSet().add("%s%s%s".formatted(CacheType.TAG_GOODS.getCachePrefix(), CacheType.TAG_GOODS.getCacheName(), goodsNo), userKey));

    }

    /**
     * 태그 엔티티가 변경됨에 따라 사용자 권한 캐시 무효화
     * todo
     *  - 1. 적용 대상을 선정해야 함
     *  - 2. 태그 key 삭제 여부
     *  - 3. 회사 권한일 경우?
     *
     * @param cacheType TAG_COMPANY, TAG_PROVIDER, TAG_GOODS
     * @param entityId companyNo, providerNo, goodsNo
     */
    private void invalidateUsersForEntity(CacheType cacheType, Integer entityId) {

        String lookupKey = "%s%s%s".formatted(cacheType.getCachePrefix(), cacheType.getCacheName(), entityId);
        Set<String> affectedUsers = redisTemplate.opsForSet().members(lookupKey);


        if (ObjectUtils.isNotEmpty(affectedUsers)) {
            affectedUsers.forEach(userNo -> {
                // 1. 캐시에서 사용자 권한 삭제
                redisTemplate.delete("%s%s::%s".formatted(CacheType.PERMISSIONS.getCachePrefix(), CacheType.PERMISSIONS.getCacheName(), userNo));
                // 2. 역방향 인덱스에서 해당 사용자 제거
                // redisTemplate.opsForSet().remove(lookupKey, userNo);
            });
        }
    }

}
