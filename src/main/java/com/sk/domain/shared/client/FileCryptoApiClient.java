package com.sk.domain.shared.client;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.shared.client.dto.request.FileCryptoDecryptReq;
import com.sk.domain.shared.client.dto.request.FileCryptoEncryptReq;
import com.sk.domain.shared.client.dto.response.FileCryptoDecryptRes;
import com.sk.domain.shared.client.dto.response.FileCryptoEncryptRes;
import com.sk.domain.shared.client.dto.response.FileCryptoUploadRes;
import com.sk.domain.shared.config.FileCryptoApiProperties;
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

    private static final int UPLOAD_SUCCESS = 1;
    private static final int ENCRYPT_SUCCESS = 0;
    private static final int DECRYPT_SUCCESS = 4;

    @Qualifier("fileCryptoRestClient")
    private final RestClient restClient;
    private final FileCryptoApiProperties properties;

    public byte[] encrypt(byte[] plainBytes, String originalFileName) {
        FileCryptoUploadRes uploaded = upload(plainBytes, originalFileName);
        FileCryptoEncryptRes encrypted = encrypt(new FileCryptoEncryptReq(
                properties.getUserId(),
                properties.getLabelId(),
                uploaded.fid(),
                properties.getPid()));
        return downloadAndVerify("encrypt", encrypted.outputFilePath(), encrypted.dataSize());
    }

    public byte[] decrypt(byte[] encryptedBytes, String originalFileName) {
        FileCryptoUploadRes uploaded = upload(encryptedBytes, originalFileName);
        FileCryptoDecryptRes decrypted = decrypt(new FileCryptoDecryptReq(
                properties.getUserId(),
                uploaded.fid(),
                properties.getPid()));
        return downloadAndVerify("decrypt", decrypted.outputFilePath(), decrypted.dataSize());
    }

    // 암복호화 서비스가 보고한 산출물 크기(dataSize)와 실제 다운로드 바이트 수를 교차검증해 잘림/손상을 탐지한다.
    // dataSize 의미가 불명확하거나 0/null인 경우는 검증을 건너뛴다.
    private byte[] downloadAndVerify(String operation, String outputFilePath, Long expectedSize) {
        byte[] data = download(outputFilePath);
        if (expectedSize != null && expectedSize > 0 && data.length != expectedSize) {
            throw cryptoFailure(operation,
                    "산출물 크기 불일치 (expected=%d, actual=%d)".formatted(expectedSize, data.length));
        }
        return data;
    }

    FileCryptoUploadRes upload(byte[] fileBytes, String originalFileName) {
        return executeWithRetry("upload", () -> {
            MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
            multipart.add("file", new NamedByteArrayResource(fileBytes, originalFileName));

            FileCryptoUploadRes response = restClient.post()
                    .uri(cryptoUri(normalizePath(properties.getUploadPath()) + "/" + properties.getPid()))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(multipart)
                    .retrieve()
                    .body(FileCryptoUploadRes.class);

            if (response == null || response.result() != UPLOAD_SUCCESS || response.fid() == null
                    || StringUtils.hasText(response.errorMsg())) {
                throw cryptoFailure("upload", response == null ? null : response.errorMsg());
            }
            return response;
        });
    }

    FileCryptoEncryptRes encrypt(FileCryptoEncryptReq request) {
        return executeWithRetry("encrypt", () -> {
            FileCryptoEncryptRes response = restClient.post()
                    .uri(cryptoUri(properties.getEncryptPath()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(request.toFormData())
                    .retrieve()
                    .body(FileCryptoEncryptRes.class);

            if (response == null || response.status() == null || response.status() != ENCRYPT_SUCCESS
                    || StringUtils.hasText(response.errorMsg()) || !StringUtils.hasText(response.outputFilePath())) {
                throw cryptoFailure("encrypt", response == null ? null : response.errorMsg());
            }
            return response;
        });
    }

    FileCryptoDecryptRes decrypt(FileCryptoDecryptReq request) {
        return executeWithRetry("decrypt", () -> {
            FileCryptoDecryptRes response = restClient.post()
                    .uri(cryptoUri(properties.getDecryptPath()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(request.toFormData())
                    .retrieve()
                    .body(FileCryptoDecryptRes.class);

            if (response == null || response.status() == null || response.status() != DECRYPT_SUCCESS
                    || StringUtils.hasText(response.errorMsg()) || !StringUtils.hasText(response.outputFilePath())) {
                throw cryptoFailure("decrypt", response == null ? null : response.errorMsg());
            }
            return response;
        });
    }

    byte[] download(String outputFilePath) {
        return executeWithRetry("download", () -> {
            byte[] response = restClient.get()
                    .uri(cryptoUri(outputFilePath))
                    .retrieve()
                    .body(byte[].class);

            if (response == null) {
                throw cryptoFailure("download", null);
            }
            return response;
        });
    }

    private <T> T executeWithRetry(String operation, CryptoCall<T> call) {
        int maxAttempts = Math.max(1, properties.getRetryCount() + 1);
        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return call.execute();
            } catch (RuntimeException e) {
                lastException = e;
                if (attempt >= maxAttempts || e instanceof AppErrorException) {
                    throw e;
                }
                log.warn("File crypto API {} failed. retry={}/{}", operation, attempt, properties.getRetryCount());
                sleepBeforeRetry();
            }
        }

        throw lastException == null ? cryptoFailure(operation, null) : lastException;
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(properties.getRetryBackoffMs());
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
