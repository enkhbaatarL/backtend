package com.sk.domain.shared.service;

import com.sk.domain.shared.config.FileEncryptionProperties;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileEncryptionPolicy {

    private final FileEncryptionProperties properties;

    public boolean isEnabled(String companyCd, String fileName) {
        return isEnabledCompany(companyCd) && isTargetExtension(fileName);
    }

    public boolean isEnabledCompany(String companyCd) {
        if (companyCd == null) {
            return false;
        }

        return properties.getEnabledCompanyCds().stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.toUpperCase(Locale.ROOT))
                .anyMatch(companyCd.toUpperCase(Locale.ROOT)::equals);
    }

    public boolean isTargetExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return false;
        }

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return false;
        }

        String extension = fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        return properties.getTargetExtensions().stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(extension::equals);
    }
}
