package com.sk.common.config.redis;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CacheType {

    // CacheManager
    PERMISSIONS("perm:", "permissions", 10L),

    TAG_COMPANY("tag:", "company:user:", null),
    TAG_PROVIDER("tag:", "provider:user:", null),
    TAG_GOODS("tag:", "goods:user:", null),

    REFRESH_TOKEN("auth:", "refresh:user:", null);

    public static final String PERMISSIONS_CONSTANT = "permissions";

    private final String cachePrefix;
    private final String cacheName;
    private final Long timeToLiveMinutes;

}
