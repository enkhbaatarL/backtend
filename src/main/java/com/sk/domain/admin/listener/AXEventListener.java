package com.sk.domain.admin.listener;

import java.io.File;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sk.common.utils.JsonFileWriter;
import com.sk.common.utils.MultipartFileUtil;
import com.sk.domain.admin.listener.event.AXUploadFileEvent;
import com.sk.domain.shared.client.AuthTokenApi;
import com.sk.domain.shared.client.AxPlatformApi;
import com.sk.domain.shared.client.dto.AXMetaFileDto;
import com.sk.domain.shared.client.dto.request.AXMetaRegisterReq;
import com.sk.domain.shared.client.dto.request.AXMetaRegisterReq.ModifiedFile;
import com.sk.domain.shared.client.dto.response.AXMetaLookUpRes;
import com.sk.domain.shared.client.dto.response.AXMetaRegisterRes;
import com.sk.domain.shared.client.dto.response.AXMetaUploadRes;
import com.sk.domain.shared.client.dto.response.AuthTokenRes;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.repository.master.tmd.GoodsDataRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AXEventListener {
    private final GoodsDataRepository goodsDataRepository;
    private final JsonFileWriter jsonFileWriter;
    private final AuthTokenApi authTokenApi;
    private final AxPlatformApi axPlatformApi;

    @Value("${api.ax-platform.datasource-id}")
    private String datasourceId;

    @Async
    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUploadFileEvent(AXUploadFileEvent event) {
        Integer goodsDataNo = event.getGoodsDataNo();

        GoodsData goodsData = goodsDataRepository.findByGoodsDataNoAndIsDeletedFalse(goodsDataNo).orElse(null);
        if (goodsData == null) {
            log.error("[AX Platform] GoodsData not found: goodsDataNo= {}", goodsDataNo);
            return;
        }
        AXMetaFileDto axMetaFileDto = event.getAxMetaFileDto();
        if (axMetaFileDto == null) {
            log.error("[AX Platform] AXMetaFileDto is null");
            return;
        }

        try {
            String directoryPath = "./temp";
            File directory = new File(directoryPath);
            if (!directory.exists()) {
                directory.mkdir();
            }
            String uniqueFilename = String.valueOf(System.currentTimeMillis()).concat(".json");

            jsonFileWriter.write(axMetaFileDto, directoryPath + File.separator + uniqueFilename);

            File file = new File(directoryPath + File.separator + uniqueFilename);
            MultipartFileUtil multipartFile = new MultipartFileUtil("files", file, null);

            // 문서 Meta File 업로드
            AXMetaUploadRes uploadRes = axPlatformApi.upload(multipartFile);
            log.info("[AX Platform] upload: " + uploadRes);

            ModifiedFile uploadFile = new ModifiedFile(uploadRes.data().get(0).fileName(),
                    null,
                    uploadRes.data().get(0).tempFilePath(),
                    uploadRes.data().get(0).fileMetaData(),
                    uploadRes.data().get(0).knowledgeConfig(),
                    "added");

            AXMetaRegisterReq registerReq = new AXMetaRegisterReq();
            registerReq.addModifiedFile(uploadFile);

            if (event.getIsNew() == false && goodsData.getMetadUpldFileNm() != null && !goodsData.getMetadUpldFileNm().isEmpty()) {
                String fileName = goodsData.getMetadUpldFileNm();

                if (fileName != null && !fileName.isEmpty()) {
                    // 삭제 대상 파일의 정보 조회
                    AXMetaLookUpRes lookUpRes = axPlatformApi.lookup(datasourceId, fileName);
                    log.info("[AX Platform] lookup: " + lookUpRes);

                    ModifiedFile deletedFile = new ModifiedFile(
                            fileName,
                            lookUpRes.fileId(),
                            null,
                            null,
                            null,
                            "deleted");

                    registerReq.addModifiedFile(deletedFile);
                }
            }

            // Datasource에 삭제 대상 파일, 변경된 파일 정보 등록
            AXMetaRegisterRes axMetaRegisterRes = axPlatformApi.register(datasourceId, registerReq);
            log.info("[AX Platform] register: " + axMetaRegisterRes);

            // 업로드 결과 저장
            goodsData.updateMetaDataUploadStatus(true);
            goodsData.updateMetadUpldFileNm(uniqueFilename);
            goodsDataRepository.save(goodsData);
        } catch (IOException e) {
            log.error("[AX Platform] IOException Error: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("[AX Platform] Exception Error: {}", e.getMessage(), e);
        }
    }
}
