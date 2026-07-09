package com.sk.domain.mi.service.nextgen.paramproposal.excel;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import com.sk.domain.shared.service.ObjectStorageService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * Basecase 원본 워크북 로더.
 * 우선순위 오브젝트 스토리지 - 고객사 전용 버킷
 * 스토리지 조회가 실패하면 classpath 리소스로 폴백한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class BasecaseWorkbookProvider {

    private final ResourceLoader resourceLoader;
    private final ObjectStorageService objectStorageService;

    /**
     * 기본값은 테넌트 버킷 파생 규칙(ncp.storage.prefix + 고객사코드) — dev/tech/prod에서 환경별 버킷이 자동 결정된다.
     * local/testdb는 prefix(skt-)가 실버킷(sk-)과 달라 스토리지 조회가 실패하고 classpath 폴백으로 동작한다.
     */
    @Value("${mi.next-gen.model-input.basecase-bucket:${ncp.storage.prefix:}skgas}")
    private String basecaseBucket;

    @Value("${mi.next-gen.model-input.basecase-path:fundamental_model_artifact/input_template}")
    private String basecasePath;

    @Value("${mi.next-gen.model-input.basecase-fallback-location:classpath:basecase/}")
    private String fallbackLocation;

    XSSFWorkbook open(ModelInputFile file) {
        try {
            return openFromStorage(file);
        } catch (Exception e) {
            log.error("[ModelInput Merge] Object-Storage basecase 로드 실패 -> classpath 폴백 사용 (스토리지 원본과 다를 수 있음): bucket={}, path={}, role={}, cause={}", basecaseBucket, basecasePath, file.getCode(), e.getMessage());
            return openFromFallback(file);
        }
    }

    private XSSFWorkbook openFromStorage(ModelInputFile file) throws IOException {
        String objectKey = resolveObjectKey(file);
        byte[] bytes = objectStorageService.downloadFile(basecaseBucket, objectKey);
        log.info("[ModelInput Merge] basecase 로드(storage): bucket={}, key={}, bytes={}", basecaseBucket, objectKey, bytes.length);
        return new XSSFWorkbook(new ByteArrayInputStream(bytes));
    }

    /**
     * input_template 폴더에서 역할 키워드로 object key를 찾는다.
     * 파일명(마지막 / 이후)이 .xlsm이고 역할명(DEMAND 등)을 포함해야 하며, 정확히 1개가 아니면 실패한다.
     */
    private String resolveObjectKey(ModelInputFile file) {
        List<String> keys = objectStorageService.listFolder(basecaseBucket, basecasePath, false)
            .getOrDefault("files", List.of());

        List<String> matched = keys.stream()
            .filter(key -> {
                String fileName = key.substring(key.lastIndexOf('/') + 1).toUpperCase(Locale.ROOT);
                return fileName.endsWith(".XLSM") && fileName.contains(file.name());
            })
            .toList();

        if (matched.size() != 1) {
            throw new IllegalStateException("역할 %s의 basecase 파일이 %d개 매칭됨 (path=%s, 후보=%s)"
                .formatted(file.name(), matched.size(), basecasePath, matched));
        }
        return matched.get(0);
    }

    private XSSFWorkbook openFromFallback(ModelInputFile file) {
        Resource resource = resourceLoader.getResource(fallbackLocationOf(file));
        try (InputStream in = resource.getInputStream()) {
            return new XSSFWorkbook(in);
        } catch (IOException e) {
            log.error("[ModelInput Merge] classpath 폴백 로드도 실패: {}", fallbackLocationOf(file), e);
            throw SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "수급모델 입력 병합 실패 — Basecase 원본을 열 수 없습니다 (스토리지·폴백 모두 실패): %s".formatted(file.getBasecaseFileName()));
        }
    }

    private String fallbackLocationOf(ModelInputFile file) {
        String location = fallbackLocation.endsWith("/") ? fallbackLocation : fallbackLocation + "/";
        return location + file.getBasecaseFileName();
    }

}
