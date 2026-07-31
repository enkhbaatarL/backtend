package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.SystemErrorException;
import com.sk.common.utils.DateUtil;
import com.sk.common.utils.PdfUtil;
import com.sk.common.utils.StringUtil;
import com.sk.domain.mi.dto.response.PdfMetadataRes;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.Extension;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import com.sk.domain.shared.service.ObjectStorageService;

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
    private final ObjectStorageService objectStorageService;

    /**
     * 파일 메타데이터를 반환한다.
     *
     * @param fileNo 파일 번호
     * @return PdfMetadataRes
     */
    public PdfMetadataRes getPdfFileMetadata(Integer fileNo) {

        UploadedFile uploadedFile = uploadedFileRepository.findByFileNoAndIsDeletedFalse(fileNo.longValue())
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                        "It doesn't exist in the [uploaded-file]. fileNo: %s".formatted(fileNo)));

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
    public byte[] getImagePageFromPdf(Integer fileNo, int page, User user) {

        UploadedFile uploadedFile = uploadedFileRepository.findByFileNoAndIsDeletedFalse(fileNo.longValue())
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
            String imageName = StringUtils.concat(StringUtil.formatPageNumber(page), ".", Extension.JPEG.getCode());
            String imagePath = StringUtils.concat(prefix, "/", imageName);

            byte[] bytes = objectStorageService.downloadFile(bucket, imagePath);
            BufferedImage bufferedImage = PdfUtil.byteArrayToImage(bytes);

            // 26.07.29 취약점 진단 수정, '표준 라이브러리의 널 반환 값 역참조-102187' -> ImageIO.read는 미인식 포맷에서 예외 대신 null 반환
            if (bufferedImage == null) {
                throw SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "이미지 변환에 실패했습니다. imagePath: %s".formatted(imagePath));
            }

            // 회사명#사번#날짜
            String waterMark = StringUtil.joinPoundSignNonNull(user.getCustomerCompany().getCustomerCompanyNm(), user.getUserLoginId(), DateUtil.currentYmdHyphen());
            BufferedImage addTextWatermark = PdfUtil.addTextWatermarkRGB(bufferedImage, waterMark);
//            BufferedImage result = PdfUtil.resize(addTextWatermark, 0.75f);

            return PdfUtil.imageToJpegByteArray(addTextWatermark, 1.0f);
        } catch (Exception e) {
            log.error("Failed to get the image page from the PDF file. fileNo: {}, page: {}", fileNo, page, e);
            throw SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR);
        }

    }

}
