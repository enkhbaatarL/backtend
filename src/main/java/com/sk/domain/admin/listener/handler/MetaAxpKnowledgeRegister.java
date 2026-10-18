package com.sk.domain.admin.listener.handler;

import com.sk.common.utils.JsonFileWriter;
import com.sk.common.utils.MultipartFileUtil;
import com.sk.domain.admin.dto.response.provider.CategoryDataRes;
import com.sk.domain.admin.listener.event.AXUploadFileEvent;
import com.sk.domain.admin.util.GoodsUtil;
import com.sk.domain.shared.client.AxPlatformApi;
import com.sk.domain.shared.client.dto.AXMetaFileDto;
import com.sk.domain.shared.client.dto.request.AXMetaRegisterReq;
import com.sk.domain.shared.client.dto.request.AXMetaRegisterReq.ModifiedFile;
import com.sk.domain.shared.client.dto.response.AXMetaLookUpRes;
import com.sk.domain.shared.client.dto.response.AXMetaRegisterRes;
import com.sk.domain.shared.client.dto.response.AXMetaUploadRes;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataExtension;
import com.sk.domain.shared.enums.AxpKnowledgeRegisterComponentType;
import com.sk.domain.shared.enums.FileRelationshipObjectSeparateCode;
import com.sk.domain.shared.repository.master.tmd.GoodsDataCategoryRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataExtensionRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataRepository;
import com.sk.domain.shared.service.FileService;
import java.io.File;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class MetaAxpKnowledgeRegister implements AxpKnowledgeRegister{

    private final FileService fileService;
    private final GoodsDataCategoryRepository goodsDataCategoryRepository;
    private final GoodsDataExtensionRepository goodsDataExtensionRepository;
    private final GoodsDataRepository goodsDataRepository;
    private final AxPlatformApi axPlatformApi;
    private final JsonFileWriter jsonFileWriter;

    @Override
    public AxpKnowledgeRegisterComponentType getType() {
        return AxpKnowledgeRegisterComponentType.META;
    }

    @Override
    public void process(AXUploadFileEvent event, String datasourceId, GoodsData goodsData){

        String documentId = null;
        if(!event.isNew()){
            documentId = goodsData.getGoodsDataDistinguishId();
        }
        if(documentId != null && !documentId.isEmpty()){
            documentId = UUID.randomUUID().toString();
        }
        goodsData.updateGoodsDataDistinguishId(documentId);

        List<String> fileNameList = fileService.getFiles(goodsData.getGoodsDataNo(), FileRelationshipObjectSeparateCode.GUDAT.getCode())
            .stream()
            .map(UploadedFile::getOriginalFileNm)
            .toList();

        List<CategoryDataRes> categoryDataResList = goodsDataCategoryRepository.findLangCategoryDataByGoodsDataNo(goodsData.getGoodsDataNo());
        Optional<GoodsDataExtension> goodsDataExtension = goodsDataExtensionRepository.findById(goodsData.getGoodsDataNo());

        AXMetaFileDto axMetaFileDto = AXMetaFileDto.builder()
            .gdsNo(goodsData.getGoods().getGoodsNo())
            .documentId(documentId)
            .documentReleaseDate(nowOffsetDateTime())
            .headline(goodsData.getGoodsDataNm())
            .synopsis(goodsData.getGoodsDataExpn() != null ? goodsData.getGoodsDataExpn()
                : goodsData.getGoodsDataSummaryCntt())
            .contributor(goodsData.getDataContributorNm())
            .authors(goodsDataExtension.map(GoodsDataExtension::getAuthors).orElse(null))
            .documentFileName(fileNameList)
            .language(GoodsUtil.getLanguage(categoryDataResList))
            .countries(List.of(""))
            .build();

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

            if (event.isNew() == false && goodsData.getMetadUpldFileNm() != null && !goodsData.getMetadUpldFileNm().isEmpty()) {
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
            log.info("[AX Platform] register: {}", axMetaRegisterRes);

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


    private static String nowOffsetDateTime() {
        return ZonedDateTime.now(ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX"));
    }

}
