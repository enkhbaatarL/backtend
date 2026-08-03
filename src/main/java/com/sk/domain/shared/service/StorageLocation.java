package com.sk.domain.shared.service;

import java.util.Objects;

public record StorageLocation(String bucket, String key) {

    public StorageLocation {
        Objects.requireNonNull(bucket, "bucket must not be null");
        Objects.requireNonNull(key, "key must not be null");
    }
}
