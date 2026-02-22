package com.sk.domain.admin.listener.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sk.common.utils.DateUtil;
import com.sk.common.utils.MultipartFileUtil;
import com.sk.domain.admin.listener.event.AXUploadFileEvent;
import com.sk.domain.mi.dto.response.detail.GoodsDataRes;
import com.sk.domain.mi.dto.response.detail.GoodsDataRes.CategoryDataInfo;
import com.sk.domain.mi.service.MiDataDetailService;
import com.sk.domain.shared.client.AxPlatformApi;
import com.sk.domain.shared.client.dto.request.AXMetaRegisterReq;
import com.sk.domain.shared.client.dto.request.AXMetaRegisterReq.ModifiedFile;
import com.sk.domain.shared.client.dto.response.AXMetaLookUpRes;
import com.sk.domain.shared.client.dto.response.AXMetaUploadRes;
import com.sk.domain.shared.dto.request.file.FileDownloadReq;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataExtension;
import com.sk.domain.shared.enums.AxpKnowledgeRegisterComponentType;
import com.sk.domain.shared.enums.CategorySeparateHighCd;
import com.sk.domain.shared.enums.FileRelationshipObjectSeparateCode;
import com.sk.domain.shared.repository.master.tmd.GoodsDataExtensionRepository;
import com.sk.domain.shared.service.FileService;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Service
@Slf4j
public class FileAxpKnowledgeRegister implements AxpKnowledgeRegister {

    private final FileService fileService;
    private final AxPlatformApi axPlatformApi;
    private final ObjectMapper objectMapper;
    private final GoodsDataExtensionRepository goodsDataExtensionRepository;
    private final MiDataDetailService miDataDetailService;

    @Override
    public AxpKnowledgeRegisterComponentType getType() {
        return AxpKnowledgeRegisterComponentType.FILE;
    }

    @Override
    public void process(AXUploadFileEvent event, String datasourceId, GoodsData goodsData) {
        log.info("[AX Platform] File upload process start for goodsDataNo: {}", goodsData.getGoodsDataNo());

        // 1. goodsData와 연관된 파일 목록 조회
        List<UploadedFile> files = fileService.getFiles(goodsData.getGoodsDataNo(), FileRelationshipObjectSeparateCode.GUDAT.getCode());
        Optional<GoodsDataExtension> goodsDataExtension = goodsDataExtensionRepository.findById(goodsData.getGoodsDataNo());
        GoodsDataRes goodsDataRes = miDataDetailService.getGoodsData(goodsData.getGoods().getGoodsNo(), goodsData.getGoodsDataNo());

        AXMetaRegisterReq registerReq = new AXMetaRegisterReq();

        // 1. 삭제된 파일 및 기존 파일 lookup 처리
        handleDeletedFiles(event, datasourceId, files, registerReq);

        // 2. 파일 업로드 및 registerReq에 추가
        uploadFiles(files, registerReq, goodsData, goodsDataExtension, goodsDataRes);

        // 3. 업로드된 파일들을 datasource에 등록
        registerToDatasource(datasourceId, registerReq);

        // 모두 수행이 끝나면 goodsData.updateMetaDataUploadStatus(true); 를 실행
        goodsData.updateMetaDataUploadStatus(true);
    }

    private void handleDeletedFiles(AXUploadFileEvent event, String datasourceId, List<UploadedFile> files, AXMetaRegisterReq registerReq) {
        if (!Boolean.FALSE.equals(event.getIsNew())) {
            return;
        }

        Set<String> fileNamesToLookup = new HashSet<>();
        if (event.getDeletedFiles() != null) {
            for (UploadedFile deletedFile : event.getDeletedFiles()) {
                fileNamesToLookup.add(deletedFile.getOriginalFileNm());
            }
        }
        for (UploadedFile uploadedFile : files) {
            fileNamesToLookup.add(uploadedFile.getOriginalFileNm());
        }

        for (String fileName : fileNamesToLookup) {
            try {
                AXMetaLookUpRes lookUpRes = axPlatformApi.lookup(datasourceId, fileName);
                log.info("[AX Platform] lookup: " + lookUpRes);

                if (lookUpRes != null && lookUpRes.fileId() != null) {
                    ModifiedFile deletedFile = new ModifiedFile(
                            fileName,
                            lookUpRes.fileId(),
                            null,
                            null,
                            null,
                            "deleted");

                    registerReq.addModifiedFile(deletedFile);
                }
            } catch (Exception e) {
                log.warn("[AX Platform] lookup failed for file: {}, error: {}", fileName, e.getMessage());
            }
        }
    }

