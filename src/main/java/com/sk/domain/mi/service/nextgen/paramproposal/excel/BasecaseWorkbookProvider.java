package com.sk.domain.mi.service.nextgen.paramproposal.excel;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import com.sk.domain.shared.service.ObjectStorageService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * Basecase 원본 로더.
 * 우선순위 오브젝트 스토리지 - 고객사 전용 버킷
 * 스토리지 조회가 실패하면 classpath 리소스로 폴백한다.
 *
 * 원본은 워크북 객체가 아니라 임시 파일 경로로 내려준다 — POI가 File 기반(zip 랜덤 액세스)으로 열 수 있게 해
 * 스트림 오픈 시 zip 전체를 힙에 버퍼링하는 비용을 없애기 위함(OOM 대응). 임시 파일 삭제는 호출자 책임이다.
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

    /** basecase 원본 1개를 임시 파일로 내려받아 경로를 반환한다. */
    Path materialize(ModelInputFile file) {
        try {
            return materializeFromStorage(file);
        } catch (Exception e) {
            log.error("[ModelInput Merge] - FALLBACK [{}] Object-Storage basecase 로드 실패 -> classpath 폴백 사용 (스토리지 원본과 다를 수 있음): bucket={}, path={}, cause={}", file.getCode(), basecaseBucket, basecasePath, e.getMessage());
            return materializeFromFallback(file);
        }
    }

    private Path materializeFromStorage(ModelInputFile file) throws IOException {
        String objectKey = resolveObjectKey(file);
        byte[] bytes = objectStorageService.downloadFile(basecaseBucket, objectKey);
        Path temp = createTempFile(file);
        try {
            Files.write(temp, bytes);
        } catch (IOException e) {
            deleteQuietly(temp);
            throw e;
        }
        log.info("[ModelInput Merge] basecase 로드(storage): bucket={}, key={}, bytes={}, temp={}", basecaseBucket, objectKey, bytes.length, temp);
        return temp;
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

    private Path materializeFromFallback(ModelInputFile file) {
        Resource resource = resourceLoader.getResource(fallbackLocationOf(file));
        Path temp = null;
        try (InputStream in = resource.getInputStream()) {
            temp = createTempFile(file);
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            return temp;
        } catch (IOException e) {
            deleteQuietly(temp);
            log.error("[ModelInput Merge] classpath 폴백 로드도 실패: {}", fallbackLocationOf(file), e);
            throw SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "수급모델 입력 병합 실패 — Basecase 원본을 열 수 없습니다 (스토리지·폴백 모두 실패): %s".formatted(file.getBasecaseFileName()));
        }
    }

    private Path createTempFile(ModelInputFile file) throws IOException {
        return Files.createTempFile("basecase-%s-".formatted(file.name().toLowerCase(Locale.ROOT)), ".xlsm");
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("[ModelInput Merge] 임시 파일 삭제 실패: {}", path, e);
        }
    }

    private String fallbackLocationOf(ModelInputFile file) {
        String location = fallbackLocation.endsWith("/") ? fallbackLocation : fallbackLocation + "/";
        return location + file.getBasecaseFileName();
    }

}
