package com.sk.domain.shared.service;

import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.DateUtil;
import com.sk.common.utils.PathUtil;
import com.sk.common.utils.RoleUtil;
import com.sk.domain.admin.dto.response.file.FileDownloadRes;
import com.sk.domain.admin.dto.response.file.FileUploadRes;
import com.sk.domain.shared.client.FileCryptoApiClient;
import com.sk.domain.shared.dto.request.file.FileDownloadReq;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.enums.DownloadEncryptionDecision;
import com.sk.domain.shared.enums.FileRelationshipObjectSeparateCode;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FileService {
    private static final String TEMP_DIR = "TEMP";
    private static final int STAGED_RELATIONSHIP_NO = 0;
    private static final String INTERNAL_STAGED_FINAL_RELATIONSHIP_CODE = "SCNRCASE";
    private static final String FILE_NO_MESSAGE_FORMAT = "file no: %d";
    private static final String FILE_DOWNLOAD_ERROR_MESSAGE = "파일 다운로드 중 오류가 발생했습니다.";
    private static final String FILE_URL_ERROR_MESSAGE = "파일 URL 생성 중 오류가 발생했습니다.";
    private final UploadedFileRepository uploadedFileRepository;
    private final ObjectStorageService objectStorageService;
    private final FileEncryptionPolicy fileEncryptionPolicy;
    private final FileCryptoApiClient fileCryptoApiClient;
    @Value("${ncp.storage.end-point}")
    private String endPoint;

    @Value("${ncp.storage.bucket-name}")
    private String commBucketName;

    @Value("${ncp.storage.prefix}")
    private String prefix;

    @Value("${server.domain}")
    private String serverDomain;

    /**
    * 파일를 조회한다.
    *
    * @param fileNo 파일번호
    * @return UploadedFile
    */
    public UploadedFile getFile(Long fileNo) {
        return uploadedFileRepository.findByFileNo(fileNo)
                .orElseThrow(
                        () -> AppErrorException.of(
                                ResultCode.Error.RSC_NOT_FOUND,
                                FILE_NO_MESSAGE_FORMAT.formatted(fileNo)));
    }

    /**
    * 파일들을 조회한다.
    *
    * @param fileRelationshipObjectDistinguishNo 파일관계객체식별번호
    * @param fileRelationshipObjectSeparateCode 파일관계객체구분코드
    * @return UploadedFile
    */
    public List<UploadedFile> getFiles(Integer fileRelationshipObjectDistinguishNo, String fileRelationshipObjectSeparateCode) {
        return uploadedFileRepository.findByRelationshipObjectInfo(fileRelationshipObjectDistinguishNo, fileRelationshipObjectSeparateCode);
    }

    /**
    * 파일을 업로드 한다.
    *
    * @param companyCd 회사코드
    * @param role 역할
    * @param file 단일 파일 업로드
    * @param isPublic 공용 여부
    * @return FileUploadRes
    */
    @Transactional
    public FileUploadRes uploadFile(String companyCd, Role role, MultipartFile file, boolean isPublic) {
        String separateCode = FileRelationshipObjectSeparateCode.TEMP.getCode();
        return upload(companyCd, role, file, isPublic, null, separateCode);
    }

    /**
    * 파일을 업로드 한다.
    *
    * @param companyCd 회사코드
    * @param role 역할
    * @param file 단일 파일 업로드
    * @param isPublic 공용 여부
    * @param fileHash 파일 해시
    * @return FileUploadRes
    */
    @Transactional
    public FileUploadRes uploadFile(String companyCd, Role role, MultipartFile file, boolean isPublic, String fileHash) {
        String bucketDir = FileRelationshipObjectSeparateCode.TEMP.getCode();
        return upload(companyCd, role, file, isPublic, fileHash, bucketDir);
    }

    /**
     * 파일을 지정 relation으로 바로 업로드 한다.
     *
     * @param companyCd 회사코드
     * @param role 역할
     * @param file 단일 파일 업로드
     * @param isPublic 공용 여부
     * @param fileRelationshipObjectDistinguishNo 파일관계객체식별번호
     * @param fileRelationshipObjectSeparateCode 파일관계객체구분코드
     * @return FileUploadRes
     */
    @Transactional
    public FileUploadRes uploadFileToRelationship(
            String companyCd,
            Role role,
            MultipartFile file,
            boolean isPublic,
            Integer fileRelationshipObjectDistinguishNo,
            String fileRelationshipObjectSeparateCode) {
        if (fileRelationshipObjectDistinguishNo == null || fileRelationshipObjectSeparateCode == null || fileRelationshipObjectSeparateCode.isBlank()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "파일 relation 정보가 올바르지 않습니다.");
        }

        return upload(
                companyCd,
                role,
                file,
                isPublic,
                null,
                UploadDestination.relationship(
                        fileRelationshipObjectDistinguishNo,
                        fileRelationshipObjectSeparateCode));
    }

    /**
     * Object는 최종 relation prefix에 업로드하고 metadata는 TEMP/0 staged marker로 기록한다.
     * Scenario lock/Case transaction 밖에서 호출해야 한다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public FileUploadRes uploadStagedFileToRelationshipPath(
            String companyCd,
            Role role,
            MultipartFile file,
            boolean isPublic,
            String finalRelationshipCode) {
        if (finalRelationshipCode == null || finalRelationshipCode.isBlank()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "파일 relation 정보가 올바르지 않습니다.");
        }

        return upload(
                companyCd,
                role,
                file,
                isPublic,
                null,
                UploadDestination.staged(finalRelationshipCode));
    }

    private void registerUploadedObjectRollbackCompensation(UploadedFile uploadedFile) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        String bucketName;
        String filePath;
        try {
            bucketName = getBucketName(uploadedFile.getStoragePathNm());
            filePath = getFilePath(uploadedFile.getStoragePathNm());
        } catch (MalformedURLException e) {
            throw AppErrorException.of(
                ResultCode.Error.SYS_INTERNAL_ERROR,
                "uploaded file storage path is invalid. fileNo: %d".formatted(uploadedFile.getFileNo())
            );
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    deleteUploadedObjectQuietly(bucketName, filePath);
                }
            }
        });
    }

    /**
     * 정형파일을 업로드 한다.
     * 정형파일 업로드시 OAuth2.0 인증의 경우 버킷 디렉토리가 정형 디렉토리로 바로 설정된다.
     *
     * @param companyCd 회사코드
     * @param role 역할
     * @param file 단일 파일 업로드
     * @param isPublic 공용 여부
     * @return FileUploadRes
     */
    @Transactional
    public FileUploadRes uploadStructuredFile(String companyCd, Role role, MultipartFile file, boolean isPublic) {
        String bucketDir = switch (role) {
            case USER, ADMIN, OPERATOR, SYSTEM -> FileRelationshipObjectSeparateCode.TEMP.getCode();
            case API_SUB_ADMIN, API_PLUS_OPERATOR -> FileRelationshipObjectSeparateCode.GDATS.getCode();
            default -> throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "역할 코드가 올바르지 않습니다.");
        };

        return upload(companyCd, role, file, isPublic, null, bucketDir);
    }

    /**
     * 파일을 업로드 한다.
     *
     * @param companyCd 회사코드
     * @param role 역할
     * @param file 단일 파일 업로드
     * @param isPublic 공용 여부
     * @param fileHash 파일 해시
     * @return FileUploadRes
     */
    private FileUploadRes upload(String companyCd, Role role, MultipartFile file, boolean isPublic, String fileHash, String separateCode) {
        return upload(companyCd, role, file, isPublic, fileHash, UploadDestination.temporary(separateCode));
    }

    private FileUploadRes upload(
            String companyCd,
            Role role,
            MultipartFile file,
            boolean isPublic,
            String fileHash,
            UploadDestination destination) {
        if (file == null) {
            throw AppErrorException.of(ResultCode.Error.FILE_INVALID);
        }

        if (file.getSize() <= 0) {
            throw AppErrorException.of(ResultCode.Error.FILE_SIZE_ERROR);
        }

        // 26.07.29 취약점 진단 수정, '표준 라이브러리의 널 반환 값 역참조-102127'
        String rawFileName = file.getOriginalFilename();
        if (rawFileName == null) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "파일명이 없습니다.");
        }

        String uploadedBucketName = null;
        String uploadedFileName = null;
        try {
            String originalFileNm = Normalizer.normalize(rawFileName, Normalizer.Form.NFC);
            String fileExtNm = originalFileNm.substring(originalFileNm.lastIndexOf('.') + 1).toLowerCase();
            long fileSize = file.getSize();
            String mimeTypeNm = file.getContentType();

            String customerCompanyCd = companyCd.toLowerCase();
            String bucketName = RoleUtil.isAdmin(role) || RoleUtil.isSubAdmin(role) ? commBucketName : prefix + customerCompanyCd;
            String storageFileNm = UUID.randomUUID().toString().concat(getFileExtension(rawFileName));
            String storagePathNm = PathUtil.IncludeTrailingPathDelimiter(endPoint) + PathUtil.IncludeTrailingPathDelimiter(bucketName)
                    + PathUtil.IncludeTrailingPathDelimiter(destination.storageDirectory())
                    + storageFileNm;

            // Object Storage 저장
            String fileName = PathUtil.IncludeTrailingPathDelimiter(destination.storageDirectory()) + storageFileNm;
            String fileUrl;
            if (fileEncryptionPolicy.isEnabled(companyCd, originalFileNm)) {
                byte[] decryptedFileData = fileCryptoApiClient.decrypt(file.getBytes(), originalFileNm);
                fileSize = decryptedFileData.length;
                fileUrl = objectStorageService.uploadFile(
                        new ByteArrayInputStream(decryptedFileData),
                        bucketName,
                        fileName,
                        isPublic,
                        mimeTypeNm,
                        fileSize);
            } else {
                fileUrl = objectStorageService.uploadFile(file, bucketName, fileName, isPublic);
            }
            uploadedBucketName = bucketName;
            uploadedFileName = fileName;

            // DB 에 저장
            UploadedFile savedUploadedFile = uploadedFileRepository.save(UploadedFile.of(
                    originalFileNm,
                    storageFileNm,
                    storagePathNm,
                    fileSize,
                    fileExtNm,
                    mimeTypeNm,
                    DateUtil.endYmd(),
                    destination.relationshipNo(),
                    destination.relationshipCode(),
                    fileHash));
            registerUploadedObjectRollbackCompensation(savedUploadedFile);

            return FileUploadRes.of(savedUploadedFile.getFileNo(), originalFileNm, fileSize, fileUrl);
        } catch (Exception e) {
            deleteUploadedObjectQuietly(uploadedBucketName, uploadedFileName);
            throw internalFileException("파일 업로드 중 오류가 발생했습니다.", e);
        }
    }

    private void deleteUploadedObjectQuietly(String bucketName, String fileName) {
        if (bucketName == null || fileName == null) {
            return;
        }

        try {
            objectStorageService.deleteFile(bucketName, fileName);
        } catch (Exception deleteException) {
            log.warn("Failed to compensate uploaded object. bucketName={}, fileName={}", bucketName, fileName, deleteException);
        }
    }

    /**
    * 1개 파일을 다운로드 한다.
    *
    * @param fileNo 파일번호
    * @return FileDownloadRes
    */
    public FileDownloadRes downloadFile(FileDownloadReq dto) {
        long metadataStartedAt = System.nanoTime();
        UploadedFile uploadedFile = uploadedFileRepository.findByFileNo(dto.fileNo())
                .orElseThrow(
                        () -> AppErrorException.of(
                                ResultCode.Error.RSC_NOT_FOUND,
                                FILE_NO_MESSAGE_FORMAT.formatted(dto.fileNo())));
        ensureNotInternalStagedFile(uploadedFile);
        long metadataMs = elapsedMs(metadataStartedAt);

        // 26.07.28 취약점 진단 검토, '누락된 널 값 검사-102129,102130,102131,102132,102133,102134,102135,102136,102137,102138', 오탐 -> uploadedFile은 orElseThrow()로 not null 보장됨.
        if (!(uploadedFile.getOriginalFileNm().equals(dto.fileNm()) && uploadedFile.getFileSize().equals(dto.fileSize()))) {
            throw AppErrorException.of(ResultCode.Error.FILE_ACCESS_DENIED);
        }

        try {
            String bucketName = getBucketName(uploadedFile.getStoragePathNm());
            String filePath = getFilePath(uploadedFile.getStoragePathNm());

            long storageStartedAt = System.nanoTime();
            byte[] fileData = objectStorageService.downloadFile(bucketName, filePath);
            long storageMs = elapsedMs(storageStartedAt);

            log.info(
                    "File download data prepared. fileNo={}, fileSizeBytes={}, extension={}, metadataMs={}, storageMs={}",
                    uploadedFile.getFileNo(),
                    uploadedFile.getFileSize(),
                    uploadedFile.getFileExtNm(),
                    metadataMs,
                    storageMs);

            return FileDownloadRes.of(uploadedFile.getFileNo(), uploadedFile.getOriginalFileNm(), uploadedFile.getFileSize(), fileData);
        } catch (Exception e) {
            throw internalFileException(FILE_DOWNLOAD_ERROR_MESSAGE, e);
        }
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    /**
    * 1개 이상 파일을 다운로드 한다.
    *
    * @param fileNoList 파일번호목록
    * @return byte[]
    */
    public byte[] downloadFiles(List<FileDownloadReq> list) {
        return downloadFiles(list, null, null);
    }

    public byte[] downloadFiles(List<FileDownloadReq> list, Role role, String companyCd) {
        List<UploadedFile> uploadedFiles = validateDownloadRequests(list);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (UploadedFile f : uploadedFiles) {
                String bucketName = getBucketName(f.getStoragePathNm());
                String filePath = getFilePath(f.getStoragePathNm());
                String fileName = f.getOriginalFileNm();

                try (S3ObjectInputStream s3is = objectStorageService.getObjectContent(bucketName, filePath)) {
                    if (s3is == null)
                        continue;

                    zos.putNextEntry(new ZipEntry(fileName));
                    DownloadEncryptionDecision decision = fileEncryptionPolicy.evaluateDownload(role, companyCd, fileName);
                    if (decision.shouldEncrypt()) {
                        byte[] plainData = IOUtils.toByteArray(s3is);
                        // 26.07.28 취약점 진단 검토, '누락된 널 값 검사-102265', 오탐 -> fileName은 originalFileNm 필수값이며 상단 ZipEntry 생성 시점에 non null이 보장됨.
                        byte[] encryptedData = fileCryptoApiClient.encrypt(plainData, fileName);
                        zos.write(encryptedData);
                    } else {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = s3is.read(buffer)) != -1) {
                            zos.write(buffer, 0, len);
                        }
                    }
                    zos.closeEntry();
                }
            }
            zos.finish();

            return baos.toByteArray();
        } catch (IOException e) {
            throw internalFileException(FILE_DOWNLOAD_ERROR_MESSAGE, e);
        }
    }

    private List<UploadedFile> validateDownloadRequests(List<FileDownloadReq> list) {
        List<Long> fileNoList = list.stream()
                .map(FileDownloadReq::fileNo)
                .toList();
        List<UploadedFile> uploadedFiles = uploadedFileRepository.findByFileNoList(fileNoList);
        if (uploadedFiles.isEmpty() || uploadedFiles.size() != new HashSet<>(fileNoList).size()) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("file no: %s", fileNoList));
        }
        uploadedFiles.forEach(this::ensureNotInternalStagedFile);

        Map<Long, UploadedFile> uploadedFileByNo = uploadedFiles.stream()
                .collect(Collectors.toMap(UploadedFile::getFileNo, uploadedFile -> uploadedFile));
        for (FileDownloadReq request : list) {
            UploadedFile uploadedFile = uploadedFileByNo.get(request.fileNo());
            if (!matchesDownloadRequest(uploadedFile, request)) {
                throw AppErrorException.of(ResultCode.Error.FILE_ACCESS_DENIED);
            }
        }
        return uploadedFiles;
    }

    private boolean matchesDownloadRequest(UploadedFile uploadedFile, FileDownloadReq request) {
        return uploadedFile != null
                && uploadedFile.getOriginalFileNm().equals(request.fileNm())
                && uploadedFile.getFileSize().equals(request.fileSize());
    }

    /**
    * 1개 이상 파일을 다운로드 한다.*
    * 2개 이상일 경우, 주어진 file 이름으로 zip 압축을 하고,
    * 1개일 경우 주어진 URL 의 단일 파일로 다운로드 한다.
    *
    * @param urlList 파일 URL 목록
    * @param zipFileName ZIP 파일명
    * @return FileDownloadRes
    */
    public FileDownloadRes directDownloadFiles(List<String> urlList, String zipFileName) {
        if (urlList.size() == 1) {
            return directDownloadFile(urlList.get(0));
        }

        return directDownloadFilesAsZip(urlList, zipFileName);
    }

    private FileDownloadRes directDownloadFile(String url) {
        try {
            String bucketName = getBucketName(url);
            String filePath = getFilePath(url);
            String fileName = getFileName(url);
            return readDirectDownloadFile(bucketName, filePath, fileName);
        } catch (IOException e) {
            throw internalFileException(FILE_DOWNLOAD_ERROR_MESSAGE, e);
        }
    }

    private FileDownloadRes readDirectDownloadFile(String bucketName, String filePath, String fileName)
            throws IOException {
        try (S3ObjectInputStream s3is = objectStorageService.getObjectContent(bucketName, filePath)) {
            byte[] fileData = IOUtils.toByteArray(s3is);
            return FileDownloadRes.of(0L, fileName, (long) fileData.length, fileData);
        }
    }

    private FileDownloadRes directDownloadFilesAsZip(List<String> urlList, String zipFileName) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ZipOutputStream zos = new ZipOutputStream(baos)) {
            Set<String> addedNames = new HashSet<>();
            for (String url : urlList) {
                writeDirectDownloadZipEntry(zos, url, addedNames);
            }
            zos.finish();

            byte[] fileData = baos.toByteArray();
            return FileDownloadRes.of(0L, zipFileName, (long)fileData.length, fileData);
        } catch (IOException e) {
            throw internalFileException(FILE_DOWNLOAD_ERROR_MESSAGE, e);
        }
    }

    private void writeDirectDownloadZipEntry(ZipOutputStream zos, String url, Set<String> addedNames)
            throws IOException {
        String bucketName = getBucketName(url);
        String filePath = getFilePath(url);

        try (S3ObjectInputStream s3is = objectStorageService.getObjectContent(bucketName, filePath)) {
            if (s3is == null) {
                return;
            }

            String fileName = uniqueFileName(getFileName(url), addedNames);
            zos.putNextEntry(new ZipEntry(fileName));
            byte[] buffer = new byte[4096];
            int len;
            while ((len = s3is.read(buffer)) != -1) {
                zos.write(buffer, 0, len);
            }
            zos.closeEntry();
        }
    }

    private String uniqueFileName(String fileName, Set<String> addedNames) {
        if (addedNames.add(fileName)) {
            return fileName;
        }

        String base = FilenameUtils.getBaseName(fileName);
        String extension = FilenameUtils.getExtension(fileName);
        int index = 1;
        String candidate;
        do {
            candidate = base + "_" + index++ + (extension.isEmpty() ? "" : "." + extension);
        } while (!addedNames.add(candidate));
        return candidate;
    }

    /**
    * 파일을 삭제 한다.
    *
    * @param fileNo 파일번호
    * @return UploadedFile
    */
    @Transactional
    public UploadedFile deleteFile(Long fileNo) {
        UploadedFile uploadedFile = uploadedFileRepository.findByFileNo(fileNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, FILE_NO_MESSAGE_FORMAT.formatted(fileNo)));
        ensureNotInternalStagedFile(uploadedFile);
        // 26.07.28 취약점 진단 검토, '누락된 널 값 검사-102124,102125,102126', 오탐 -> uploadedFile은 orElseThrow()로 not null 보장됨.
        try {
            String bucketName = getBucketName(uploadedFile.getStoragePathNm());
            String filePath = getFilePath(uploadedFile.getStoragePathNm());

            uploadedFile.softDelete();

            objectStorageService.deleteFile(bucketName, filePath);
        } catch (Exception e) {
            throw internalFileException("파일 삭제 중 오류가 발생했습니다.", e);
        }

        return uploadedFile;
    }

    /**
    * 파일들을 삭제 한다.
    *
    * @param fileRelationshipObjectDistinguishNo 파일관계객체식별번호
    * @param fileRelationshipObjectSeparateCode 파일관계객체구분코드
    * @return
    */
    @Transactional
    public void deleteFiles(Integer fileRelationshipObjectDistinguishNo, String fileRelationshipObjectSeparateCode) {
        List<UploadedFile> fileList = uploadedFileRepository.findByRelationshipObjectInfo(
                fileRelationshipObjectDistinguishNo,
                fileRelationshipObjectSeparateCode);
        fileList.forEach(f -> {
            try {
                String bucketName = getBucketName(f.getStoragePathNm());
                String filePath = getFilePath(f.getStoragePathNm());

                f.softDelete();

                objectStorageService.deleteFile(bucketName, filePath);
            } catch (Exception e) {
                throw internalFileException("파일 삭제 중 오류가 발생했습니다.", e);
            }
        });
    }

    /**
    * 파일 정보를 수정한다.
    *
    * @param fileNo 파일번호
    * @param fileRelationshipObjectDistinguishNo 파일관계객체식별번호
    * @param fileRelationshipObjectSeparateCode 파일관계객체구분코드
    * @return UploadedFile
    */
    @Transactional
    public UploadedFile updateFile(
            Long fileNo,
            Integer fileRelationshipObjectDistinguishNo,
            String fileRelationshipObjectSeparateCode,
            String fileValidEndDt) {
        UploadedFile uploadedFile = uploadedFileRepository.findByFileNo(fileNo)
                .orElseThrow(
                        () -> AppErrorException.of(
                                ResultCode.Error.RSC_NOT_FOUND,
                                FILE_NO_MESSAGE_FORMAT.formatted(fileNo)));
        ensureNotInternalStagedFile(uploadedFile);

        // 26.07.28 취약점 진단 검토, '누락된 널 값 검사-102119,102120,102121,102122,102123', 오탐 -> uploadedFile은 orElseThrow()로 not null 보장됨.
        if (uploadedFile.getFileRelationshipObjectSeparateCd() == null || uploadedFile.getFileRelationshipObjectSeparateCd().equals(TEMP_DIR)) {
            try {
                String bucketName = getBucketName(uploadedFile.getStoragePathNm());
                String oldPath = getFilePath(uploadedFile.getStoragePathNm());
                String separateCode = fileRelationshipObjectSeparateCode != null ? fileRelationshipObjectSeparateCode : TEMP_DIR;
                String oldSeparateCode = uploadedFile.getFileRelationshipObjectSeparateCd() != null ? uploadedFile.getFileRelationshipObjectSeparateCd() : TEMP_DIR;
                String newPath = oldPath.replaceFirst(oldSeparateCode, separateCode);
                String storagePathNm = PathUtil.IncludeTrailingPathDelimiter(endPoint) + PathUtil.IncludeTrailingPathDelimiter(bucketName) + newPath;

                uploadedFile.update(storagePathNm, fileRelationshipObjectDistinguishNo, fileRelationshipObjectSeparateCode, fileValidEndDt);

                objectStorageService.moveFile(bucketName, oldPath, newPath);
            } catch (Exception e) {
                throw internalFileException("파일 수정 중 오류가 발생했습니다.", e);
            }
        } else {
            // 26.07.28 취약점 진단 검토, '누락된 널 값 검사-102117,102118', 오탐 -> uploadedFile은 orElseThrow()로 not null 보장됨.
            uploadedFile.update(uploadedFile.getStoragePathNm(), fileRelationshipObjectDistinguishNo, fileRelationshipObjectSeparateCode, fileValidEndDt);
        }
        return uploadedFile;
    }

    /**
    * 파일에 대한 URL을 생성한다.
    *
    * @param fileNo 파일번호
    * @return URL
    */
    public String getUrl(Long fileNo) {
        UploadedFile uploadedFile = uploadedFileRepository.findByFileNo(fileNo)
                .orElseThrow(
                        () -> AppErrorException.of(
                                ResultCode.Error.RSC_NOT_FOUND,
                                FILE_NO_MESSAGE_FORMAT.formatted(fileNo)));
        ensureNotInternalStagedFile(uploadedFile);
        // 26.07.28 취약점 진단 검토, '누락된 널 값 검사-102188,102189', 오탐 -> uploadedFile은 orElseThrow()로 not null 보장됨.
        try {
            String bucketName = getBucketName(uploadedFile.getStoragePathNm());
            String filePath = getFilePath(uploadedFile.getStoragePathNm());

            return objectStorageService.getUrl(bucketName, filePath);
        } catch (Exception e) {
            throw internalFileException(FILE_URL_ERROR_MESSAGE, e);
        }
    }

    /**
     * 현재 transaction에서 TEMP/0 staged row를 최종 relation에 metadata-only로 연결한다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<UploadedFile> attachStagedFilesToRelationship(
            List<Long> fileNoList,
            Integer fileRelationshipObjectDistinguishNo,
            String fileRelationshipObjectSeparateCode,
            String fileValidEndDt) {
        if (fileNoList == null || fileNoList.isEmpty()
                || fileRelationshipObjectDistinguishNo == null
                || fileRelationshipObjectSeparateCode == null
                || fileRelationshipObjectSeparateCode.isBlank()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "staged file relation 정보가 올바르지 않습니다.");
        }

        Set<Long> distinctFileNos = new HashSet<>(fileNoList);
        if (distinctFileNos.size() != fileNoList.size()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "staged fileNo는 중복될 수 없습니다.");
        }

        List<UploadedFile> stagedFiles = uploadedFileRepository.findStagedFilesForUpdate(
                fileNoList,
                STAGED_RELATIONSHIP_NO,
                TEMP_DIR);
        boolean allExpectedRowsLocked = stagedFiles.size() == fileNoList.size()
                && stagedFiles.stream().map(UploadedFile::getFileNo).collect(java.util.stream.Collectors.toSet())
                .equals(distinctFileNos);
        boolean allUseFinalStoragePath = stagedFiles.stream()
                .allMatch(file -> isInternalStagedFile(file, fileRelationshipObjectSeparateCode));
        if (!allExpectedRowsLocked || !allUseFinalStoragePath) {
            throw AppErrorException.of(
                    ResultCode.Error.BIZ_RULE_VIOLATION,
                    "staged files cannot be attached from current state.");
        }

        stagedFiles.forEach(file -> file.attachToRelationship(
                fileRelationshipObjectDistinguishNo,
                fileRelationshipObjectSeparateCode,
                fileValidEndDt));
        uploadedFileRepository.flush();
        return stagedFiles;
    }

    /**
     * staged row 하나를 lock한 뒤 object와 metadata를 함께 정리한다.
     * Scenario/Case transaction과 분리된 보상·TTL 정리 전용 경계다.
     */
    @Transactional
    public boolean deleteStagedFile(Long fileNo, String finalRelationshipCode) {
        Optional<UploadedFile> stagedFile = uploadedFileRepository.findStagedFileForUpdate(
                fileNo,
                STAGED_RELATIONSHIP_NO,
                TEMP_DIR);
        if (stagedFile.isEmpty() || !isInternalStagedFile(stagedFile.orElseThrow(), finalRelationshipCode)) {
            return false;
        }

        UploadedFile uploadedFile = stagedFile.orElseThrow();
        try {
            String bucketName = getBucketName(uploadedFile.getStoragePathNm());
            String filePath = getFilePath(uploadedFile.getStoragePathNm());
            objectStorageService.deleteFile(bucketName, filePath);
            uploadedFile.softDelete();
            return true;
        } catch (Exception e) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "staged 파일 삭제 중 오류가 발생했습니다.");
        }
    }

    public List<UploadedFile> findExpiredStagedFiles(
            String finalRelationshipCode,
            LocalDateTime createdBefore,
            int batchSize) {
        if (batchSize <= 0) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "staged file cleanup batchSize는 1 이상이어야 합니다.");
        }
        return uploadedFileRepository.findExpiredStagedFiles(
                STAGED_RELATIONSHIP_NO,
                TEMP_DIR,
                storagePathPattern(finalRelationshipCode),
                createdBefore,
                PageRequest.of(0, batchSize));
    }

    public String getUrl(UploadedFile uploadedFile) {
        if (uploadedFile == null) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "업로드된 파일이 없습니다.");
        }
        ensureNotInternalStagedFile(uploadedFile);

        try {
            String bucketName = getBucketName(uploadedFile.getStoragePathNm());
            String filePath = getFilePath(uploadedFile.getStoragePathNm());

            return objectStorageService.getUrl(bucketName, filePath);
        } catch (Exception e) {
            throw internalFileException(FILE_URL_ERROR_MESSAGE, e);
        }
    }

    public String getSecureUrl(Long fileNo) {
        UploadedFile uploadedFile = uploadedFileRepository.findByFileNo(fileNo)
                .orElseThrow(
                        () -> AppErrorException.of(
                                ResultCode.Error.RSC_NOT_FOUND,
                                FILE_NO_MESSAGE_FORMAT.formatted(fileNo)));
        ensureNotInternalStagedFile(uploadedFile);
        // 26.07.28 취약점 진단 검토, '누락된 널 값 검사-102139,102140', 오탐 -> uploadedFile은 orElseThrow()로 not null 보장됨.
        try {
            String bucketName = getBucketName(uploadedFile.getStoragePathNm());
            String filePath = getFilePath(uploadedFile.getStoragePathNm());

            return PathUtil.IncludeTrailingPathDelimiter(serverDomain) + PathUtil.IncludeTrailingPathDelimiter("images")
                    + PathUtil.IncludeTrailingPathDelimiter(bucketName) + filePath;
        } catch (Exception e) {
            throw internalFileException(FILE_URL_ERROR_MESSAGE, e);
        }
    }

    public String getSecureUrl(UploadedFile uploadedFile) {
        if (uploadedFile == null) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "업로드된 파일이 없습니다.");
        }
        ensureNotInternalStagedFile(uploadedFile);

        try {
            String bucketName = getBucketName(uploadedFile.getStoragePathNm());
            String filePath = getFilePath(uploadedFile.getStoragePathNm());

            return PathUtil.IncludeTrailingPathDelimiter(serverDomain) + PathUtil.IncludeTrailingPathDelimiter("images")
                    + PathUtil.IncludeTrailingPathDelimiter(bucketName) + filePath;

        } catch (Exception e) {
            throw internalFileException(FILE_URL_ERROR_MESSAGE, e);
        }
    }

    /**
    * 파일에 대한 URL을 생성한다.
    *
    * @param fileNo 파일번호
    * @return URL
    */
    public String getPresignedUrl(String url) {
        try {
            String bucketName = getBucketName(url);
            String filePath = getFilePath(url);

            return objectStorageService.getPresignedUrl(bucketName, filePath);
        } catch (Exception e) {
            throw internalFileException(FILE_URL_ERROR_MESSAGE, e);
        }
    }

    private void ensureNotInternalStagedFile(UploadedFile uploadedFile) {
        if (isInternalStagedFile(uploadedFile, INTERNAL_STAGED_FINAL_RELATIONSHIP_CODE)) {
            throw AppErrorException.of(
                    ResultCode.Error.RSC_NOT_FOUND,
                    FILE_NO_MESSAGE_FORMAT.formatted(uploadedFile.getFileNo()));
        }
    }

    private AppErrorException internalFileException(String message, Exception cause) {
        log.error(message, cause);
        return AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, message);
    }

    private boolean isInternalStagedFile(UploadedFile uploadedFile, String finalRelationshipCode) {
        return uploadedFile != null
                && Integer.valueOf(STAGED_RELATIONSHIP_NO).equals(uploadedFile.getFileRelationshipObjectDistinguishNo())
                && TEMP_DIR.equals(uploadedFile.getFileRelationshipObjectSeparateCd())
                && uploadedFile.getStoragePathNm() != null
                && uploadedFile.getStoragePathNm().contains("/" + finalRelationshipCode + "/");
    }

    private String storagePathPattern(String finalRelationshipCode) {
        if (finalRelationshipCode == null || finalRelationshipCode.isBlank()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "finalRelationshipCode is required.");
        }
        return "%/" + finalRelationshipCode + "/%";
    }

    public List<UploadedFile> fileExist(String fileName, String fileHash) {
        return uploadedFileRepository.findByFileNameAndFileHash(Normalizer.normalize(fileName, Normalizer.Form.NFC),
                fileHash);
    }

    private String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf("."));
    }

    /**
     * URL에서 경로(path) 부분을 "/" 기준으로 분리한 배열을 반환합니다.
     * 예시: "/버킷명/폴더/파일명.jpg" → ["", "버킷명", "폴더", "파일명.jpg"]
     *
     * @param path URL 문자열
     * @return 경로를 분리한 문자열 배열
     * @throws MalformedURLException 잘못된 URL인 경우
     */
    private String[] extractPathSegments(String path) throws MalformedURLException {
        URL url = new URL(path);
        return url.getPath().split("/");
    }

    /**
     * URL에서 버킷 이름(경로상의 두 번째 요소)을 추출합니다.
     * 예: "https://kr.object.ncloudstorage.com/버킷명/폴더/파일명.jpg" → "버킷명"
     *
     * @param path URL 문자열
     * @return 버킷 이름
     * @throws MalformedURLException URL 파싱 실패 시
     * @throws IllegalArgumentException URL에 필요한 정보가 없는 경우
     */
    private String getBucketName(String path) throws MalformedURLException {
        String[] segments = extractPathSegments(path);
        if (segments.length < 2) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "URL에 버킷 이름이 포함되어 있지 않습니다.");
        }
        return segments[1];
    }

    /**
     * URL에서 파일 경로와 파일 이름(버킷 이후의 나머지 경로)을 추출합니다.
     * 예: "https://kr.object.ncloudstorage.com/버킷명/폴더/파일명.jpg" → "폴더/파일명.jpg"
     *
     * @param path URL 문자열
     * @return 파일 경로 및 파일 이름
     * @throws MalformedURLException URL 파싱 실패 시
     * @throws IllegalArgumentException URL에 파일 경로 정보가 없는 경우
     */
    private String getFilePath(String path) throws MalformedURLException {
        String[] segments = extractPathSegments(path);
        if (segments.length < 3) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "URL에 파일 경로 정보가 포함되어 있지 않습니다.");
        }
        return String.join("/", Arrays.copyOfRange(segments, 2, segments.length));
    }

    /**
     * URL에서 파일 경로와 파일 이름(버킷 이후의 나머지 경로)을 추출합니다.
     * 예: "https://kr.object.ncloudstorage.com/버킷명/폴더/파일명.jpg" → "파일명.jpg"
     *
     * @param path URL 문자열
     * @return 파일 경로 및 파일 이름
     * @throws MalformedURLException URL 파싱 실패 시
     * @throws IllegalArgumentException URL에 파일 경로 정보가 없는 경우
     */
    private String getFileName(String path) throws MalformedURLException {
        String[] segments = extractPathSegments(path);
        if (segments.length < 3) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "URL에 파일 경로 정보가 포함되어 있지 않습니다.");
        }
        return segments[segments.length - 1]; // 마지막 요소가 파일 이름
    }

    private record UploadDestination(
            String storageDirectory,
            Integer relationshipNo,
            String relationshipCode) {

        private static UploadDestination temporary(String separateCode) {
            return new UploadDestination(separateCode, STAGED_RELATIONSHIP_NO, separateCode);
        }

        private static UploadDestination relationship(Integer relationshipNo, String relationshipCode) {
            return new UploadDestination(relationshipCode, relationshipNo, relationshipCode);
        }

        private static UploadDestination staged(String finalRelationshipCode) {
            return new UploadDestination(finalRelationshipCode, STAGED_RELATIONSHIP_NO, TEMP_DIR);
        }
    }
}