    private void uploadFiles(List<UploadedFile> files, AXMetaRegisterReq registerReq, GoodsData goodsData,
        Optional<GoodsDataExtension> goodsDataExtension,
        GoodsDataRes goodsDataRes) {
        if (files.isEmpty()) {
            log.info("[AX Platform] No files to upload.");
            return;
        }

        for (UploadedFile uploadedFile : files) {
            try {
                // 2. 파일 다운로드
                FileDownloadReq downloadReq = new FileDownloadReq(uploadedFile.getFileNo(), uploadedFile.getOriginalFileNm(), uploadedFile.getFileSize());
                var downloadRes = fileService.downloadFile(downloadReq);

                // 임시 파일 생성 - 3. 업로드 할때 파일이름은 storageFileNm를 사용
                File tempFile = new File(System.getProperty("java.io.tmpdir"), uploadedFile.getStorageFileNm());
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    fos.write(downloadRes.fileData());
                }

                try {
                    // 3. axPlatformApi.upload 로 업로드
                    MultipartFileUtil multipartFile = new MultipartFileUtil("files", tempFile, null);
                    AXMetaUploadRes uploadRes = axPlatformApi.upload(multipartFile);
                    log.info("[AX Platform] uploaded file: {}, res: {}", uploadedFile.getOriginalFileNm(), uploadRes);

                    // 4. registerReq 에 업로드한 파일 정보를 추가
                    if (uploadRes.data() != null && !uploadRes.data().isEmpty()) {
                        var data = uploadRes.data().get(0);
                        ObjectNode fileMetaData = createFileMetaData(data.fileMetaData(), goodsData, goodsDataExtension,
                            uploadedFile, goodsDataRes);

                        ModifiedFile modifiedFile = new ModifiedFile(
                                data.fileName(),
                                null,
                                data.tempFilePath(),
                                fileMetaData,
                                data.knowledgeConfig(),
                                "added"
                        );
                        registerReq.addModifiedFile(modifiedFile);
                    }
                } finally {
                    // 임시 파일 삭제
                    if (tempFile.exists()) {
                        tempFile.delete();
                    }
                }
            } catch (IOException e) {
                log.error("[AX Platform] File processing error: {}", e.getMessage(), e);
            } catch (Exception e) {
                log.error("[AX Platform] Unexpected error during file upload: {}", e.getMessage(), e);
            }
        }
    }

    private ObjectNode createFileMetaData(ObjectNode existingMetaData, GoodsData goodsData,
        Optional<GoodsDataExtension> goodsDataExtension,
        UploadedFile uploadedFile, GoodsDataRes goodsDataRes) {
        ObjectNode fileMetaData = existingMetaData != null ? existingMetaData : objectMapper.createObjectNode();

        String contentType = "Insight";
        for(CategoryDataInfo categoryDataInfo : goodsDataRes.categories()){
            if(categoryDataInfo.categoryHighCd() == CategorySeparateHighCd.TYPE){
                if(categoryDataInfo.value() != null && !categoryDataInfo.value().isEmpty()){
                    contentType = categoryDataInfo.value().get(0);
                }
            }
        }
        // 요구사항에 명시된 fileMetaData 내용 추가
        fileMetaData.put("content_id", String.valueOf(goodsData.getGoodsDataNo()));
        fileMetaData.put("file_name", uploadedFile.getOriginalFileNm());
        fileMetaData.put("file_extension", uploadedFile.getFileExtNm());
        fileMetaData.put("title", goodsData.getGoodsDataNm());
        fileMetaData.put("content_type", contentType);
        LocalDate publishedDate = goodsData.getPublishedDate();
        if(publishedDate != null){
            fileMetaData.put("published_date", DateUtil.toYmdHyphen(publishedDate));
        }else{
            fileMetaData.put("published_date", DateUtil.toYmdHyphen(goodsDataRes.auditInfo().modifyDt().toLocalDate()));
        }

        fileMetaData.put("summary", goodsData.getGoodsDataSummaryCntt());
        if(goodsDataExtension.isPresent()){
            ArrayNode authorNames = fileMetaData.putArray("author_names");
            goodsDataExtension.get().getAuthors().forEach(authorNames::add);
        }

        return fileMetaData;
    }

    private void registerToDatasource(String datasourceId, AXMetaRegisterReq registerReq) {
        try {
            var axMetaRegisterRes = axPlatformApi.register(datasourceId, registerReq);
            log.info("[AX Platform] register result: {}", axMetaRegisterRes);
        } catch (Exception e) {
            log.error("[AX Platform] Failed to register files to datasource: {}", e.getMessage(), e);
        }
    }
}
