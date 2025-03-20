package com.sk.domain.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

  public FileSaveRes saveFile(MultipartFile file) {
    // if (file != null) {
    // String fileName = file.getOriginalFilename();
    // if (fileName == null || fileName.lastIndexOf('.') == -1) {
    // throw AppErrorException.of(ResultCode.Error.REQ_INVALID);
    // }

    // String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    // if (!ALLOWED_EXTENSIONS.contains(extension)) {
    // throw AppErrorException.of(ResultCode.Error.REQ_NOT_ALLOWED_FILE_FORMAT);
    // }

    // if (file.getSize() > MAX_SIZE) {
    // throw AppErrorException.of(ResultCode.Error.REQ_NOT_ALLOWED_FILE_SIZE);
    // }

    // // todo: 어딘가에 파일 저장
    // String storagePathNm = "어딘가에 파일 저장";

    // UploadedFileRepository.save(UploadedFile.of(file.getOriginalFilename(), storagePathNm, file.getSize(), extension, file.getContentType(),
    // "99991231", savedBulletinBoard.getBulletinNo(), "BBS"));
    // }

    return FileSaveRes.builder().build();
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
