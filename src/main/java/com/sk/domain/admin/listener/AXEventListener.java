package com.sk.domain.admin.listener;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.JsonFileWriter;
import com.sk.common.utils.MultipartFileUtil;
import com.sk.domain.admin.listener.event.AXUploadFileEvent;
import com.sk.domain.shared.client.AuthTokenApi;
import com.sk.domain.shared.client.AxPlatformApi;
import com.sk.domain.shared.client.dto.AXMetaFileDto;
import com.sk.domain.shared.client.dto.request.AXMetaRegisterReq;
import com.sk.domain.shared.client.dto.request.AXMetaRegisterReq.ModifiedFile;
import com.sk.domain.shared.client.dto.response.AXMetaRegisterRes;
import com.sk.domain.shared.client.dto.response.AXMetaLookUpRes;
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
    @Transactional
    public void onUploadFileEvent(AXUploadFileEvent event) {
        Integer goodsDataNo = event.getGoodsDataNo();

        GoodsData goodsData = goodsDataRepository.findByGoodsDataNoAndIsDeletedFalse(goodsDataNo).orElse(null);
        if (goodsData == null) {
            log.error("GoodsData is null");
            return;
        }
        AXMetaFileDto axMetaFileDto = event.getAxMetaFileDto();
        if (axMetaFileDto == null) {
            log.error("AXMetaFileDto is null");
            return;
        }

        try {
            String directoryPath = "./temp";
            File directory = new File(directoryPath);
            if (!directory.exists()) {
                directory.mkdir();
            }
            String uniqueFilename = String.valueOf(System.currentTimeMillis()).concat(".json");

            // JSON 파일 생성
            jsonFileWriter.write(axMetaFileDto, directoryPath + File.separator + uniqueFilename);

            File file = new File(directoryPath + File.separator + uniqueFilename);
            MultipartFileUtil multipartFile = new MultipartFileUtil("files", file, null);

            AuthTokenRes authTokenRes = authTokenApi.getMessage();
            if (authTokenRes == null || authTokenRes.accessToken() == null) {
                System.out.println("   ===>>> Failed to retrieve access token: " + authTokenRes);
                return;
            }

            String accessToken = "Bearer ".concat(authTokenRes.accessToken());

            AXMetaUploadRes uploadRes = axPlatformApi.upload(accessToken, multipartFile);

            ModifiedFile uploadFile = new ModifiedFile(uploadRes.data().get(0).fileName(),
                    null,
                    uploadRes.data().get(0).tempFilePath(),
                    uploadRes.data().get(0).fileMetaData(),
                    uploadRes.data().get(0).knowledgeConfig(),
                    "added");

            AXMetaRegisterReq registerReq = new AXMetaRegisterReq();
            registerReq.addModifiedFile(uploadFile);

            if (event.getIsNew() == false && goodsData.getMetadUpldFileNm() != null && !goodsData.getMetadUpldFileNm().isEmpty()) {
                // 기존 파일 삭제
                String fileName = goodsData.getMetadUpldFileNm();

                AXMetaLookUpRes lookUpRes = axPlatformApi.lookup(accessToken, fileName);

                ModifiedFile deletedFile = new ModifiedFile(null,
                        lookUpRes.fileId(),
                        null,
                        null,
                        null,
                        "deleted");

                registerReq.addModifiedFile(deletedFile);
            }

            axPlatformApi.register(accessToken, datasourceId, registerReq);

            // 업로드 결과 저장
            goodsData.updateMetaDataUploadStatus(true);
            goodsData.updateMetadUpldFileNm(uniqueFilename);
        } catch (IOException e) {
            // 예외 처리 로직
            e.printStackTrace();
            // log.error(documentId, e);
        }
    }
}
