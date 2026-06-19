package com.sk.domain.shared.service;

import com.sk.domain.shared.client.FileCryptoApiClient;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import lombok.RequiredArgsConstructor;
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
 *   <li>대상 확장자가 아니면 회사 식별 없이 평문 그대로 반환한다. (어떤 회사 기준으로도 암호화 대상이 아님)</li>
 *   <li>대상 확장자면 회사코드를 반드시 확인한다. 식별이 불가능하면 예외가 그대로 전파되어 fail-closed 된다.</li>
 *   <li>회사코드가 암호화 대상 고객사(SKGAS)일 때만 암호화하고, 그 외 고객사는 평문으로 반환한다.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class FileDownloadEncryptor {

    private final FileEncryptionPolicy fileEncryptionPolicy;
    private final FileCryptoApiClient fileCryptoApiClient;
    private final AuthorizationService authorizationService;

    public byte[] encryptForDownloadIfRequired(String fileName, byte[] plainData) {
        if (!fileEncryptionPolicy.isTargetExtension(fileName)) {
            return plainData;
        }

        String companyCd = authorizationService.getCompanyCdWithPriority();
        if (!fileEncryptionPolicy.isEnabledCompany(companyCd)) {
            return plainData;
        }

        return fileCryptoApiClient.encrypt(plainData, fileName);
    }
}
