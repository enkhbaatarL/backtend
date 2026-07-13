package com.sk.domain.mi.service.nextgen.paramproposal.excel;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

/**
 * 병합 컴포넌트의 산출물 — 확정값이 반영된 수급모델 입력 파일 3개.
 *
 * 병합 대상이 없던 파일도 원본 그대로 포함해 항상 3개 완비 상태다.
 * toMultipartFiles()로 기존 실행 접수 파이프라인(파일 3개 업로드)에 바로 태울 수 있다.
 */
public record MergedModelInputFiles(

    Map<ModelInputFile, byte[]> contents

) {

    private static final String XLSM_CONTENT_TYPE = "application/vnd.ms-excel.sheet.macroEnabled.12";
    private static final String MULTIPART_FIELD_NAME = "files";

    public static MergedModelInputFiles of(Map<ModelInputFile, byte[]> contents) {
        return new MergedModelInputFiles(Map.copyOf(contents));
    }

    public byte[] content(ModelInputFile file) {
        return contents.get(file);
    }

    /**
     * 기존 수급모델 실행 접수 계약(DEMAND/SUPPLY/TRADE 파일 3개)에 맞는 MultipartFile 목록으로 변환한다.
     */
    public List<MultipartFile> toMultipartFiles() {
        return Arrays.stream(ModelInputFile.values())
            .map(file -> (MultipartFile) new ByteArrayMultipartFile(file.getBasecaseFileName(), contents.get(file)))
            .toList();
    }

    /**
     * 인메모리 바이트 기반 MultipartFile.
     * 기존 MultipartFileUtil은 File 기반이라 디스크 임시파일 없이 전달하기 위해 별도로 둔다.
     */
    private record ByteArrayMultipartFile(String filename, byte[] bytes) implements MultipartFile {

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
            return bytes == null || bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes == null ? 0 : bytes.length;
        }

        @Override
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            Files.write(dest.toPath(), bytes);
        }

        @Override
        public void transferTo(java.nio.file.Path dest) throws IOException, IllegalStateException {
            Files.write(dest, bytes);
        }

    }

}
