package com.sk.domain.shared.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import com.sk.common.utils.PdfUtil;
import com.sk.common.utils.StringUtil;
import com.sk.domain.admin.service.AdminObjectStorageService;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.entity.master.tif.UserInfo;
import com.sk.domain.shared.entity.master.tmd.GoodsCriticalValueSetting;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.Extension;
import com.sk.domain.shared.enums.FileRelationshipObjectSeparateCode;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import com.sk.domain.shared.repository.master.tif.UserInfoRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsCriticalValueSettingRepository;
import com.sk.domain.shared.repository.master.tmd.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.util.StringUtils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 스케쥴러용 서비스
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SchedulerService {

    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final AdminObjectStorageService adminObjectStorageService;
    private final GoodsCriticalValueSettingRepository goodsCriticalValueSettingRepository;

    /**
     * GDI 탈퇴 유저 soft delete 처리
     */
    @Transactional
    public Boolean processGdiUserSoftDelete() {
        // 인터페이스 테이블에서 매번 삭제된 유저 조회 하는데 User 테이블에서 삭제된 유저 상관 없이 조회 하기 때문에 비효율적
        List<UserInfo> deletedUserInfos = userInfoRepository.findByIsDeletedTrue();

        if (deletedUserInfos.isEmpty()) {
            return true;
        }

        List<User> users = userRepository.findByUserInfosAndIsDeletedFalse(deletedUserInfos);

        if (!users.isEmpty()) {
            users.stream()
                .peek(User::softDelete)
                .collect(Collectors.toList());
        }

        return true;
    }

    /**
     * 임계치를 확인하고 알림을 발송한다.
     *
     * @return 알림 발송 결과
     */
    @Transactional
    public Boolean createNotification() {
        List<GoodsCriticalValueSetting> goodsCriticalValueSettings = goodsCriticalValueSettingRepository
            .findAllByIsDeletedFalse();

        for (GoodsCriticalValueSetting goodsCriticalValueSettingData : goodsCriticalValueSettings) {
            // TODO: 호영: 임계치에 따른 이메일 발송 로직 추가, 성공한 메일, 실패한 메일 나눠서 Response 작성

            String goodsDataSourceNm = goodsCriticalValueSettingData.getGoodsDataSourceDetail().getGoodsDataSourceNm();
            String targetDbNm = goodsCriticalValueSettingData.getGoodsDataSourceDetail().getTargetDbNm();
            String targetTableNm = goodsCriticalValueSettingData.getGoodsDataSourceDetail().getTargetTableNm();
            String queryConditionCntt = goodsCriticalValueSettingData.getGoodsDataSourceDetail().getQueryConditionCntt();


            String querySelect;
            String queryFrom;
            String queryWhere;
            String queryOrderBy;

            




            // Integer goodsDataSourceNo = goodsCriticalValueSetting.getGoodsDataSource().getGoodsDataSourceNo();
            // String goodsDataSourceNm = goodsCriticalValueSetting.getGoodsDataSource().getGoodsDataSourceNm();
            // Goods goods = goodsCriticalValueSetting.getGoodsDataSource().getGoods();
            // String targetDbNm = goodsCriticalValueSetting.getGoodsDataSource().getTargetDbNm();
            // String targetTableNm = goodsCriticalValueSetting.getGoodsDataSource().getTargetTableNm();
            // String queryConditionContent = goodsCriticalValueSetting.getGoodsDataSource().getQueryConditionContent();

            // mailService.sendMail(goodsCriticalValueSetting.getUser().getEmail(), "알림", "알림");
        }

        return true;
    }

    /**
     * PDF 파일을 이미지로 변환하여 S3에 업로드 한다.
     *
     * @return Boolean
     */
    @Transactional
    public Boolean processPdfToImages() {

        log.info("[PDF-TO-IMAGES] Start processing target PDFs...");

        // 타켓 비정형데이터 조회
        List<UploadedFile> uploadedFiles = uploadedFileRepository.findPdfToImagesTarget(Extension.PDF.getCode(), FileRelationshipObjectSeparateCode.GUDAT.getCode());

        // 테스트 todo 지울 것.
        List<UploadedFile> testFiles = uploadedFiles.stream().filter(s -> s.getFileNo() == 4818).toList();

        log.info("[PDF-TO-IMAGES] Total target PDF files: {}", testFiles.size());

        testFiles.forEach(uploadedFile -> {

            String storagePathNm = uploadedFile.getStoragePathNm();
            Long fileNo = uploadedFile.getFileNo();

            log.info("[PDF-TO-IMAGES] Processing fileNo: {}", fileNo);

            Pair<String, String> bucketInfo = StringUtil.extractBucketAndPath(storagePathNm);
            String bucket = bucketInfo.getLeft();
            String fullPath = bucketInfo.getRight();
            String prefix = StringUtil.stripFileExtension(fullPath);

            log.info("[PDF-TO-IMAGES] -> Bucket: {}, Path: {}", bucket, fullPath);

            try {

                byte[] pdfBytes = adminObjectStorageService.downloadFile(bucketInfo.getLeft(), bucketInfo.getRight());
                PDDocument document = Loader.loadPDF(pdfBytes);
                PDFRenderer renderer = new PDFRenderer(document);
                int numberOfPages = document.getNumberOfPages();

                log.info("[PDF-TO-IMAGES] -> Loaded PDF ({} pages)", numberOfPages);

                IntStream.range(0, numberOfPages)
                    .forEach(pageIndex -> {
                        try {

                            int pageNo = pageIndex + 1;
                            log.info("[PDF-TO-IMAGES] -> Rendering page: {}", pageNo);

                            // 페이지를 이미지로 렌더링 (DPI 300으로 설정)
                            BufferedImage image = renderer.renderImageWithDPI(pageIndex, 300);
                            String imageName = StringUtils.concat(StringUtil.formatPageNumber(pageNo), ".", Extension.PNG.getCode());
                            String imagePath = StringUtils.concat(prefix, "/", imageName);

                            byte[] imageBytes = PdfUtil.imageToByteArray(image, Extension.PNG.getCode());

                            try (ByteArrayInputStream imageInputStream = new ByteArrayInputStream(imageBytes)) {
                                adminObjectStorageService.uploadFile(imageInputStream, bucket, imagePath, false, "image/png", imageBytes.length);
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

            } catch (IOException e) {
                log.error("[PDF-TO-IMAGES] Failed to process fileNo: {}", fileNo, e);
                throw SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR);
            }

        });

        log.info("[PDF-TO-IMAGES] All files processed successfully.");
        return Boolean.TRUE;
    }

}
