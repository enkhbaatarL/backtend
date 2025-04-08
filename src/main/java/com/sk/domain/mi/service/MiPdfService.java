package com.sk.domain.mi.service;

import com.sk.domain.admin.service.AdminObjectStorageService;
import com.sk.domain.mi.dto.response.PdfMetadataRes;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiPdfService {

    private final UploadedFileRepository uploadedFileRepository;
    private final AdminObjectStorageService adminObjectStorageService;

    // 1. pdf 파일 fileNo 유효성 체크
    // 2. pdf 파일에 매핑된 이미지 폴더 내 이미지 파일 존재 체크 -> 빈 배열
    // 3. pdf 이미지 파일 갯수 확인
    // 4. pdf 파일에 대한 메타데이터 반환(UploadedFile)
    public PdfMetadataRes getPdfMetadata(Integer fileNo) {
        return null;
    }

    public Object getSliceImage(Integer fileNo, Integer sliceNo) {
        return null;
    }

}
