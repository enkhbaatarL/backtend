package com.sk.domain.shared.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import com.sk.common.utils.PdfUtil;
import com.sk.common.utils.StringUtil;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.enums.Extension;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.util.StringUtils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class PdfToImagesProcessor {

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${features.pdf-viewer.dpi:300}")
    private int dpiValue;

    private final ObjectStorageService objectStorageService;
    private final StorageLocationParser storageLocationParser;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(UploadedFile uploadedFile) throws IOException {

        UploadedFile merged = entityManager.merge(uploadedFile);

        String storagePathNm = merged.getStoragePathNm();
        Long fileNo = merged.getFileNo();

        log.info("[PDF-TO-IMAGES] Processing fileNo: {}", fileNo);

        StorageLocation storageLocation = storageLocationParser.parse(storagePathNm);
        String prefix = StringUtil.stripFileExtension(storageLocation.key());

        log.info("[PDF-TO-IMAGES] -> Storage location validated. fileNo: {}", fileNo);

        byte[] pdfBytes = objectStorageService.downloadFile(
                storageLocation.bucket(),
                storageLocation.key());
        PDDocument document = Loader.loadPDF(pdfBytes);
        PDFRenderer renderer = new PDFRenderer(document);
        int numberOfPages = document.getNumberOfPages();

        log.info("[PDF-TO-IMAGES] -> Loaded PDF ({} pages)", numberOfPages);

        IntStream.range(0, numberOfPages)
            .forEach(pageIndex -> {
                try {

                    int pageNo = pageIndex + 1;
                    log.debug("[PDF-TO-IMAGES] -> Rendering page: {}", pageNo);

                    // 페이지를 이미지로 렌더링
                    BufferedImage image = renderer.renderImageWithDPI(pageIndex, dpiValue, ImageType.RGB);
                    String imageName = StringUtils.concat(StringUtil.formatPageNumber(pageNo), ".", Extension.JPEG.getCode());
                    String imagePath = StringUtils.concat(prefix, "/", imageName);

                    byte[] imageBytes = PdfUtil.imageToJpegByteArray(image, 0.85f);

                    try (ByteArrayInputStream imageInputStream = new ByteArrayInputStream(imageBytes)) {
                        objectStorageService.uploadFile(
                                imageInputStream,
                                storageLocation.bucket(),
                                imagePath,
                                false,
                                "image/jpeg",
                                imageBytes.length);
                        log.debug("[PDF-TO-IMAGES] -> Uploaded page {} as '{}'", pageNo, imagePath);
                    }

                } catch (IOException e) {
                    log.error("[PDF-TO-IMAGES] Failed to process page {}", pageIndex + 1, e);
                    throw SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR);
                }
            });

        document.close();
        log.info("[PDF-TO-IMAGES] -> upload Completed fileNo: {}", fileNo);

        // 완료상태, 페이지 수 업데이트
        merged.updateUploadedImagesResult(numberOfPages);

    }

}
