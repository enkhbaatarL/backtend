package com.sk.domain.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
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

  @Value("${ncp.storage.bucketName}")
  private String bucketName;

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
  public List<UploadedFile> getFiles(
      Integer fileRelationshipObjectDistinguishNo,
      String fileRelationshipObjectSeparateCode) {
    return uploadedFileRepository
        .findByRelationshipObjectInfo(fileRelationshipObjectDistinguishNo, fileRelationshipObjectSeparateCode);
  }

  /**
    * 파일 정보를 수정한다.
    *
    * @param fileNo 파일번호
    * @param fileRelationshipObjectDistinguishNo 파일관계객체식별번호
    * @param fileRelationshipObjectSeparateCode 파일관계객체구분코드
    * @return UploadedFile
    */
  public UploadedFile updateFile(
      Long fileNo,
      Integer fileRelationshipObjectDistinguishNo,
      String fileRelationshipObjectSeparateCode) {
    UploadedFile uploadedFile = uploadedFileRepository.findByFileNo(fileNo)
        .orElseThrow(
            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("file no: %d", fileNo)));

    uploadedFile.update(fileRelationshipObjectDistinguishNo, fileRelationshipObjectSeparateCode);

    return uploadedFile;
  }

  /**
    * 파일을 업로드 한다.
    *
    * @param file 단일 파일 업로드
    * @param fileRelationshipObjectDistinguishNo 파일관계객체식별번호
    * @param fileRelationshipObjectSeparateCode 파일관계객체구분코드
    * @param fileValidEndDt
    * @return FileUploadRes
    */
  @Transactional
  public FileUploadRes uploadFile(
      MultipartFile file,
      Integer fileRelationshipObjectDistinguishNo,
      String fileRelationshipObjectSeparateCode,
      String fileValidEndDt) {

    try {
      String originalFileNm = file.getOriginalFilename();
      String fileExtNm = originalFileNm.substring(originalFileNm.lastIndexOf('.') + 1).toLowerCase();
      long fileSize = file.getSize();
      String mimeTypeNm = file.getContentType();

      //
      String storagePathNm = bucketName;
      String storageFileNm = adminObjectStorageService.uploadFile(storagePathNm, fileRelationshipObjectSeparateCode, file);
      String fileUrl = adminObjectStorageService.getPresignedUrl(storagePathNm, storageFileNm);

      //
      UploadedFile savedUploadedFile = uploadedFileRepository.save(UploadedFile.of(
          originalFileNm,
          storageFileNm,
          storagePathNm,
          fileSize,
          fileExtNm,
          mimeTypeNm,
          fileValidEndDt,
          fileRelationshipObjectDistinguishNo,
          fileRelationshipObjectSeparateCode.toString()));

      return FileUploadRes.of(savedUploadedFile.getFileNo(), file.getOriginalFilename(), file.getSize(), fileUrl);
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
      byte[] fileData = adminObjectStorageService.downloadFile(uploadedFile.getStoragePathNm(), uploadedFile.getStorageFileNm());

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

    adminObjectStorageService.deleteFile(uploadedFile.getStoragePathNm(), uploadedFile.getStorageFileNm());

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
    List<UploadedFile> uploadedFile = uploadedFileRepository.findByRelationshipObjectInfo(
        fileRelationshipObjectDistinguishNo,
        fileRelationshipObjectSeparateCode);

    uploadedFile.forEach(f -> adminObjectStorageService.deleteFile(f.getStoragePathNm(), f.getStorageFileNm()));
    uploadedFile.forEach(f -> f.softDelete());
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

    return adminObjectStorageService.getPresignedUrl(uploadedFile.getStoragePathNm(), uploadedFile.getStorageFileNm());
  }

}
