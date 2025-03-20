package com.sk.common.config.redis;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Cache {

    PERMISSIONS("perm:", "goods-permissions", 30);

    public static final String PERMISSIONS_CONSTANT = "goods-permissions";

    private final String cachePrefix;
    private final String cacheName;
    private final long ttl;

}
