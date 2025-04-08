package com.sk.domain.admin.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

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

    @Value("${ncp.storage.bucketName}")
    private String commBucketName;

    @Value("${ncp.storage.prefix}")
    private String prefix;

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
    * @param file 단일 파일 업로드
    * @return FileUploadRes
    */
    @Transactional
    public FileUploadRes uploadFile(User user, MultipartFile file, boolean isPublic) {
        if (file == null) {
            throw AppErrorException.of(ResultCode.Error.FILE_INVALID);
        }

        if (file.getSize() <= 0) {
            throw AppErrorException.of(ResultCode.Error.FILE_SIZE_ERROR);
        }

        try {
            String customerCompanyCd = user.getCustomerCompany().getCustomerCompanyCd();
            String originalFileNm = file.getOriginalFilename();
            String fileExtNm = originalFileNm.substring(originalFileNm.lastIndexOf('.') + 1).toLowerCase();
            long fileSize = file.getSize();
            String mimeTypeNm = file.getContentType();

            // Object Storage 저장
            String bucketName = RoleUtil.isAdmin(user) ? commBucketName : getBucketName(customerCompanyCd);
            String storagePathNm = FileRelationshipObjectSeparateCode.TEMP.getCode();
            String storageFileNm = UUID.randomUUID().toString().concat(getFileExtension(file.getOriginalFilename()));
            String fileName = PathUtil.IncludeTrailingPathDelimiter(storagePathNm) + storageFileNm;
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
                    0,
                    storagePathNm));

            return FileUploadRes.of(savedUploadedFile.getFileNo(), originalFileNm, fileSize, fileUrl);
        } catch (Exception e) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR);
        }
    }

    /**
    * 파일을 다운로드 한다.
    *
    * @param fileNo 파일번호
    * @return FileDownloadRes
    */
    public FileDownloadRes downloadFile(User user, Long fileNo) {
        UploadedFile uploadedFile = uploadedFileRepository.findByFileNo(fileNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("file no: %d", fileNo)));

        try {
            String customerCompanyCd = user.getCustomerCompany().getCustomerCompanyCd();
            String bucketName = RoleUtil.isAdmin(user) ? commBucketName : getBucketName(customerCompanyCd);
            String fileName = PathUtil.IncludeTrailingPathDelimiter(uploadedFile.getStoragePathNm()) + uploadedFile.getStorageFileNm();
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
    public void deleteFile(User user, Long fileNo) {
        UploadedFile uploadedFile = uploadedFileRepository.findByFileNo(fileNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("file no: %d", fileNo)));

        String customerCompanyCd = user.getCustomerCompany().getCustomerCompanyCd();
        String bucketName = RoleUtil.isAdmin(user) ? commBucketName : getBucketName(customerCompanyCd);
        String fileName = PathUtil.IncludeTrailingPathDelimiter(uploadedFile.getStoragePathNm()) + uploadedFile.getStorageFileNm();

        adminObjectStorageService.deleteFile(bucketName, fileName);

        uploadedFile.softDelete();
    }

    /**
    * 파일들을 삭제 한다.
    *
    * @param fileRelationshipObjectDistinguishNo 파일관계객체식별번호
    * @param fileRelationshipObjectSeparateCode 파일관계객체구분코드
    * @return 
    */
    @Transactional
    public void deleteFiles(User user, Integer fileRelationshipObjectDistinguishNo, String fileRelationshipObjectSeparateCode) {
        List<UploadedFile> fileList = uploadedFileRepository.findByRelationshipObjectInfo(
                fileRelationshipObjectDistinguishNo,
                fileRelationshipObjectSeparateCode);

        String customerCompanyCd = user.getCustomerCompany().getCustomerCompanyCd();
        String bucketName = RoleUtil.isAdmin(user) ? commBucketName : getBucketName(customerCompanyCd);
        fileList.forEach(f -> adminObjectStorageService.deleteFile(bucketName,
                PathUtil.IncludeTrailingPathDelimiter(f.getStoragePathNm()) + f.getStorageFileNm()));
        fileList.forEach(f -> f.softDelete());
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
            User user,
            Long fileNo,
            Integer fileRelationshipObjectDistinguishNo,
            String fileRelationshipObjectSeparateCode,
            String fileValidEndDt) {
        UploadedFile uploadedFile = uploadedFileRepository.findByFileNo(fileNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("file no: %d", fileNo)));

        String customerCompanyCd = user.getCustomerCompany().getCustomerCompanyCd();
        String bucketName = RoleUtil.isAdmin(user) ? commBucketName : getBucketName(customerCompanyCd);
        String oldPath = PathUtil.IncludeTrailingPathDelimiter(uploadedFile.getStoragePathNm()) + uploadedFile.getStorageFileNm();
        String newPath = PathUtil.IncludeTrailingPathDelimiter(fileRelationshipObjectSeparateCode) + uploadedFile.getStorageFileNm();

        adminObjectStorageService.moveFile(bucketName, oldPath, newPath);

        uploadedFile.update(fileRelationshipObjectDistinguishNo, fileRelationshipObjectSeparateCode, fileValidEndDt);

        return uploadedFile;
    }

    /**
    * 파일에 대한 URL을 생성한다.
    *
    * @param fileNo 파일번호
    * @return URL
    */
    // public String getPresignedUrl(User user, Long fileNo) {
    // UploadedFile uploadedFile = uploadedFileRepository.findByFileNo(fileNo)
    // .orElseThrow(
    // () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("file no: %d", fileNo)));

    // String customerCompanyCd = user.getCustomerCompany().getCustomerCompanyCd();
    // String bucketName = RoleUtil.isAdmin(user) ? commBucketName : getBucketName(customerCompanyCd);
    // String fileName = Paths.get(uploadedFile.getStoragePathNm(), uploadedFile.getStorageFileNm()).toString();

    // return adminObjectStorageService.getPresignedUrl(bucketName, fileName);
    // }
    public String getUrl(User user, Long fileNo) {
        UploadedFile uploadedFile = uploadedFileRepository.findByFileNo(fileNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("file no: %d", fileNo)));

        String customerCompanyCd = user.getCustomerCompany().getCustomerCompanyCd();
        String bucketName = RoleUtil.isAdmin(user) ? commBucketName : getBucketName(customerCompanyCd);
        String fileName = PathUtil.IncludeTrailingPathDelimiter(uploadedFile.getStoragePathNm()) + uploadedFile.getStorageFileNm();

        return adminObjectStorageService.getUrl(bucketName, fileName);
    }

    private String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf("."));
    }

    private String getBucketName(String customerCompanyCd) {
        return prefix + customerCompanyCd;
    }
}
