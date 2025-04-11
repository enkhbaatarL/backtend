package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.SystemErrorException;
import com.sk.common.utils.PdfUtil;
import com.sk.common.utils.StringUtil;
import com.sk.domain.admin.service.AdminObjectStorageService;
import com.sk.domain.mi.dto.response.PdfMetadataRes;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.enums.Extension;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * @param page 페이지
     * @return byte[]
     */
    public byte[] getImagePageFromPdf(Long fileNo, int page) {

        UploadedFile uploadedFile = uploadedFileRepository.findByFileNoAndIsDeletedFalse(fileNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [uploaded-file]. fileNo: %s".formatted(fileNo)));

        if (!Extension.PDF.getCode().equalsIgnoreCase(uploadedFile.getFileExtNm())) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "The fileNo is not a PDF file. fileNo: %s".formatted(fileNo));
        }

        if (!uploadedFile.getIsImageConversionCompleted()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "The PDF image conversion has not yet been completed. fileNo: %s".formatted(fileNo));
        }

        try {
            byte[] bytes = adminObjectStorageService.downloadFile("skt-mi-d-osb-01", "GUDAT/353f20b7-32cd-44fa-afbc-b52881fe84b3/%s.png".formatted(StringUtil.formatPageNumber(page)));
            BufferedImage bufferedImage = PdfUtil.byteArrayToImage(bytes);
            BufferedImage addTextWatermark = PdfUtil.addTextWatermark(bufferedImage, "T E S T - W A T E R M A R K");
            return PdfUtil.imageToByteArray(addTextWatermark, Extension.PNG.getCode());
        } catch (Exception e) {
            log.error("error [getImagePageFromPdf]: {}", e.getMessage(), e);
            throw SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR);
        }

    }

//    /**
//     * PDF 파일을 받아 각 페이지를 이미지로 변환하여 S3에 업로드하는 메소드
//     *
//     * @param pdfFile PDF 파일
//     */
//    public void uploadPageImagesFromPDF(MultipartFile pdfFile, String uuidFileName) {
//        try {
//
//            byte[] pdfBytes = pdfFile.getBytes();
//            PDDocument document = Loader.loadPDF(pdfBytes);
//            PDFRenderer renderer = new PDFRenderer(document);
//
//            String imageFolderPath = "GUDAT/%s/".formatted(uuidFileName);
//
//            int pageCount = document.getNumberOfPages();
//
//            IntStream.range(0, pageCount)
//                .forEach(pageIndex -> {
//                    try {
//                        // 페이지를 이미지로 렌더링 (DPI 300으로 설정)
//                        BufferedImage image = renderer.renderImageWithDPI(pageIndex, 300);
//
//                        String imageName = String.format("page_%03d.png", pageIndex + 1);
//                        byte[] imageBytes = PdfUtil.imageToByteArray(image, "png");
//
//                        try (ByteArrayInputStream imageInputStream = new ByteArrayInputStream(imageBytes)) {
////                            adminObjectStorageService.uploadImageTemp(imageFolderPath + imageName, imageBytes.length, imageInputStream);
//                        }
//
//                    } catch (IOException e) {
//                        throw new RuntimeException("페이지 " + (pageIndex + 1) + " 처리 중 오류 발생", e);
//                    }
//                });
//
//            document.close();
//
//        } catch (IOException e) {
//            throw new RuntimeException("PDF 파일을 이미지로 변환하는 중 오류가 발생했습니다.", e);
//        }
//
//    }

}
