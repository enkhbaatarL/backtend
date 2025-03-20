package com.sk.domain.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import com.amazonaws.services.s3.AmazonS3;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.admin.dto.response.file.FileSaveRes;
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
  private final AmazonS3 objectStorageClient;

  @Value("${ncp.storage.bucketName}")
  private String bucketName;

  public Integer uploadFile(MultipartFile file) {

    return 1;
  }

  public UploadedFile getFile(Long fileNo) {
    return uploadedFileRepository.findByFileNo(fileNo)
        .orElseThrow(
            () -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, String.format("file id: %d", fileNo)));
  }

  public void deleteFile(Long fileNo) {
    // todo
  }

  public void deleteFile(Integer fileRelationshipObjectDistinguishNo) {
    // todo
  }
}
