package com.sk.domain.mi.service.nextgen.paramproposal.excel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

/**
 * 병합 컴포넌트의 산출물 — 확정값이 반영된 수급모델 입력 파일 4개.
 *
 * 병합 대상이 없던 파일과 OUTPUT도 원본 그대로 포함해 항상 4개 완비 상태다.
 * 산출물은 힙이 아니라 임시 파일로 보관한다(메모리 절감) — 사용이 끝나면 호출 측이 cleanup()으로 삭제해야 한다.
 * toMultipartFiles()로 기존 실행 접수 파이프라인(파일 4개 업로드)에 바로 태울 수 있다.
 */
@Slf4j
public final class MergedModelInputFiles {

    private static final String XLSM_CONTENT_TYPE = "application/vnd.ms-excel.sheet.macroEnabled.12";
    private static final String MULTIPART_FIELD_NAME = "files";

    private final Map<ModelInputFile, Path> files;

    private MergedModelInputFiles(Map<ModelInputFile, Path> files) {
        this.files = Map.copyOf(files);
    }

    public static MergedModelInputFiles of(Map<ModelInputFile, Path> files) {
        return new MergedModelInputFiles(files);
    }

    /** 산출물 바이트 — 테스트/검증용. 운영 경로는 toMultipartFiles()의 스트림을 쓴다. */
    public byte[] content(ModelInputFile file) {
        try {
            return Files.readAllBytes(files.get(file));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 기존 수급모델 실행 접수 계약(DEMAND/SUPPLY/TRADE/OUTPUT 파일 4개)에 맞는 MultipartFile 목록으로 변환한다.
     * 파일 기반이라 업로드 시 스트림으로 읽힌다 — cleanup() 전까지만 유효하다.
     */
    public List<MultipartFile> toMultipartFiles() {
        return Arrays.stream(ModelInputFile.values())
            .map(file -> (MultipartFile) new FileBackedMultipartFile(file.getBasecaseFileName(), files.get(file)))
            .toList();
    }

    /** 임시 산출물 파일을 삭제한다. 업로드(접수)가 끝나면 성공/실패와 무관하게 반드시 호출한다. */
    public void cleanup() {
        files.values().forEach(path -> {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.warn("[ModelInput Merge] 병합 산출물 임시 파일 삭제 실패: {}", path, e);
            }
        });
    }

    /** 임시 파일 기반 MultipartFile — 바이트를 힙에 상주시키지 않는다. */
    private record FileBackedMultipartFile(String filename, Path path) implements MultipartFile {

        @Override
        public String getName() {
            return MULTIPART_FIELD_NAME;
        }

        @Override
        public String getOriginalFilename() {
            return filename;
        }

        @Override
        public String getContentType() {
            return XLSM_CONTENT_TYPE;
        }

        @Override
        public boolean isEmpty() {
            return getSize() == 0;
        }

        @Override
        public long getSize() {
            try {
                return Files.size(path);
            } catch (IOException e) {
                return 0;
            }
        }

        @Override
        public byte[] getBytes() throws IOException {
            return Files.readAllBytes(path);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return Files.newInputStream(path);
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            Files.copy(path, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        @Override
        public void transferTo(Path dest) throws IOException, IllegalStateException {
            Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
        }

    }

}
