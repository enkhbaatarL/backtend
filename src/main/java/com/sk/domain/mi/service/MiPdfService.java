package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.SystemErrorException;
import com.sk.common.utils.DateUtil;
import com.sk.common.utils.PdfUtil;
import com.sk.common.utils.StringUtil;
import com.sk.domain.admin.service.AdminObjectStorageService;
import com.sk.domain.mi.dto.response.PdfMetadataRes;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.Extension;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.util.StringUtils;

import java.awt.image.BufferedImage;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MiPdfService {

    private final UploadedFileRepository uploadedFileRepository;
    private final AdminObjectStorageService adminObjectStorageService;

    /**
     * 파일 메타데이터를 반환한다.
     *
     * @param fileNo 파일 번호
     * @return PdfMetadataRes
     */
    public PdfMetadataRes getPdfFileMetadata(Long fileNo) {

        UploadedFile uploadedFile = uploadedFileRepository.findByFileNoAndIsDeletedFalse(fileNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [uploaded-file]. fileNo: %s".formatted(fileNo)));

        if (!Extension.PDF.getCode().equalsIgnoreCase(uploadedFile.getFileExtNm())) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "The fileNo is not a PDF file. fileNo: %s".formatted(fileNo));
        }

        return PdfMetadataRes.of(uploadedFile);
    }

    /**
     * PDF 파일 이미지를 조회한다.
     *
     * @param fileNo 파일 번호
     * @param page   페이지
     * @param user   사용자
     * @return byte[]
     */
    public byte[] getImagePageFromPdf(Long fileNo, int page, User user) {

        UploadedFile uploadedFile = uploadedFileRepository.findByFileNoAndIsDeletedFalse(fileNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [uploaded-file]. fileNo: %s".formatted(fileNo)));

        if (page > uploadedFile.getFilePageCnt()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "The page is out of range. fileNo: %s, page: %s".formatted(fileNo, page));
        }

        if (!Extension.PDF.getCode().equalsIgnoreCase(uploadedFile.getFileExtNm())) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "The fileNo is not a PDF file. fileNo: %s".formatted(fileNo));
        }

        if (!uploadedFile.getIsImageConversionCompleted()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "The PDF image conversion has not yet been completed. fileNo: %s".formatted(fileNo));
        }

        try {

            Pair<String, String> bucketInfo = StringUtil.extractBucketAndPath(uploadedFile.getStoragePathNm());
            String bucket = bucketInfo.getLeft();
            String fullPath = bucketInfo.getRight();
            String prefix = StringUtil.stripFileExtension(fullPath);
            String imageName = StringUtils.concat(StringUtil.formatPageNumber(page), ".", Extension.PNG.getCode());
            String imagePath = StringUtils.concat(prefix, "/", imageName);

            byte[] bytes = adminObjectStorageService.downloadFile(bucket, imagePath);
            BufferedImage bufferedImage = PdfUtil.byteArrayToImage(bytes);

            String waterMark = StringUtil.joinPoundSignNonNull(user.getUserLoginId(), user.getJobGradeNm(), user.getDepartmentNm(), user.getUserNm(), DateUtil.currentYmdHyphen());
            BufferedImage addTextWatermark = PdfUtil.addTextWatermark(bufferedImage, waterMark);

            return PdfUtil.imageToByteArray(addTextWatermark, Extension.PNG.getCode());
        } catch (Exception e) {
            throw SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR);
        }

    }

}
