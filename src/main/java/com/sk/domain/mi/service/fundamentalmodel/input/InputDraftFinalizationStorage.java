package com.sk.domain.mi.service.fundamentalmodel.input;

import static com.sk.domain.mi.service.fundamentalmodel.input.InputDraftFinalizationService.*;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.sk.domain.shared.client.FileCryptoApiClient;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository.Operation;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository.WorkerOwner;
import com.sk.domain.shared.service.FileEncryptionPolicy;
import com.sk.domain.shared.service.ObjectStorageService;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

/** FileService와 같은 bucket/복호화 정책과 storage primitive만 재사용한다. metadata/rollback hook은 호출하지 않는다. */
final class InputDraftFinalizationStorage implements ObjectsStore {
    private final ObjectStorageService storage;
    private final FileEncryptionPolicy protection;
    private final FileCryptoApiClient crypto;
    private final String endpoint;
    private final String commonBucket;
    private final String prefix;

    InputDraftFinalizationStorage(ObjectStorageService storage, FileEncryptionPolicy protection, FileCryptoApiClient crypto,
            String endpoint, String commonBucket, String prefix) {
        this.storage = Objects.requireNonNull(storage);
        this.protection = Objects.requireNonNull(protection);
        this.crypto = Objects.requireNonNull(crypto);
        this.endpoint = endpoint.replaceAll("/+$", "");
        this.commonBucket = commonBucket;
        this.prefix = prefix;
    }

    @Override public Reference plan(WorkerOwner owner, Operation operation, String role) {
        if (!ROLES.contains(role) || !java.util.Set.of("USER", "ADMIN").contains(operation.role())) throw InputDraftException.missing();
        String bucket = operation.role().equals("ADMIN") ? commonBucket : prefix + owner.companyCode().toLowerCase(Locale.ROOT);
        String key = "SCNRCASE/INPUTDRAFT/" + uuid(operation.id()) + "/" + uuid(operation.token()) + "/" + role + ".xlsm";
        Reference result = new Reference(bucket, key, endpoint + "/" + bucket + "/" + key, role + ".xlsm", MIME, owner.companyCode(), null, null, null);
        result.validate(operation, role, owner);
        return result;
    }

    @Override public void upload(Reference ref, Path generated) throws Exception {
        allowed(ref);
        if (protection.isEnabled(ref.company(), ref.name())) {
            // 고정 원본은 공통 복호화 후 저장된 파일이다. 기존 고객사/확장자 정책을 그대로 적용한다.
            byte[] plain = crypto.decrypt(Files.readAllBytes(generated), ref.name());
            try {
                try (InputStream input = new ByteArrayInputStream(plain)) { verify(input, ref); }
                try (InputStream input = new ByteArrayInputStream(plain)) {
                    storage.uploadFile(input, ref.bucket(), ref.key(), false, ref.mime(), plain.length);
                }
            } finally { java.util.Arrays.fill(plain, (byte) 0); }
        } else {
            try (InputStream input = Files.newInputStream(generated)) {
                storage.uploadFile(input, ref.bucket(), ref.key(), false, ref.mime(), Files.size(generated));
            }
        }
    }

    @Override public void verifyReadable(Reference ref) throws Exception {
        allowed(ref);
        try (var object = storage.getObject(ref.bucket(), ref.key()); InputStream input = object.getObjectContent()) {
            verify(input, ref);
        }
    }

    @Override public void delete(Reference ref) {
        allowed(ref);
        storage.deleteFile(ref.bucket(), ref.key());
    }

    @Override public boolean absent(Reference ref) throws Exception {
        allowed(ref);
        try (var object = storage.getObject(ref.bucket(), ref.key())) {
            return false;
        } catch (AmazonS3Exception ex) {
            // 권한/네트워크 오류나 미확인 null은 미발견 증거가 아니다.
            if (ex.getStatusCode() == 404 && "NoSuchKey".equals(ex.getErrorCode())) return true;
            throw ex;
        }
    }

    private void allowed(Reference ref) {
        if (ref.company() == null || ref.company().isBlank() || ref.bucket() == null
                || !(ref.bucket().equals(commonBucket) || ref.bucket().equals(prefix + ref.company().toLowerCase(Locale.ROOT)))
                || ref.key() == null || !ref.key().matches("SCNRCASE/INPUTDRAFT/[0-9a-f-]{36}/[0-9a-f-]{36}/(SUPPLY|DEMAND|TRADE|OUTPUT)\\.(xlsm|xlsx)")
                || !Objects.equals(ref.location(), endpoint + "/" + ref.bucket() + "/" + ref.key()) || !(ref.name()!=null&&ref.name().endsWith(".xlsx")?InputUploadValidation.XLSX:MIME).equals(ref.mime())
                || !ref.key().endsWith("/"+ref.name())) {
            throw InputDraftException.missing();
        }
    }

    private static void verify(InputStream input, Reference ref) throws Exception {
        if (ref.size() == null || ref.size() <= 0 || ref.size() > 300L * 1024 * 1024 || ref.sha256() == null) throw InputDraftException.internal();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[65536];
        long size = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            size += read;
            if (size > ref.size()) throw InputDraftException.internal();
            digest.update(buffer, 0, read);
        }
        if (size != ref.size() || !HexFormat.of().formatHex(digest.digest()).equals(ref.sha256())) throw InputDraftException.internal();
    }
}
