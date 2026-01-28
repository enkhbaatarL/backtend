package com.sk.domain.admin.listener.handler;

import com.sk.common.utils.MultipartFileUtil;
import com.sk.domain.admin.listener.event.AXUploadFileEvent;
import com.sk.domain.shared.client.AxPlatformApi;
import com.sk.domain.shared.client.dto.request.AXMetaRegisterReq;
import com.sk.domain.shared.client.dto.request.AXMetaRegisterReq.ModifiedFile;
import com.sk.domain.shared.client.dto.response.AXMetaUploadRes;
import com.sk.domain.shared.dto.request.file.FileDownloadReq;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.enums.AxpKnowledgeRegisterComponentType;
import com.sk.domain.shared.enums.FileRelationshipObjectSeparateCode;
import com.sk.domain.shared.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class FileAxpKnowledgeRegister implements AxpKnowledgeRegister {

    private final FileService fileService;
    private final AxPlatformApi axPlatformApi;

    @Override
    public AxpKnowledgeRegisterComponentType getType() {
        return AxpKnowledgeRegisterComponentType.FILE;
    }

    @Override
    public void process(AXUploadFileEvent event, String datasourceId, GoodsData goodsData) {
        log.info("[AX Platform] File upload process start for goodsDataNo: {}", goodsData.getGoodsDataNo());

        // 1. goodsData와 연관된 파일 목록 조회
        List<UploadedFile> files = fileService.getFiles(goodsData.getGoodsDataNo(), FileRelationshipObjectSeparateCode.GUDAT.getCode());

        if (files.isEmpty()) {
            log.info("[AX Platform] No files found for goodsDataNo: {}", goodsData.getGoodsDataNo());
            return;
        }

        AXMetaRegisterReq registerReq = new AXMetaRegisterReq();

        for (UploadedFile uploadedFile : files) {
            try {
                // 2. 파일 다운로드
                FileDownloadReq downloadReq = new FileDownloadReq(uploadedFile.getFileNo(), uploadedFile.getOriginalFileNm(), uploadedFile.getFileSize());
                var downloadRes = fileService.downloadFile(downloadReq);

                // 임시 파일 생성
                File tempFile = File.createTempFile("axp_", uploadedFile.getOriginalFileNm());
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    fos.write(downloadRes.fileData());
                }

                // 3. axPlatformApi.upload 로 업로드
                MultipartFileUtil multipartFile = new MultipartFileUtil("files", tempFile, null);
                AXMetaUploadRes uploadRes = axPlatformApi.upload(multipartFile);
                log.info("[AX Platform] uploaded file: {}, res: {}", uploadedFile.getOriginalFileNm(), uploadRes);

                // 결과 기억 (register 요청 준비)
                if (uploadRes.data() != null && !uploadRes.data().isEmpty()) {
                    var data = uploadRes.data().get(0);
                    ModifiedFile modifiedFile = new ModifiedFile(
                            data.fileName(),
                            null,
                            data.tempFilePath(),
                            data.fileMetaData(),
                            data.knowledgeConfig(),
                            "added"
                    );
                    registerReq.addModifiedFile(modifiedFile);
                }

                // 임시 파일 삭제
                if (tempFile.exists()) {
                    tempFile.delete();
                }

            } catch (IOException e) {
                log.error("[AX Platform] File processing error: {}", e.getMessage(), e);
            } catch (Exception e) {
                log.error("[AX Platform] Unexpected error during file upload: {}", e.getMessage(), e);
            }
        }

        // 4. 업로드된 파일들을 datasource에 등록
        try {
            var axMetaRegisterRes = axPlatformApi.register(datasourceId, registerReq);
            log.info("[AX Platform] register result: {}", axMetaRegisterRes);
        } catch (Exception e) {
            log.error("[AX Platform] Failed to register files to datasource: {}", e.getMessage(), e);
        }
    }
}
