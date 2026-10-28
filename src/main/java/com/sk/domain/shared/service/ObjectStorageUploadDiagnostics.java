package com.sk.domain.shared.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.Request;
import com.amazonaws.handlers.HandlerAfterAttemptContext;
import com.amazonaws.handlers.HandlerBeforeAttemptContext;
import com.amazonaws.handlers.HandlerContextKey;
import com.amazonaws.handlers.RequestHandler2;
import com.amazonaws.http.HttpResponse;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.PutObjectRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/** 공통 파일 업로드 안의 SDK 시도만 진단한다. 원문 key/헤더/예외 메시지는 기록하지 않는다. */
@Slf4j
public final class ObjectStorageUploadDiagnostics extends RequestHandler2 {
    private static final ThreadLocal<Upload> ACTIVE = new ThreadLocal<>();
    private static final HandlerContextKey<Call> CALL = new HandlerContextKey<>(
            "ObjectStorageUploadDiagnostics.Call");

    static Upload begin(String bucket, String key, long size) {
        Upload upload = new Upload(bucket, key, size, ACTIVE.get());
        ACTIVE.set(upload);
        return upload;
    }

    @Override
    public void beforeRequest(Request<?> request) {
        Upload upload = ACTIVE.get();
        if (upload == null) return;
        boolean filePut = request.getOriginalRequest() instanceof PutObjectRequest put
                && Objects.equals(put.getBucketName(), upload.bucket)
                && Objects.equals(put.getKey(), upload.key);
        request.addHandlerContext(CALL, new Call(upload,
                request.getOriginalRequest().getClass().getSimpleName(), filePut));
    }

    @Override
    public void beforeAttempt(HandlerBeforeAttemptContext context) {
        Call call = context.getRequest().getHandlerContext(CALL);
        if (call == null) return;
        call.attempt++;
        call.started = System.nanoTime();
        call.upload.sdkAttempts++;
        if (call.attempt > 1) call.upload.sdkRetryCount++;
        if (call.filePut) {
            call.upload.putAttempts++;
            if (call.attempt > 1) call.upload.putRetryCount++;
        }
    }

    @Override
    public void afterAttempt(HandlerAfterAttemptContext context) {
        Call call = context.getRequest().getHandlerContext(CALL);
        if (call == null || call.attempt == 0) return;
        Exception failure = context.getException();
        HttpResponse response = context.getResponse() == null ? null
                : context.getResponse().getHttpResponse();
        AmazonServiceException serviceFailure = failure instanceof AmazonServiceException service
                ? service : null;
        int status = serviceFailure != null ? serviceFailure.getStatusCode()
                : response == null ? 0 : response.getStatusCode();
        Map<String, String> headers = serviceFailure != null ? serviceFailure.getHttpHeaders()
                : response == null ? null : response.getHeaders();
        String requestId = serviceFailure == null ? null : serviceFailure.getRequestId();
        if (requestId == null || requestId.isBlank()) requestId = header(headers, "x-amz-request-id");
        String extendedRequestId = failure instanceof AmazonS3Exception s3
                ? s3.getExtendedRequestId() : null;
        if (extendedRequestId == null || extendedRequestId.isBlank()) {
            extendedRequestId = header(headers, "x-amz-id-2");
        }
        String message = "Object storage upload attempt: fileId={} sizeBytes={} operation={} filePut={} "
                + "attempt={} retryCount={} attemptMs={} httpStatus={} errorCode={} requestId={} "
                + "extendedRequestId={} failureType={}";
        Object[] fields = {call.upload.fileId, call.upload.size, call.operation, call.filePut,
                call.attempt, Math.max(0, call.attempt - 1), elapsed(call.started), status,
                safe(serviceFailure == null ? null : serviceFailure.getErrorCode()), safe(requestId),
                safe(extendedRequestId), failure == null ? "NONE" : failure.getClass().getSimpleName()};
        if (failure == null && status < 400) log.info(message, fields);
        else log.warn(message, fields);
    }

    private static String header(Map<String, String> headers, String name) {
        if (headers == null) return null;
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(java.util.Map.Entry::getValue).findFirst().orElse(null);
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) return "NONE";
        return value.substring(0, Math.min(value.length(), 256))
                .replaceAll("[\\p{Cntrl}\\p{Zl}\\p{Zp}]", "_");
    }

    private static long elapsed(long started) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
    }

    static final class Upload implements AutoCloseable {
        private final String bucket;
        private final String key;
        private final String fileId;
        private final long size;
        private final long started = System.nanoTime();
        private final Upload previous;
        private int sdkAttempts;
        private int sdkRetryCount;
        private int putAttempts;
        private int putRetryCount;
        private boolean succeeded;

        private Upload(String bucket, String key, long size, Upload previous) {
            this.bucket = bucket;
            this.key = key;
            this.size = size;
            this.previous = previous;
            try {
                byte[] digest = MessageDigest.getInstance("SHA-256")
                        .digest((bucket + "/" + key).getBytes(StandardCharsets.UTF_8));
                fileId = HexFormat.of().formatHex(digest, 0, 8);
            } catch (NoSuchAlgorithmException impossible) {
                throw new IllegalStateException("SHA-256 unavailable", impossible);
            }
        }

        void succeeded() { succeeded = true; }

        @Override
        public void close() {
            if (previous == null) ACTIVE.remove();
            else ACTIVE.set(previous);
            String message = "Object storage upload completed: fileId={} sizeBytes={} outcome={} "
                    + "totalMs={} putAttempts={} putRetryCount={} sdkAttempts={} sdkRetryCount={}";
            Object[] fields = {fileId, size, succeeded ? "SUCCEEDED" : "FAILED", elapsed(started),
                    putAttempts, putRetryCount, sdkAttempts, sdkRetryCount};
            if (succeeded) log.info(message, fields);
            else log.warn(message, fields);
        }
    }

    private static final class Call {
        private final Upload upload;
        private final String operation;
        private final boolean filePut;
        private int attempt;
        private long started;

        private Call(Upload upload, String operation, boolean filePut) {
            this.upload = upload;
            this.operation = operation;
            this.filePut = filePut;
        }
    }
}
