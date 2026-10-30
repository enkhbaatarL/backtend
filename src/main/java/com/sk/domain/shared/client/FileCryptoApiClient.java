package com.sk.domain.shared.client;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.ApiErrorException;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.shared.client.dto.request.FileCryptoDecryptReq;
import com.sk.domain.shared.client.dto.request.FileCryptoEncryptReq;
import com.sk.domain.shared.client.dto.response.FileCryptoDecryptRes;
import com.sk.domain.shared.client.dto.response.FileCryptoEncryptRes;
import com.sk.domain.shared.client.dto.response.FileCryptoStatusRes;
import com.sk.domain.shared.client.dto.response.FileCryptoUploadRes;
import com.sk.domain.shared.config.FileCryptoApiProperties;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileCryptoApiClient {

    private static final int LOG_DETAIL_MAX_LENGTH = 500;
    private static final int UPLOAD_SUCCESS = 1;
    private static final int ENCRYPT_SUCCESS = 0;
    private static final int DECRYPT_SUCCESS = 4;

    @Qualifier("fileCryptoRestClient")
    private final RestClient restClient;
    private final FileCryptoApiProperties properties;

    private static final java.util.concurrent.ScheduledExecutorService DEADLINES=java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r->{Thread thread=new Thread(r,"file-crypto-read-deadline");thread.setDaemon(true);return thread;});
    private final ThreadLocal<ReadBudget> readBudget=new ThreadLocal<>();
    private static final class ReadBudget {
        final java.util.function.IntSupplier remaining;
        final Runnable checkpoint;
        final java.util.List<java.util.concurrent.ScheduledFuture<?>> timers=new java.util.ArrayList<>();
        ReadBudget(java.util.function.IntSupplier remaining,Runnable checkpoint){this.remaining=remaining;this.checkpoint=checkpoint;}
        void finished(){timers.forEach(timer->timer.cancel(false));timers.clear();}
    }
    /** 읽기 호출 전체의 남은 기한을 모든 업로드/확인/복호화/다운로드와 재시도에 적용한다. */
    public byte[] decrypt(byte[] encrypted,String name,java.util.function.IntSupplier remaining,Runnable checkpoint) {
        if(readBudget.get()!=null)throw new IllegalStateException("nested crypto read budget");
        var budget=new ReadBudget(remaining,checkpoint);readBudget.set(budget);
        try {checkpoint.run();return decrypt(encrypted,name);}finally{budget.finished();readBudget.remove();}
    }
    private RestClient client() {
        var budget=readBudget.get();if(budget==null)return restClient;budget.checkpoint.run();
        var factory=new org.springframework.http.client.SimpleClientHttpRequestFactory(){
            @Override protected void prepareConnection(java.net.HttpURLConnection connection,String method)throws java.io.IOException {
                super.prepareConnection(connection,method);budget.checkpoint.run();int timeout=budget.remaining.getAsInt();if(timeout<=0)throw new java.net.SocketTimeoutException("crypto read deadline");
                connection.setConnectTimeout(timeout);connection.setReadTimeout(timeout);
                budget.timers.add(DEADLINES.schedule(connection::disconnect,timeout,java.util.concurrent.TimeUnit.MILLISECONDS));
            }
        };
        return restClient.mutate().requestFactory(factory).build();
    }

    public byte[] encrypt(byte[] plainBytes, String originalFileName) {
        log.info("File crypto encrypt flow started. inputBytes={}, fileExt={}",
                plainBytes.length, extensionForLog(originalFileName));
        FileCryptoUploadRes uploaded = upload(plainBytes, originalFileName);
        if (isEncrypted(uploaded.fid(), properties.getPid())) {
            log.info("File crypto encrypt skipped. reason=already-encrypted, fid={}, outputBytes={}",
                    fidForLog(uploaded.fid()), plainBytes.length);
            return plainBytes;
        }
        FileCryptoEncryptRes encrypted = encrypt(new FileCryptoEncryptReq(
                properties.getUserId(),
                properties.getLabelId(),
                uploaded.fid(),
                properties.getPid()));
        byte[] result = downloadAndVerify("encrypt", encrypted.outputFilePath(), encrypted.dataSize());
        log.info("File crypto encrypt flow completed. fid={}, outputBytes={}",
                fidForLog(uploaded.fid()), result.length);
        return result;
    }

    public byte[] decrypt(byte[] encryptedBytes, String originalFileName) {
        log.info("File crypto decrypt flow started. inputBytes={}, fileExt={}",
                encryptedBytes.length, extensionForLog(originalFileName));
        FileCryptoUploadRes uploaded = upload(encryptedBytes, originalFileName);
        if (!isEncrypted(uploaded.fid(), properties.getPid())) {
            log.info("File crypto decrypt skipped. reason=not-encrypted, fid={}, outputBytes={}",
                    fidForLog(uploaded.fid()), encryptedBytes.length);
            return encryptedBytes;
        }
        FileCryptoDecryptRes decrypted = decrypt(new FileCryptoDecryptReq(
                properties.getUserId(),
                uploaded.fid(),
                properties.getPid()));
        byte[] result = downloadAndVerify("decrypt", decrypted.outputFilePath(), decrypted.dataSize());
        log.info("File crypto decrypt flow completed. fid={}, outputBytes={}",
                fidForLog(uploaded.fid()), result.length);
        return result;
    }

    // 암복호화 서비스가 보고한 산출물 크기(dataSize)와 실제 다운로드 바이트 수를 교차검증해 잘림/손상을 탐지한다.
    // dataSize 의미가 불명확하거나 0/null인 경우는 검증을 건너뛴다.
    private byte[] downloadAndVerify(String operation, String outputFilePath, Long expectedSize) {
        byte[] data = download(outputFilePath);
        if (expectedSize != null && expectedSize > 0 && data.length != expectedSize) {
            throw cryptoFailure(operation,
                    "산출물 크기 불일치 (expected=%d, actual=%d)".formatted(expectedSize, data.length));
        }
        log.info("File crypto API {} download verified. expectedBytes={}, actualBytes={}",
                operation, expectedSize, data.length);
        return data;
    }

    FileCryptoUploadRes upload(byte[] fileBytes, String originalFileName) {
        return executeWithRetry("upload", () -> {
            MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
            multipart.add("file", new NamedByteArrayResource(fileBytes, originalFileName));

            FileCryptoUploadRes response = client().post()
                    .uri(cryptoUri(normalizePath(properties.getUploadPath()) + "/" + properties.getPid()))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(multipart)
                    .retrieve()
                    .body(FileCryptoUploadRes.class);

            if (response == null || response.result() != UPLOAD_SUCCESS || response.fid() == null
                    || StringUtils.hasText(response.errorMsg())) {
                throw cryptoFailure("upload", response == null ? null : response.errorMsg());
            }
            log.info("File crypto API upload response accepted. fid={}, inputBytes={}, result={}",
                    fidForLog(response.fid()), fileBytes.length, response.result());
            return response;
        });
    }

    boolean isEncrypted(UUID fid, String pid) {
        return executeWithRetry("status", () -> {
            FileCryptoStatusRes response = client().get()
                    .uri(cryptoUri(normalizePath(properties.getStatusPath()) + "/" + fid + "/" + pid))
                    .retrieve()
                    .body(FileCryptoStatusRes.class);

            if (response == null || response.result() == null || StringUtils.hasText(response.errorMsg())) {
                throw cryptoFailure("status", response == null ? null : response.errorMsg());
            }
            log.info("File crypto API status response accepted. fid={}, encrypted={}",
                    fidForLog(fid), response.result());
            return response.result();
        });
    }

    FileCryptoEncryptRes encrypt(FileCryptoEncryptReq request) {
        return executeWithRetry("encrypt", () -> {
            MultiValueMap<String, String> formData = request.toFormData();
            log.info("File crypto API encrypt request prepared. fid={}, fields={}",
                    fidForLog(request.fid()), formData.keySet());

            FileCryptoEncryptRes response = client().post()
                    .uri(cryptoUri(properties.getEncryptPath()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(FileCryptoEncryptRes.class);

            if (response == null || response.status() == null || response.status() != ENCRYPT_SUCCESS
                    || StringUtils.hasText(response.errorMsg()) || !StringUtils.hasText(response.outputFilePath())) {
                log.warn("File crypto API encrypt response rejected. requestFid={}, responseFid={}, status={}, outputPathPresent={}, errorPresent={}, errorDetail={}",
                        fidForLog(request.fid()), response == null ? "N/A" : fidForLog(response.fid()),
                        response == null ? null : response.status(),
                        response != null && StringUtils.hasText(response.outputFilePath()),
                        response != null && StringUtils.hasText(response.errorMsg()),
                        response == null ? "N/A" : sanitizeForLog(response.errorMsg()));
                throw cryptoFailure("encrypt", response == null ? null : response.errorMsg());
            }
            log.info("File crypto API encrypt response accepted. fid={}, status={}, outputPathPresent={}, dataSize={}",
                    fidForLog(response.fid()), response.status(), StringUtils.hasText(response.outputFilePath()),
                    response.dataSize());
            return response;
        });
    }

    FileCryptoDecryptRes decrypt(FileCryptoDecryptReq request) {
        return executeWithRetry("decrypt", () -> {
            FileCryptoDecryptRes response = client().post()
                    .uri(cryptoUri(properties.getDecryptPath()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(request.toFormData())
                    .retrieve()
                    .body(FileCryptoDecryptRes.class);

            if (response == null || response.status() == null || response.status() != DECRYPT_SUCCESS
                    || StringUtils.hasText(response.errorMsg()) || !StringUtils.hasText(response.outputFilePath())) {
                throw cryptoFailure("decrypt", response == null ? null : response.errorMsg());
            }
            log.info("File crypto API decrypt response accepted. fid={}, status={}, outputPathPresent={}, dataSize={}",
                    fidForLog(response.fid()), response.status(), StringUtils.hasText(response.outputFilePath()),
                    response.dataSize());
            return response;
        });
    }

    byte[] download(String outputFilePath) {
        return executeWithRetry("download", () -> {
            byte[] response = client().get()
                    .uri(cryptoUri(outputFilePath))
                    .retrieve()
                    .body(byte[].class);

            if (response == null) {
                throw cryptoFailure("download", null);
            }
            log.info("File crypto API download response accepted. outputBytes={}", response.length);
            return response;
        });
    }

    private <T> T executeWithRetry(String operation, CryptoCall<T> call) {
        int maxAttempts = Math.max(1, properties.getRetryCount() + 1);
        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            var budget=readBudget.get();if(budget!=null)budget.checkpoint.run();
            long startedAt = System.nanoTime();
            log.info("File crypto API {} request started. attempt={}/{}", operation, attempt, maxAttempts);
            try {
                T result = call.execute();
                log.info("File crypto API {} request succeeded. attempt={}/{}, elapsedMs={}",
                        operation, attempt, maxAttempts, elapsedMs(startedAt));
                return result;
            } catch (RuntimeException e) {
                if(budget!=null)budget.finished();
                lastException = e;
                if (attempt >= maxAttempts || e instanceof AppErrorException) {
                    log.warn("File crypto API {} request failed. attempt={}/{}, elapsedMs={}, retryable=false, exception={}, detail={}",
                            operation, attempt, maxAttempts, elapsedMs(startedAt), exceptionTypeForLog(e), errorDetailForLog(e));
                    throw e;
                }
                log.warn("File crypto API {} request failed. attempt={}/{}, elapsedMs={}, retryable=true, exception={}, detail={}",
                        operation, attempt, maxAttempts, elapsedMs(startedAt), exceptionTypeForLog(e), errorDetailForLog(e));
                sleepBeforeRetry();
            }finally{if(budget!=null)budget.finished();}
        }

        throw lastException == null ? cryptoFailure(operation, null) : lastException;
    }

    private void sleepBeforeRetry() {
        try {
            var budget=readBudget.get();if(budget!=null)budget.checkpoint.run();
            long delay=properties.getRetryBackoffMs();if(budget!=null)delay=Math.min(delay,budget.remaining.getAsInt());Thread.sleep(Math.max(0,delay));
            if(budget!=null)budget.checkpoint.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "파일 암복호화 API 재시도 대기 중 오류가 발생했습니다.");
        }
    }

    private AppErrorException cryptoFailure(String operation, String errorMessage) {
        String message = StringUtils.hasText(errorMessage)
                ? "파일 암복호화 API " + operation + " 실패: " + errorMessage
                : "파일 암복호화 API " + operation + " 실패";
        return AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, message);
    }

    private String exceptionTypeForLog(RuntimeException e) {
        return e.getClass().getSimpleName();
    }

    private String errorDetailForLog(RuntimeException e) {
        String detail = null;
        if (e instanceof AppErrorException appErrorException) {
            detail = StringUtils.hasText(appErrorException.getAdditionalMessage())
                    ? appErrorException.getAdditionalMessage()
                    : appErrorException.getMessage();
        } else if (e instanceof ApiErrorException apiErrorException) {
            detail = "httpStatus=%d, message=%s".formatted(
                    apiErrorException.getStatusCode(),
                    apiErrorException.getMessage());
        } else {
            detail = e.getMessage();
        }

        if (!StringUtils.hasText(detail) && e.getCause() != null) {
            detail = e.getCause().toString();
        }
        return sanitizeForLog(detail);
    }

    private String sanitizeForLog(String value) {
        if (!StringUtils.hasText(value)) {
            return "N/A";
        }
        String sanitized = value.replaceAll("[\\r\\n\\t]+", " ").trim();
        if (sanitized.length() <= LOG_DETAIL_MAX_LENGTH) {
            return sanitized;
        }
        return sanitized.substring(0, LOG_DETAIL_MAX_LENGTH) + "...";
    }

    private String normalizePath(String path) {
        if (!StringUtils.hasText(path)) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "파일 암복호화 API 경로 설정이 없습니다.");
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private String cryptoUri(String path) {
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "파일 암복호화 API base URL 설정이 없습니다.");
        }

        String baseUrl = properties.getBaseUrl().endsWith("/")
                ? properties.getBaseUrl().substring(0, properties.getBaseUrl().length() - 1)
                : properties.getBaseUrl();
        return baseUrl + normalizePath(path);
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private String fidForLog(UUID fid) {
        if (fid == null) {
            return "null";
        }
        String value = fid.toString();
        return value.length() <= 8 ? value : value.substring(0, 8);
    }

    private String extensionForLog(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "unknown";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "none";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private interface CryptoCall<T> {
        T execute();
    }

    private static class NamedByteArrayResource extends ByteArrayResource {

        private final String filename;

        NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
