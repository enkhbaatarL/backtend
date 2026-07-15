package com.sk.domain.shared.service;

import com.sk.domain.shared.client.FileCryptoApiClient;
import com.sk.domain.shared.enums.DownloadEncryptionDecision;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 외부 클라이언트로 파일을 전달하는 경계(다운로드 컨트롤러)에서만 호출하는 재암호화 훅이다.
 *
 * <p>Object Storage에는 평문이 저장되므로, 특정 고객사(SKGAS) 대상 확장자 파일은 외부로 나가기 직전 다시 암호화한다.
 * 회사코드는 인증된 요청 컨텍스트에서만 얻을 수 있고, 비동기/시스템 등 보안 컨텍스트가 없는 내부 소비자는
 * 이 경계를 거치지 않고 {@code FileService}에서 평문을 그대로 사용한다.
 *
 * <p>케이스 처리:
 * <ul>
 *   <li>feature flag가 꺼져 있거나 대상 확장자가 아니면 인증 컨텍스트 조회 없이 평문을 반환한다.</li>
 *   <li>{@link Role#USER}가 아니면 회사 식별 없이 평문을 반환한다.</li>
 *   <li>USER의 회사코드 식별이 불가능하면 예외가 그대로 전파되어 fail-closed 된다.</li>
 *   <li>최종 암호화 여부와 skip reason은 {@link FileEncryptionPolicy#evaluateDownload(Role, String, String)}가 결정한다.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileDownloadEncryptor {

    private final FileEncryptionPolicy fileEncryptionPolicy;
    private final FileCryptoApiClient fileCryptoApiClient;
    private final AuthorizationService authorizationService;

    public byte[] encryptForDownloadIfRequired(String fileName, byte[] plainData) {
        long startedAt = System.nanoTime();

        if (!fileEncryptionPolicy.isEnabled()) {
            DownloadEncryptionDecision decision = fileEncryptionPolicy.evaluateDownload(null, null, fileName);
            return skipEncryption(fileName, plainData, decision, startedAt);
        }

        if (!fileEncryptionPolicy.isTargetExtension(fileName)) {
            DownloadEncryptionDecision decision = fileEncryptionPolicy.evaluateDownload(null, null, fileName);
            return skipEncryption(fileName, plainData, decision, startedAt);
        }

        Role role = authorizationService.getCurrentRole();
        String companyCd = Role.USER == role
                ? authorizationService.getCompanyCdWithPriority()
                : null;
        DownloadEncryptionDecision decision = fileEncryptionPolicy.evaluateDownload(role, companyCd, fileName);
        if (!decision.shouldEncrypt()) {
            return skipEncryption(fileName, plainData, decision, startedAt);
        }

        long cryptoStartedAt = System.nanoTime();
        byte[] encryptedData = fileCryptoApiClient.encrypt(plainData, fileName);
        log.info(
                "File download encryption completed. extension={}, inputBytes={}, outputBytes={}, cryptoMs={}, totalMs={}",
                extensionForLog(fileName),
                plainData.length,
                encryptedData.length,
                elapsedMs(cryptoStartedAt),
                elapsedMs(startedAt));
        return encryptedData;
    }

    private byte[] skipEncryption(
            String fileName,
            byte[] plainData,
            DownloadEncryptionDecision decision,
            long startedAt) {
        log.info(
                "File download encryption skipped. extension={}, reason={}, elapsedMs={}",
                extensionForLog(fileName),
                decision.skipReason(),
                elapsedMs(startedAt));
        return plainData;
    }

    private String extensionForLog(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "unknown";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "none";
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
