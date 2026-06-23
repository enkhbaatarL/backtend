package com.sk.domain.shared.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "file.encryption")
public class FileEncryptionProperties {

    /** 파일 암복호화 전역 사용 여부. 기본 false. */
    private boolean enabled = false;

    private List<String> enabledCompanyCds = List.of();

    private List<String> targetExtensions = List.of(
            "xlsx",
            "xls",
            "xlsm",
            "docx",
            "doc",
            "docm",
            "pptx",
            "ppt",
            "pptm");
}
