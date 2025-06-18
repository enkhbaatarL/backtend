package com.sk.common.config.redis;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CacheType {

    // === CacheManager ===
    PERMISSIONS("perm:", "permissions", 10L), // 10 minutes
    SCHEMA_COLUMN_TYPES("schema:", "column", 10L * 6 * 24 * 7), // 7 days
    // === CacheManager ===

    TAG_COMPANY("tag:", "company:user:", null),
    TAG_PROVIDER("tag:", "provider:user:", null),
    TAG_GOODS("tag:", "goods:user:", null),

    REFRESH_TOKEN("auth:", "refresh:user:", null),

    CHATBOT_TOKEN_USAGE("chatbot:", "token-usage:company:%s:%s", null);




    public static final String PERMISSIONS_CONSTANT = "permissions";
    public static final String SCHEMA_COLUMN_TYPES_CONSTANT = "column";

    private final String cachePrefix;
    private final String cacheName;
    private final Long timeToLiveMinutes;

}
