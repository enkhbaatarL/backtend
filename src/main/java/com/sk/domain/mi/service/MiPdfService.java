package com.sk.domain.mi.service;

import com.sk.common.utils.PDFUtil;
import com.sk.domain.admin.service.AdminObjectStorageService;
import com.sk.domain.mi.dto.response.PdfMetadataRes;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.stream.IntStream;

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

    /**
     * PDF 파일을 받아 각 페이지를 이미지로 변환하여 S3에 업로드하는 메소드
     *
     * @param pdfFile PDF 파일
     */
    public void uploadPageImagesFromPDF(MultipartFile pdfFile, String uuidFileName) {
        try {

            byte[] pdfBytes = pdfFile.getBytes();
            PDDocument document = Loader.loadPDF(pdfBytes);
            PDFRenderer renderer = new PDFRenderer(document);

            String imageFolderPath = "GUDAT/%s/".formatted(uuidFileName);

            int pageCount = document.getNumberOfPages();

            IntStream.range(0, pageCount)
                .forEach(pageIndex -> {
                    try {
                        // 페이지를 이미지로 렌더링 (DPI 300으로 설정)
                        BufferedImage image = renderer.renderImageWithDPI(pageIndex, 300);

                        String imageName = String.format("page_%03d.png", pageIndex + 1);
                        byte[] imageBytes = PDFUtil.imageToByteArray(image, "png");

                        try (ByteArrayInputStream imageInputStream = new ByteArrayInputStream(imageBytes)) {
                            adminObjectStorageService.uploadImageTemp(imageFolderPath + imageName, imageBytes.length, imageInputStream);
                        }

                    } catch (IOException e) {
                        throw new RuntimeException("페이지 " + (pageIndex + 1) + " 처리 중 오류 발생", e);
                    }
                });

            document.close();

        } catch (IOException e) {
            throw new RuntimeException("PDF 파일을 이미지로 변환하는 중 오류가 발생했습니다.", e);
        }

    }

}
