package com.sk.domain.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.response.file.FileDownloadRes;
import com.sk.domain.admin.dto.response.file.FileUploadRes;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
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

    /**
    * 전체 파일를 조회한다.
    *
    * @return List<String>
    */
    // public List<String> getAll() {
    // return adminObjectStorageService.getAll();
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
    * @param file 단일 파일 업로드
    * @return FileUploadRes
    */
    @Transactional
    public FileUploadRes uploadFile(MultipartFile file, boolean isPublic) {
        if (file == null) {
            throw AppErrorException.of(ResultCode.Error.FILE_INVALID);
        }

        if (file.getSize() <= 0) {
            throw AppErrorException.of(ResultCode.Error.FILE_SIZE_ERROR);
        }

        try {
            String originalFileNm = file.getOriginalFilename();
            String fileExtNm = originalFileNm.substring(originalFileNm.lastIndexOf('.') + 1).toLowerCase();
            long fileSize = file.getSize();
            String mimeTypeNm = file.getContentType();

            // Object Storage 저장
            String storagePathNm = "TEMP";
            String storageFileNm = UUID.randomUUID().toString().concat(getFileExtension(file.getOriginalFilename()));
            String fileName = Paths.get(storagePathNm, storageFileNm).toString();

            // todo-companyCd
            String fileUrl = "";// adminObjectStorageService.uploadFile(file, fileName, isPublic);

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
    public FileDownloadRes downloadFile(Long fileNo) {
        UploadedFile uploadedFile = uploadedFileRepository.findByFileNo(fileNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("file no: %d", fileNo)));

        try {
            String fileName = Paths.get(uploadedFile.getStoragePathNm(), uploadedFile.getStorageFileNm()).toString();

            // todo-companyCd
            byte[] fileData = null; // adminObjectStorageService.downloadFile(fileName);

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

        String fileName = Paths.get(uploadedFile.getStoragePathNm(), uploadedFile.getStorageFileNm()).toString();

        // todo-companyCd
        // adminObjectStorageService.deleteFile(fileName);

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
    public void deleteFiles(Integer fileRelationshipObjectDistinguishNo, String fileRelationshipObjectSeparateCode) {
        List<UploadedFile> fileList = uploadedFileRepository.findByRelationshipObjectInfo(
                fileRelationshipObjectDistinguishNo,
                fileRelationshipObjectSeparateCode);

        // todo-companyCd
        // fileList.forEach(f -> adminObjectStorageService.deleteFile(Paths.get(f.getStoragePathNm(), f.getStorageFileNm()).toString()));
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
            Long fileNo,
            Integer fileRelationshipObjectDistinguishNo,
            String fileRelationshipObjectSeparateCode,
            String fileValidEndDt) {
        UploadedFile uploadedFile = uploadedFileRepository.findByFileNo(fileNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("file no: %d", fileNo)));

        String oldPath = Paths.get(uploadedFile.getStoragePathNm(), uploadedFile.getStorageFileNm()).toString();
        String newPath = Paths.get(fileRelationshipObjectSeparateCode, uploadedFile.getStorageFileNm()).toString();

        // todo-companyCd
        // adminObjectStorageService.moveFile(oldPath, newPath);

        uploadedFile.update(fileRelationshipObjectDistinguishNo, fileRelationshipObjectSeparateCode, fileValidEndDt);

        return uploadedFile;
    }

    /**
    * 파일에 대한 URL을 생성한다.
    *
    * @param fileNo 파일번호
    * @return URL
    */
    public String getPresignedUrl(Long fileNo) {
        UploadedFile uploadedFile = uploadedFileRepository.findByFileNo(fileNo)
                .orElseThrow(
                        () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("file no: %d", fileNo)));

        String fileName = Paths.get(uploadedFile.getStoragePathNm(), uploadedFile.getStorageFileNm()).toString();
        // todo-companyCd
        // return adminObjectStorageService.getPresignedUrl(fileName);
        return null;
    }

    private String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf("."));
    }
}
