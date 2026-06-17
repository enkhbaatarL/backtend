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
import com.sk.domain.shared.enums.FileRelationshipObjectSeparateCode;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FileService {
    private final UploadedFileRepository uploadedFileRepository;
    private final ObjectStorageService objectStorageService;
    private final FileEncryptionPolicy fileEncryptionPolicy;
    private final FileCryptoApiClient fileCryptoApiClient;

    private final String TEMP_DIR = "TEMP";

    @Value("${ncp.storage.end-point}")
    private String endPoint;

    @Value("${ncp.storage.bucket-name}")
    private String commBucketName;

    @Value("${ncp.storage.prefix}")
    private String prefix;

    @Value("${server.domain}")
    private String serverDomain;

    // public void getAll() {
    // objectStorageService.getAll();
    // return;
    // }

    /**
    * 파일를 조회한다.
    *
    * @param fileNo 파일번호
    * @return UploadedFile
    */
    public UploadedFile getFile(Long fileNo) {
        return uploadedFileRepository.findByFileNo(fileNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("file no: %d", fileNo)));
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
                fileRelationshipObjectSeparateCode,
                fileRelationshipObjectDistinguishNo);
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
        return upload(companyCd, role, file, isPublic, fileHash, separateCode, 0);
    }

    private FileUploadRes upload(
            String companyCd,
            Role role,
            MultipartFile file,
            boolean isPublic,
            String fileHash,
            String separateCode,
            Integer fileRelationshipObjectDistinguishNo) {
        if (file == null) {
            throw AppErrorException.of(ResultCode.Error.FILE_INVALID);
        }

        if (file.getSize() <= 0) {
            throw AppErrorException.of(ResultCode.Error.FILE_SIZE_ERROR);
        }

        String uploadedBucketName = null;
        String uploadedFileName = null;
        try {
            String originalFileNm = Normalizer.normalize(file.getOriginalFilename(), Normalizer.Form.NFC);
            String fileExtNm = originalFileNm.substring(originalFileNm.lastIndexOf('.') + 1).toLowerCase();
            long fileSize = file.getSize();
            String mimeTypeNm = file.getContentType();

            String customerCompanyCd = companyCd.toLowerCase();
            String bucketName = RoleUtil.isAdmin(role) || RoleUtil.isSubAdmin(role) ? commBucketName : prefix + customerCompanyCd;
            String storageFileNm = UUID.randomUUID().toString().concat(getFileExtension(file.getOriginalFilename()));
            String storagePathNm = PathUtil.IncludeTrailingPathDelimiter(endPoint) + PathUtil.IncludeTrailingPathDelimiter(bucketName)
                    + PathUtil.IncludeTrailingPathDelimiter(separateCode)
                    + storageFileNm;
            String fileRelationshipObjectSeparateCd = separateCode;

            // Object Storage 저장
            String fileName = PathUtil.IncludeTrailingPathDelimiter(separateCode) + storageFileNm;
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
                    fileRelationshipObjectDistinguishNo,
                    fileRelationshipObjectSeparateCd,
                    fileHash));

            return FileUploadRes.of(savedUploadedFile.getFileNo(), originalFileNm, fileSize, fileUrl);
        } catch (Exception e) {
            deleteUploadedObjectQuietly(uploadedBucketName, uploadedFileName);
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "파일 업로드 중 오류가 발생했습니다." + e.getMessage());
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
        UploadedFile uploadedFile = uploadedFileRepository.findByFileNo(dto.fileNo())
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("file no: %d", dto.fileNo())));

        if (!(uploadedFile.getOriginalFileNm().equals(dto.fileNm()) && uploadedFile.getFileSize().equals(dto.fileSize()))) {
            throw AppErrorException.of(ResultCode.Error.FILE_ACCESS_DENIED);
        }

        try {
            String bucketName = getBucketName(uploadedFile.getStoragePathNm());
            String filePath = getFilePath(uploadedFile.getStoragePathNm());

            byte[] fileData = objectStorageService.downloadFile(bucketName, filePath);

            return FileDownloadRes.of(uploadedFile.getFileNo(), uploadedFile.getOriginalFileNm(), uploadedFile.getFileSize(), fileData);
        } catch (Exception e) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "파일 다운로드 중 오류가 발생했습니다." + e.getMessage());
        }
    }

    /**
    * 1개 이상 파일을 다운로드 한다.
    *
    * @param fileNoList 파일번호목록
    * @return byte[]
    */
    public byte[] downloadFiles(List<FileDownloadReq> list) {
        List<Long> fileNoList = list.stream()
                .map(FileDownloadReq::fileNo)
                .toList();
        List<UploadedFile> uploadedFiles = uploadedFileRepository.findByFileNoList(fileNoList);
        if (uploadedFiles.isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("file no: %s", fileNoList));
        }
        for (UploadedFile f1 : uploadedFiles) {
            for (FileDownloadReq f2 : list) {
                if (f1.getFileNo().equals(f2.fileNo())) {
                    if (f1.getOriginalFileNm().equals(f2.fileNm()) && f1.getFileSize().equals(f2.fileSize())) {
                        continue;
                    } else {
                        throw AppErrorException.of(ResultCode.Error.FILE_ACCESS_DENIED);
                    }
                }
            }
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (UploadedFile f : uploadedFiles) {
                String bucketName = getBucketName(f.getStoragePathNm());
                String filePath = getFilePath(f.getStoragePathNm());
                String fileName = f.getOriginalFileNm();

                // S3에서 스트림으로 읽어와 바로 ZIP에 쓰기
                try (S3ObjectInputStream s3is = objectStorageService.getObjectContent(bucketName, filePath)) {
                    if (s3is == null)
                        continue;

                    zos.putNextEntry(new ZipEntry(fileName));
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = s3is.read(buffer)) != -1) {
                        zos.write(buffer, 0, len);
                    }
                    zos.closeEntry();
                }
            }
            zos.finish();

            return baos.toByteArray();
        } catch (IOException e) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "파일 다운로드 중 오류가 발생했습니다." + e.getMessage());
        }
    }

    /**
    * 1개 이상 파일을 다운로드 한다.*
    * 2개 이상일 경우, 주어진 file 이름으로 zip 압축을 하고,
    * 1개일 경우 주어진 URL 의 단일 파일로 다운로드 한다.
    *
    * @param filePathList 파일번호목록
    * @return byte[]
    */
    public FileDownloadRes directDownloadFiles(List<String> urlList, String zipFileName) {
        try{
            if(urlList.size() == 1){
                String bucketName = getBucketName(urlList.get(0));
                String filePath = getFilePath(urlList.get(0));
                S3ObjectInputStream s3is = objectStorageService.getObjectContent(bucketName, filePath);
                byte[] fileData = IOUtils.toByteArray(s3is);
                String fileName = getFileName(urlList.get(0));
                return FileDownloadRes.of(0L, fileName, (long)fileData.length, fileData);
            }
        }catch (IOException e) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "파일 다운로드 중 오류가 발생했습니다." + e.getMessage());
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ZipOutputStream zos = new ZipOutputStream(baos)) {

            Set<String> addedNames = new HashSet<>();
            for (String url : urlList) {
                String bucketName = getBucketName(url);
                String filePath = getFilePath(url);
                String fileName = getFileName(url);

                // S3에서 스트림으로 읽어와 바로 ZIP에 쓰기
                try (S3ObjectInputStream s3is = objectStorageService.getObjectContent(bucketName, filePath)) {
                    if (s3is == null)
                        continue;

                    // 중복된 파일 이름 처리
                    if (!addedNames.add(fileName)) {
                        String base = FilenameUtils.getBaseName(fileName); // report
                        String ext = FilenameUtils.getExtension(fileName); // pdf
                        int idx = 1;
                        while (!addedNames.add(
                                fileName = base + "_" + idx++ + (ext.isEmpty() ? "" : "." + ext)))
                            ;
                    }

                    zos.putNextEntry(new ZipEntry(fileName));
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = s3is.read(buffer)) != -1) {
                        zos.write(buffer, 0, len);
                    }
                    zos.closeEntry();
                }
            }
            zos.finish();

            byte[] fileData = baos.toByteArray();
            return FileDownloadRes.of(0L, zipFileName, (long)fileData.length, fileData);
        } catch (IOException e) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "파일 다운로드 중 오류가 발생했습니다." + e.getMessage());
        }
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
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("file no: %d", fileNo)));
        try {
            String bucketName = getBucketName(uploadedFile.getStoragePathNm());
            String filePath = getFilePath(uploadedFile.getStoragePathNm());

            uploadedFile.softDelete();

            objectStorageService.deleteFile(bucketName, filePath);
        } catch (Exception e) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "파일 삭제 중 오류가 발생했습니다." + e.getMessage());
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
                throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "파일 삭제 중 오류가 발생했습니다." + e.getMessage());
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
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("file no: %d", fileNo)));

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
                throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "파일 수정 중 오류가 발생했습니다." + e.getMessage());
            }
        } else {
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
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("file no: %d", fileNo)));
        try {
            String bucketName = getBucketName(uploadedFile.getStoragePathNm());
            String filePath = getFilePath(uploadedFile.getStoragePathNm());

            return objectStorageService.getUrl(bucketName, filePath);
        } catch (Exception e) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "파일 URL 생성 중 오류가 발생했습니다." + e.getMessage());
        }
    }

    public String getUrl(UploadedFile uploadedFile) {
        if (uploadedFile == null) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "업로드된 파일이 없습니다.");
        }

        try {
            String bucketName = getBucketName(uploadedFile.getStoragePathNm());
            String filePath = getFilePath(uploadedFile.getStoragePathNm());

            return objectStorageService.getUrl(bucketName, filePath);
        } catch (Exception e) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "파일 URL 생성 중 오류가 발생했습니다." + e.getMessage());
        }
    }

    public String getSecureUrl(Long fileNo) {
        UploadedFile uploadedFile = uploadedFileRepository.findByFileNo(fileNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("file no: %d", fileNo)));
        try {
            String bucketName = getBucketName(uploadedFile.getStoragePathNm());
            String filePath = getFilePath(uploadedFile.getStoragePathNm());

            return PathUtil.IncludeTrailingPathDelimiter(serverDomain) + PathUtil.IncludeTrailingPathDelimiter("images")
                    + PathUtil.IncludeTrailingPathDelimiter(bucketName) + filePath;
        } catch (Exception e) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "파일 URL 생성 중 오류가 발생했습니다." + e.getMessage());
        }
    }

    public String getSecureUrl(UploadedFile uploadedFile) {
        if (uploadedFile == null) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "업로드된 파일이 없습니다.");
        }

        try {
            String bucketName = getBucketName(uploadedFile.getStoragePathNm());
            String filePath = getFilePath(uploadedFile.getStoragePathNm());

            return PathUtil.IncludeTrailingPathDelimiter(serverDomain) + PathUtil.IncludeTrailingPathDelimiter("images")
                    + PathUtil.IncludeTrailingPathDelimiter(bucketName) + filePath;

        } catch (Exception e) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "파일 URL 생성 중 오류가 발생했습니다." + e.getMessage());
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
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "파일 URL 생성 중 오류가 발생했습니다." + e.getMessage());
        }
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
}
