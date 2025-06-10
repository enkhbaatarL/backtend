package com.sk.common.utils;

import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class MultipartFileUtil implements MultipartFile {

    private final File file;
    private final String name;
    private final String originalFilename;
    private final String contentType;

    /**
     * @param name             폼 필드 이름
     * @param file             실제 파일
     * @param contentType      MIME 타입 (null 이면 probe로 자동 감지)
     */
    public MultipartFileUtil(String name, File file, String contentType) {
        this.file = file;
        this.name = name;
        this.originalFilename = file.getName();
        this.contentType = (contentType != null ? contentType : probeContentType(file));
    }

    private String probeContentType(File file) {
        try {
            String type = Files.probeContentType(file.toPath());
            return (type != null ? type : "application/octet-stream");
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return file == null || file.length() == 0;
    }

    @Override
    public long getSize() {
        return (file != null ? file.length() : 0);
    }

    @Override
    public byte[] getBytes() throws IOException {
        return Files.readAllBytes(file.toPath());
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(file);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
