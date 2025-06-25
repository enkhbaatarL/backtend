package com.sk.domain.shared.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import com.sk.common.utils.PdfUtil;
import com.sk.common.utils.StringUtil;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.enums.Extension;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
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

    @Value("${features.pdf-viewer.dpi:300}")
    private int dpiValue;

    private final ObjectStorageService objectStorageService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(UploadedFile uploadedFile) throws IOException {

        String storagePathNm = uploadedFile.getStoragePathNm();
        Long fileNo = uploadedFile.getFileNo();

        log.info("[PDF-TO-IMAGES] Processing fileNo: {}", fileNo);

        Pair<String, String> bucketInfo = StringUtil.extractBucketAndPath(storagePathNm);
        String bucket = bucketInfo.getLeft();
        String fullPath = bucketInfo.getRight();
        String prefix = StringUtil.stripFileExtension(fullPath);

        log.info("[PDF-TO-IMAGES] -> Bucket: {}, Path: {}", bucket, fullPath);

        byte[] pdfBytes = objectStorageService.downloadFile(bucketInfo.getLeft(), bucketInfo.getRight());
        PDDocument document = Loader.loadPDF(pdfBytes);
        PDFRenderer renderer = new PDFRenderer(document);
        int numberOfPages = document.getNumberOfPages();

        log.info("[PDF-TO-IMAGES] -> Loaded PDF ({} pages)", numberOfPages);

        IntStream.range(0, numberOfPages)
            .forEach(pageIndex -> {
                try {

                    int pageNo = pageIndex + 1;
                    log.info("[PDF-TO-IMAGES] -> Rendering page: {}", pageNo);

                    // 페이지를 이미지로 렌더링
                    BufferedImage image = renderer.renderImageWithDPI(pageIndex, dpiValue, ImageType.RGB);
                    String imageName = StringUtils.concat(StringUtil.formatPageNumber(pageNo), ".", Extension.JPEG.getCode());
                    String imagePath = StringUtils.concat(prefix, "/", imageName);

                    byte[] imageBytes = PdfUtil.imageToJpegByteArray(image, 0.7f);

                    try (ByteArrayInputStream imageInputStream = new ByteArrayInputStream(imageBytes)) {
                        objectStorageService.uploadFile(imageInputStream, bucket, imagePath, false, "image/jpeg", imageBytes.length);
                        log.info("[PDF-TO-IMAGES] -> Uploaded page {} as '{}'", pageNo, imagePath);
                    }

                } catch (IOException e) {
                    log.error("[PDF-TO-IMAGES] Failed to process page {}", pageIndex + 1, e);
                    throw SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR);
                }
            });

        document.close();
        log.info("[PDF-TO-IMAGES] -> upload Completed fileNo: {}", fileNo);

        // 완료상태, 페이지 수 업데이트
        uploadedFile.updateUploadedImagesResult(numberOfPages);

    }

}
