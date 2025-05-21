package com.sk.domain.admin.listener;

import java.io.File;
import java.io.IOException;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.sk.common.utils.JsonFileWriter;
import com.sk.domain.admin.listener.event.AXUploadFileEvent;
import com.sk.domain.shared.client.dto.AXMetaFileDto;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AXEventListener {
    private final JsonFileWriter jsonFileWriter;

    @Async
    @EventListener
    public void onUploadFileEvent(AXUploadFileEvent event) {
        System.out.println(" =====>>>> Event: + " + Thread.currentThread().getId());
        log.info("AXUploadFileEvent: {}", event);

        GoodsData goodsData = event.getGoodsData();
        if (goodsData == null) {
            log.error("GoodsData is null");
            return;
        }
        AXMetaFileDto axMetaFileDto = event.getAxMetaFileDto();
        if (axMetaFileDto == null) {
            log.error("AXMetaFileDto is null");
            return;
        }

        if (event.getIsUpdate() == true) {
            // 기존 파일 삭제
        }

        try {
            String directoryPath = "./temp";
            File directory = new File(directoryPath);
            if (!directory.exists()) {
                directory.mkdir();
            }
            String uniqueFilename = String.valueOf(System.currentTimeMillis());

            // JSON 파일 생성
            jsonFileWriter.write(axMetaFileDto, directoryPath + File.separator + uniqueFilename.concat(".json"));

            // todo: API 호출 로직 작성

            // 업로드 결과 저장
            goodsData.updateMetaDataUploadStatus(true);
        } catch (IOException e) {
            // 예외 처리 로직
            e.printStackTrace();
            // log.error(documentId, e);
        }

    }
}
