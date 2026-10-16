package com.sk.domain.mi.service.nextgen.paramproposal.excel;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import com.sk.domain.mi.service.nextgen.paramproposal.MiBaseInputSetResolver;
import com.sk.domain.mi.service.nextgen.paramproposal.MiBaseInputSetResolver.InvalidMiBaseInputSetException;
import com.sk.domain.mi.service.nextgen.paramproposal.MiBaseInputSetResolver.MiBaseInputSet;
import com.sk.domain.shared.service.ObjectStorageService;
import com.sk.domain.shared.service.StorageLocation;
import com.sk.domain.shared.service.StorageLocationParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * Basecase 원본 로더.
 * 병합 시작 시 기준본 4파일을 source snapshot으로 한 번 고정한다.
 * - 제안 생성 때 고정한 MI Base case가 있으면 그 Case의 SCNRCASE input 4개.
 * - 없으면(이 변경 이전 제안) current MI Base의 SCNRCASE input 4개. MI Base 파일을 읽지 못하면 실패하고 기본 template으로 넘어가지 않는다.
 * - current MI Base도 없으면 플랫폼 기본 template 4개 한 세트 — 오브젝트 스토리지(고객사 전용 버킷)에 4역할이 모두 있으면 스토리지, 아니면 classpath.
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
    private final StorageLocationParser storageLocationParser;
    private final MiBaseInputSetResolver miBaseInputSetResolver;

    /**
     * 기본값은 테넌트 버킷 파생 규칙(ncp.storage.prefix + 고객사코드) — dev/tech/prod에서 환경별 버킷이 자동 결정된다.
     * local/testdb는 prefix(skt-)가 실버킷(sk-)과 달라 스토리지 조회가 실패하고 classpath 세트로 동작한다.
     */
    @Value("${mi.next-gen.model-input.basecase-bucket:${ncp.storage.prefix:}skgas}")
    private String basecaseBucket;

    @Value("${mi.next-gen.model-input.basecase-path:fundamental_model_artifact/input_template}")
    private String basecasePath;

    @Value("${mi.next-gen.model-input.basecase-fallback-location:classpath:basecase/}")
    private String fallbackLocation;

    /** 병합 1회분의 기준본 4파일을 고정한다. 이후 materialize는 DB·스토리지 목록을 다시 보지 않는다. */
    BasecaseSourceSnapshot selectSourceSnapshot() {
        BasecaseSourceSnapshot snapshot;
        try {
            snapshot = miBaseInputSetResolver.findCurrent()
                .map(this::miBaseSnapshot)
                .orElseGet(this::platformDefaultSnapshot);
        } catch (InvalidMiBaseInputSetException e) {
            throw invalidMiBaseSource("current MI Base", e.getCaseId(), e.getReason());
        }
        log.info("[ModelInput Merge] 기준본 snapshot: {}", snapshot.describe());
        return snapshot;
    }

    /** 제안 생성 때 고정한 MI Base case의 기준본 4파일. 지금 current MI Base인지와 무관하며, 읽지 못하면 다른 기준으로 넘어가지 않는다. */
    BasecaseSourceSnapshot selectSourceSnapshot(Long miBaseCaseId) {
        BasecaseSourceSnapshot snapshot;
        try {
            snapshot = miBaseSnapshot(miBaseInputSetResolver.resolve(miBaseCaseId));
        } catch (InvalidMiBaseInputSetException e) {
            throw invalidMiBaseSource("제안 생성 시 고정한 MI Base", e.getCaseId(), e.getReason());
        }
        log.info("[ModelInput Merge] 기준본 snapshot(제안 고정): {}", snapshot.describe());
        return snapshot;
    }

    /** snapshot의 basecase 원본 1개를 임시 파일로 내려받아 경로를 반환한다. */
    Path materialize(BasecaseSourceSnapshot snapshot, ModelInputFile file) {
        return switch (snapshot.source()) {
            case MI_BASE -> materializeFromMiBase(snapshot, file);
            case PLATFORM_STORAGE -> materializeFromPlatformStorage(snapshot, file);
            case PLATFORM_CLASSPATH -> materializeFromFallback(file);
        };
    }

    private BasecaseSourceSnapshot miBaseSnapshot(MiBaseInputSet inputSet) {
        Map<ModelInputFile, SourceFile> files = new EnumMap<>(ModelInputFile.class);
        inputSet.files().forEach((file, inputFile) -> files.put(file, new SourceFile(inputFile.fileNo(), inputFile.storagePath())));
        return new BasecaseSourceSnapshot(BasecaseSource.MI_BASE, inputSet.caseId(), files);
    }

    /**
     * MI Base가 없을 때의 플랫폼 기본 template 세트.
     * input_template 목록을 한 번 조회해 4역할이 모두 정확히 1개씩이면 스토리지 세트, 아니면 classpath 세트로 고정한다.
     */
    private BasecaseSourceSnapshot platformDefaultSnapshot() {
        try {
            return new BasecaseSourceSnapshot(BasecaseSource.PLATFORM_STORAGE, null, resolveObjectKeys());
        } catch (Exception e) {
            log.error("[ModelInput Merge] - FALLBACK Object-Storage basecase 세트 구성 실패 -> classpath 세트 사용 (스토리지 원본과 다를 수 있음): bucket={}, path={}, cause={}", basecaseBucket, basecasePath, e.getMessage());
            return new BasecaseSourceSnapshot(BasecaseSource.PLATFORM_CLASSPATH, null, Map.of());
        }
    }

    /**
     * input_template 폴더에서 역할 키워드로 object key를 찾는다.
     * 파일명(마지막 / 이후)이 .xlsm이고 역할명(DEMAND 등)을 포함해야 하며, 역할마다 정확히 1개가 아니면 실패한다.
     */
    private Map<ModelInputFile, SourceFile> resolveObjectKeys() {
        List<String> keys = objectStorageService.listFolder(basecaseBucket, basecasePath, false)
            .getOrDefault("files", List.of());

        Map<ModelInputFile, SourceFile> files = new EnumMap<>(ModelInputFile.class);
        for (ModelInputFile file : ModelInputFile.values()) {
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
            files.put(file, new SourceFile(null, matched.get(0)));
        }
        return files;
    }

    private Path materializeFromMiBase(BasecaseSourceSnapshot snapshot, ModelInputFile file) {
        SourceFile sourceFile = snapshot.files().get(file);
        try {
            StorageLocation location = storageLocationParser.parse(sourceFile.location());
            Path temp = download(location.bucket(), location.key(), file);
            log.info("[ModelInput Merge] basecase 로드(MI Base): caseId={}, role={}, fileNo={}, temp={}", snapshot.miBaseCaseId(), file.name(), sourceFile.fileNo(), temp);
            return temp;
        } catch (Exception e) {
            log.error("[ModelInput Merge] MI Base 파일 로드 실패: caseId={}, role={}, fileNo={}", snapshot.miBaseCaseId(), file.name(), sourceFile.fileNo(), e);
            throw invalidMiBaseSource("MI Base", snapshot.miBaseCaseId(), "input file cannot be downloaded: " + file.name());
        }
    }

    private Path materializeFromPlatformStorage(BasecaseSourceSnapshot snapshot, ModelInputFile file) {
        String objectKey = snapshot.files().get(file).location();
        try {
            Path temp = download(basecaseBucket, objectKey, file);
            log.info("[ModelInput Merge] basecase 로드(storage): bucket={}, key={}, temp={}", basecaseBucket, objectKey, temp);
            return temp;
        } catch (Exception e) {
            log.error("[ModelInput Merge] Object-Storage basecase 로드 실패 (세트 고정 후라 classpath 폴백 없음): bucket={}, key={}", basecaseBucket, objectKey, e);
            throw SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "수급모델 입력 병합 실패 — Basecase 원본을 내려받을 수 없습니다 (storage): %s".formatted(file.name()));
        }
    }

    private Path download(String bucket, String objectKey, ModelInputFile file) throws IOException {
        byte[] bytes = objectStorageService.downloadFile(bucket, objectKey);
        Path temp = createTempFile(file);
        try {
            Files.write(temp, bytes);
        } catch (IOException e) {
            deleteQuietly(temp);
            throw e;
        }
        return temp;
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
            log.error("[ModelInput Merge] classpath basecase 로드 실패: {}", fallbackLocationOf(file), e);
            throw SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "수급모델 입력 병합 실패 — Basecase 원본을 열 수 없습니다 (classpath): %s".formatted(file.getBasecaseFileName()));
        }
    }

    private SystemErrorException invalidMiBaseSource(String sourceLabel, Long caseId, String reason) {
        return SystemErrorException.of(
            ResultCode.Error.SYS_INTERNAL_ERROR,
            "수급모델 입력 병합 실패 — %s 기준본이 유효하지 않습니다. caseId: %d, reason: %s".formatted(sourceLabel, caseId, reason)
        );
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

    enum BasecaseSource {
        MI_BASE,
        PLATFORM_STORAGE,
        PLATFORM_CLASSPATH
    }

    /** location: MI Base는 tcm_filel 저장 경로(URL), 플랫폼 스토리지는 object key. classpath 세트는 비어 있다. */
    record SourceFile(Long fileNo, String location) {
    }

    record BasecaseSourceSnapshot(BasecaseSource source, Long miBaseCaseId, Map<ModelInputFile, SourceFile> files) {

        BasecaseSourceSnapshot {
            files = Map.copyOf(files);
        }

        /** 병합 오류 메시지에 쓸 원본 표시명 — MI Base면 caseId와 역할로 플랫폼 template과 구분한다. */
        String labelOf(ModelInputFile file) {
            if (source == BasecaseSource.MI_BASE) {
                return "MI Base caseId=%d %s".formatted(miBaseCaseId, file.name());
            }
            return file.getBasecaseFileName();
        }

        String describe() {
            return switch (source) {
                case MI_BASE -> "MI_BASE caseId=%d, fileNos=%s".formatted(miBaseCaseId, ordered(SourceFile::fileNo));
                case PLATFORM_STORAGE -> "PLATFORM_STORAGE keys=%s".formatted(ordered(SourceFile::location));
                case PLATFORM_CLASSPATH -> "PLATFORM_CLASSPATH";
            };
        }

        private <T> Map<ModelInputFile, T> ordered(Function<SourceFile, T> attribute) {
            Map<ModelInputFile, T> ordered = new EnumMap<>(ModelInputFile.class);
            files.forEach((file, sourceFile) -> ordered.put(file, attribute.apply(sourceFile)));
            return ordered;
        }
    }

}
