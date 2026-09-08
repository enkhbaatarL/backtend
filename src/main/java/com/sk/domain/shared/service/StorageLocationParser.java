package com.sk.domain.shared.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import java.net.URI;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StorageLocationParser {

    private static final String HTTPS_SCHEME = "https";
    private static final int DEFAULT_HTTPS_PORT = 443;

    private final EndpointAuthority allowedEndPoint;
    private final String commBucketName;
    private final String prefix;

    public StorageLocationParser(
            @Value("${ncp.storage.end-point}") String endPoint,
            @Value("${ncp.storage.bucket-name}") String commBucketName,
            @Value("${ncp.storage.prefix}") String prefix) {
        this.allowedEndPoint = parseConfiguredEndPoint(endPoint);
        this.commBucketName = commBucketName;
        this.prefix = prefix;
    }

    public StorageLocation parse(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            throw invalidStorageLocation();
        }

        URI storageUri = parseUri(storagePath);
        if (!isValidStorageUri(storageUri)
                || !allowedEndPoint.equals(EndpointAuthority.from(storageUri))) {
            throw invalidStorageLocation();
        }

        String path = storageUri.getRawPath();
        if (path == null || path.isBlank()) {
            throw invalidStorageLocation();
        }

        String pathWithoutLeadingSlash = path.startsWith("/") ? path.substring(1) : path;
        int bucketSeparatorIndex = pathWithoutLeadingSlash.indexOf('/');
        if (bucketSeparatorIndex <= 0
                || bucketSeparatorIndex == pathWithoutLeadingSlash.length() - 1) {
            throw invalidStorageLocation();
        }

        String bucket = pathWithoutLeadingSlash.substring(0, bucketSeparatorIndex);
        String key = pathWithoutLeadingSlash.substring(bucketSeparatorIndex + 1);
        if (bucket.isBlank() || key.isBlank() || !isAllowedBucket(bucket)) {
            throw invalidStorageLocation();
        }

        return new StorageLocation(bucket, key);
    }

    private EndpointAuthority parseConfiguredEndPoint(String value) {
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw invalidEndPointConfiguration();
        }
//        if (!isCanonicalEndPoint(uri)) {
//            throw invalidEndPointConfiguration();
//        }
        return EndpointAuthority.from(uri);
    }

    private URI parseUri(String value) {
        if (value == null || value.isBlank()) {
            throw invalidStorageLocation();
        }
        try {
            return URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw invalidStorageLocation();
        }
    }

    private boolean isCanonicalEndPoint(URI uri) {
        String path = uri.getRawPath();
        return isAbsoluteHttpsUri(uri)
                && hasCanonicalAuthority(uri)
                && (path == null || path.isEmpty() || "/".equals(path));
    }

    private boolean isValidStorageUri(URI uri) {
        return isAbsoluteHttpsUri(uri)
                && hasCanonicalAuthority(uri);
    }

    private boolean isAbsoluteHttpsUri(URI uri) {
        return uri.isAbsolute()
                && HTTPS_SCHEME.equalsIgnoreCase(uri.getScheme())
                && uri.getHost() != null;
    }

    private boolean hasCanonicalAuthority(URI uri) {
        String rawAuthority = uri.getRawAuthority();
        return rawAuthority != null
                && !rawAuthority.endsWith(":")
                && uri.getUserInfo() == null
                && uri.getQuery() == null
                && uri.getFragment() == null
                && (uri.getPort() == -1
                        || (uri.getPort() > 0 && uri.getPort() <= 65535));
    }

    private boolean isAllowedBucket(String bucket) {
        if (bucket.equals(commBucketName)) {
            return true;
        }
        return prefix != null
                && !prefix.isBlank()
                && bucket.startsWith(prefix)
                && bucket.length() > prefix.length();
    }

    private SystemErrorException invalidStorageLocation() {
        return SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR);
    }

    private IllegalStateException invalidEndPointConfiguration() {
        return new IllegalStateException("Invalid object storage endpoint configuration");
    }

    private record EndpointAuthority(String host, int port) {

        private static EndpointAuthority from(URI uri) {
            int effectivePort = uri.getPort() == -1 ? DEFAULT_HTTPS_PORT : uri.getPort();
            return new EndpointAuthority(uri.getHost().toLowerCase(Locale.ROOT), effectivePort);
        }
    }
}
