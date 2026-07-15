package com.sk.domain.shared.service;

import com.sk.domain.shared.config.FileEncryptionProperties;
import com.sk.domain.shared.enums.DownloadEncryptionDecision;
import com.sk.domain.shared.enums.Role;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileEncryptionPolicy {

    private final FileEncryptionProperties properties;

    /** 전역 사용 여부 */
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public boolean isEnabled(String companyCd, String fileName) {
        return isEnabled() && isEnabledCompany(companyCd) && isTargetExtension(fileName);
    }

    public DownloadEncryptionDecision evaluateDownload(Role role, String companyCd, String fileName) {
        if (!isEnabled()) {
            return DownloadEncryptionDecision.FEATURE_DISABLED;
        }
        if (!isTargetExtension(fileName)) {
            return DownloadEncryptionDecision.EXTENSION_NOT_TARGET;
        }
        if (role != Role.USER) {
            return DownloadEncryptionDecision.ROLE_NOT_TARGET;
        }
        if (!isEnabledCompany(companyCd)) {
            return DownloadEncryptionDecision.COMPANY_NOT_TARGET;
        }
        return DownloadEncryptionDecision.ENCRYPT;
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
