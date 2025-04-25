package com.sk.domain.admin.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.Normalizer;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.DateUtil;
import com.sk.common.utils.PathUtil;
import com.sk.common.utils.RoleUtil;
import com.sk.domain.admin.dto.response.file.FileDownloadRes;
import com.sk.domain.admin.dto.response.file.FileUploadRes;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.FileRelationshipObjectSeparateCode;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminFileService {
    private final UploadedFileRepository uploadedFileRepository;
    private final AdminObjectStorageService adminObjectStorageService;

    private final String TEMP_DIR = "TEMP";

    @Value("${ncp.storage.endPoint}")
    private String endPoint;

    @Value("${ncp.storage.bucketName}")
    private String commBucketName;

    @Value("${ncp.storage.prefix}")
    private String prefix;

    public void getAll() {
        adminObjectStorageService.getAll();
        return;
    }

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
    * @return FileUploadRes
    */
    @Transactional
    public FileUploadRes uploadFile(String companyCd, Role role, MultipartFile file, boolean isPublic) {
        if (file == null) {
            throw AppErrorException.of(ResultCode.Error.FILE_INVALID);
        }

        if (file.getSize() <= 0) {
            throw AppErrorException.of(ResultCode.Error.FILE_SIZE_ERROR);
        }

        try {
            String originalFileNm = Normalizer.normalize(file.getOriginalFilename(), Normalizer.Form.NFC);
            String fileExtNm = originalFileNm.substring(originalFileNm.lastIndexOf('.') + 1).toLowerCase();
            long fileSize = file.getSize();
            String mimeTypeNm = file.getContentType();

            String customerCompanyCd = companyCd.toLowerCase();
            String bucketName = RoleUtil.isAdmin(role) || RoleUtil.isSubAdmin(role) ? commBucketName : prefix + customerCompanyCd;
            String storageFileNm = UUID.randomUUID().toString().concat(getFileExtension(file.getOriginalFilename()));
            String storagePathNm = PathUtil.IncludeTrailingPathDelimiter(endPoint) + PathUtil.IncludeTrailingPathDelimiter(bucketName)
                    + PathUtil.IncludeTrailingPathDelimiter(TEMP_DIR)
                    + storageFileNm;
            Integer fileRelationshipObjectDistinguishNo = 0;
            String fileRelationshipObjectSeparateCd = FileRelationshipObjectSeparateCode.TEMP.getCode();

            // Object Storage 저장
            String fileName = PathUtil.IncludeTrailingPathDelimiter(TEMP_DIR) + storageFileNm;
            String fileUrl = adminObjectStorageService.uploadFile(file, bucketName, fileName, isPublic);

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
                    fileRelationshipObjectSeparateCd));

            return FileUploadRes.of(savedUploadedFile.getFileNo(), originalFileNm, fileSize, fileUrl);
        } catch (Exception e) {
            log.error("Exception during upload: {}", e.getMessage(), e);
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR);
        }
    }

    /**
    * 파일을 다운로드 한다.
    *
    * @param fileNo 파일번호
    * @return FileDownloadRes
    */
    public FileDownloadRes downloadFile(Long fileNo) {
        UploadedFile uploadedFile = uploadedFileRepository.findByFileNo(fileNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("file no: %d", fileNo)));

        try {
            String bucketName = getBucketName(uploadedFile.getStoragePathNm());
            String fileName = getFileName(uploadedFile.getStoragePathNm());

            byte[] fileData = adminObjectStorageService.downloadFile(bucketName, fileName);

            return FileDownloadRes.of(uploadedFile.getFileNo(), uploadedFile.getOriginalFileNm(), uploadedFile.getFileSize(), fileData);
        } catch (Exception e) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR);
        }
    }

    /**
    * 파일을 삭제 한다.
    *
    * @param fileNo 파일번호
    * @return 
    */
    @Transactional
    public void deleteFile(Long fileNo) {
        UploadedFile uploadedFile = uploadedFileRepository.findByFileNo(fileNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("file no: %d", fileNo)));
        try {
            String bucketName = getBucketName(uploadedFile.getStoragePathNm());
            String fileName = getFileName(uploadedFile.getStoragePathNm());

            adminObjectStorageService.deleteFile(bucketName, fileName);

            uploadedFile.softDelete();
        } catch (Exception e) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR);
        }
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
                String fileName = getFileName(f.getStoragePathNm());

                adminObjectStorageService.deleteFile(bucketName, fileName);

                f.softDelete();
            } catch (Exception e) {
                throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR);
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

        try {
            String bucketName = getBucketName(uploadedFile.getStoragePathNm());
            String oldPath = getFileName(uploadedFile.getStoragePathNm());
            String newPath = oldPath.replaceFirst(TEMP_DIR, fileRelationshipObjectSeparateCode);
            String storagePathNm = PathUtil.IncludeTrailingPathDelimiter(endPoint) + PathUtil.IncludeTrailingPathDelimiter(bucketName) + newPath;

            adminObjectStorageService.moveFile(bucketName, oldPath, newPath);

            uploadedFile.update(storagePathNm, fileRelationshipObjectDistinguishNo, fileRelationshipObjectSeparateCode, fileValidEndDt);

            return uploadedFile;
        } catch (Exception e) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR);
        }
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
            String fileName = getFileName(uploadedFile.getStoragePathNm());

            return adminObjectStorageService.getUrl(bucketName, fileName);
        } catch (Exception e) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR);
        }
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
    private String getFileName(String path) throws MalformedURLException {
        String[] segments = extractPathSegments(path);
        if (segments.length < 3) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "URL에 파일 경로 정보가 포함되어 있지 않습니다.");
        }
        return String.join("/", Arrays.copyOfRange(segments, 2, segments.length));
    }
}
