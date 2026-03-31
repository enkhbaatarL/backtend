package com.sk.domain.mi.service.agent;

import com.sk.domain.mi.dto.response.agent.talk.Related;
import com.sk.domain.shared.client.dto.response.ModelAgentRes;
import com.sk.domain.shared.client.dto.response.ModelAgentStreamBody;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataSourceDetailRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MariAgentResParser {

    private final GoodsDataSourceDetailRepository goodsDataSourceDetailRepository;
    private final CustomerCompanyRepository customerCompanyRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final GoodsDataRepository goodsDataRepository;

    public List<Related> prepareAnswerDataResources(ModelAgentRes.Output output) {

        List<Related> related = new ArrayList<>();

        // 1. 정형데이터 연관데이터 추출
        List<Related> structured = prepareStructuredDateResources(output);
        if (structured != null && !structured.isEmpty()) {
            related.addAll(structured);
        }

        // 2. 비정형데이터 연관데이터 추출
        List<Related> unstructured = prepareUnstructuredDateResources(output);
        if (unstructured != null && !unstructured.isEmpty()) {
            related.addAll(unstructured);
        }

        return related;
    }

    public List<Related> prepareAnswerDataResources(ModelAgentStreamBody modelAgentStreamBody) {
        return prepareRelatedByRdbAndVdb(modelAgentStreamBody);
    }

    private List<Related> prepareStructuredDateResources(ModelAgentRes.Output output) {
        List<Integer> gdsNos = extractRelatedGdsNos(output);
        return prepareStructuredDateResources(gdsNos);
    }

    private List<Related> prepareStructuredDateResources(ModelAgentStreamBody modelAgentStreamBody) {
        List<Integer> goodsDataNos = extractRdbProductDataNumbers(modelAgentStreamBody);
        if (goodsDataNos.isEmpty()) {
            return Collections.emptyList();
        }
        return toRelatedByGoodsDataNos(goodsDataNos, Related.Type.STRUCTURED);
    }

    private List<Related> prepareStructuredDateResources(List<Integer> gdsNos) {
        Map<Integer, GoodsDataSourceDetail> gdsMap = goodsDataSourceDetailRepository.findByGoodsDataSourceNoInAndIsDeletedFalse(gdsNos)
            .stream()
            .collect(Collectors.toMap(GoodsDataSourceDetail::getGoodsDataSourceNo, Function.identity()));

        return Related.ofStructuredList(gdsNos, gdsMap);
    }

    private List<Related> prepareUnstructuredDateResources(ModelAgentRes.Output output) {
        return prepareUnstructuredDateResources(extractDistinguishIds(output));
    }

    private List<Related> prepareUnstructuredDateResources(ModelAgentStreamBody modelAgentStreamBody) {
        List<Integer> goodsDataNosFromProductDataNum = extractVdbProductDataNumbers(modelAgentStreamBody);
        List<Related> relatedFromProductDataNum = goodsDataNosFromProductDataNum.isEmpty()
            ? Collections.emptyList()
            : toRelatedByGoodsDataNos(goodsDataNosFromProductDataNum, Related.Type.UNSTRUCTURED);

        List<GoodsData> goodsDataFromDocumentId = extractVdbGoodsDataFromDocumentIds(modelAgentStreamBody);
        List<Related> relatedFromDocumentId = toRelatedByGoodsDataList(
            goodsDataFromDocumentId, Related.Type.UNSTRUCTURED);

        List<Related> combined = new ArrayList<>();
        combined.addAll(relatedFromProductDataNum);
        combined.addAll(relatedFromDocumentId);
        return combined;
    }

    private List<Related> prepareUnstructuredDateResources(List<String> distinguishIds) {
        List<GoodsData> goodsDataList = goodsDataRepository.findByGoodsDataDistinguishIdInAndIsDeletedFalse(distinguishIds);
        List<Integer> goodsDataNos = goodsDataList.stream().map(GoodsData::getGoodsDataNo).toList();

        Map<Integer, UploadedFile> fileMap = uploadedFileRepository.findByFileRelationshipObjectDistinguishNoInAndIsDeletedFalse(goodsDataNos)
            .stream()
            .collect(Collectors.toMap(UploadedFile::getFileRelationshipObjectDistinguishNo, Function.identity(), (v1, v2) -> v2));

        return Related.ofUnstructuredList(goodsDataList, fileMap);
    }


    /**
     * 정형데이터 식별자 추출
     */
    private List<Integer> extractRelatedGdsNos(ModelAgentRes.Output output) {
//        return Optional.ofNullable(output)
//            .map(ModelAgentRes.Output::messages)
//            .orElse(Collections.emptyList()) // null 체크 강화
//            .stream()
//            .map(ModelAgentRes.Output.Message::content)
//            .filter(Objects::nonNull)
//            .map(ModelAgentRes.Output.Message.Content::relatedData)
//            .filter(Objects::nonNull)
//            .flatMap(List::stream)
//            .toList();
        return Collections.emptyList();
    }

    /**
     * 상품 데이터 식별번호 추출
     */
    private List<String> extractDistinguishIds(ModelAgentRes.Output output) {
//        List<ModelAgentRes.Output.Message.Content.VectorSource> vectorSources = Optional.ofNullable(output)
//            .map(ModelAgentRes.Output::messages)
//            .orElse(Collections.emptyList()) // null 체크 강화
//            .stream()
//            .map(ModelAgentRes.Output.Message::content)
//            .filter(Objects::nonNull)
//            .map(ModelAgentRes.Output.Message.Content::vectorSources)
//            .filter(Objects::nonNull)
//            .flatMap(List::stream)
//            .toList();
//
//        return extractDistinguishIds(vectorSources);
        return Collections.emptyList();
    }

    private List<String> extractDistinguishIds(List<ModelAgentRes.Output> vectorSources) {
//        return Optional.ofNullable(vectorSources)
//            .orElse(Collections.emptyList())
//            .stream()
//            .map(ModelAgentRes.Output.Message.Content.VectorSource::gdsDataDstgId)
//            .toList();
        return Collections.emptyList();
    }

    private List<Integer> extractRelatedGdsNos(ModelAgentStreamBody streamBody) {
        if (streamBody == null || streamBody.segment() == null || streamBody.segment().sources() == null
            || streamBody.segment().sources().rdb() == null
            || streamBody.segment().sources().rdb().productNumbers() == null) {
            return Collections.emptyList();
        }

        return streamBody.segment().sources().rdb().productNumbers().stream()
            .filter(Objects::nonNull)
            .toList();
    }

    private List<Integer> extractRdbProductDataNumbers(ModelAgentStreamBody streamBody) {
        if (streamBody == null || streamBody.segment() == null || streamBody.segment().sources() == null
            || streamBody.segment().sources().rdb() == null
            || streamBody.segment().sources().rdb().productDataNumbers() == null) {
            return Collections.emptyList();
        }

        return streamBody.segment().sources().rdb().productDataNumbers().stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    private List<Integer> extractVdbProductDataNumbers(ModelAgentStreamBody streamBody) {
        if (streamBody == null || streamBody.segment() == null || streamBody.segment().sources() == null
            || streamBody.segment().sources().vdb() == null) {
            return Collections.emptyList();
        }

        return streamBody.segment().sources().vdb().stream()
            .map(ModelAgentStreamBody.Segment.VdbSource::productDataNum)
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(s -> {
                try {
                    return Integer.valueOf(s);
                } catch (NumberFormatException e) {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    /**
     * vdb에서 product_data_num 대신 document_id가 들어오는 경우를 처리합니다.
     * - vdb의 product_num  -> tmd_gdsdatam.gds_no
     * - vdb의 document_id -> tmd_gdsdatam.gds_data_dstg_id
     */
    private List<GoodsData> extractVdbGoodsDataFromDocumentIds(ModelAgentStreamBody streamBody) {
        if (streamBody == null || streamBody.segment() == null || streamBody.segment().sources() == null
            || streamBody.segment().sources().vdb() == null) {
            return Collections.emptyList();
        }

        Map<Integer, List<String>> goodsNoToDocumentIds = streamBody.segment().sources().vdb().stream()
            .filter(vdb -> vdb.productNum() != null)
            .filter(vdb -> vdb.documentId() != null && !vdb.documentId().trim().isEmpty())
            .collect(Collectors.groupingBy(
                ModelAgentStreamBody.Segment.VdbSource::productNum,
                Collectors.mapping(vdb -> vdb.documentId().trim(), Collectors.toList())
            ));

        if (goodsNoToDocumentIds.isEmpty()) {
            return Collections.emptyList();
        }

        return goodsNoToDocumentIds.entrySet().stream()
            .flatMap(entry -> goodsDataRepository
                .findByGoods_GoodsNoAndGoodsDataDistinguishIdInAndIsDeletedFalse(
                    entry.getKey(),
                    entry.getValue().stream().distinct().toList()
                ).stream())
            .collect(Collectors.toMap(GoodsData::getGoodsDataNo, Function.identity(), (a, b) -> a))
            .values()
            .stream()
            .toList();
    }

    private List<Related> toRelatedByGoodsDataNos(List<Integer> goodsDataNos, Related.Type type) {
        if (goodsDataNos == null || goodsDataNos.isEmpty()) {
            return Collections.emptyList();
        }

        List<GoodsData> goodsDataList = goodsDataRepository.findByGoodsDataNoInAndIsDeletedFalse(goodsDataNos);
        return goodsDataList.stream()
            .filter(goodsData -> goodsData.getGoods() != null)
            .map(goodsData -> Related.builder()
                .type(type)
                .goodsNo(goodsData.getGoods().getGoodsNo())
                .goodsDataNo(goodsData.getGoodsDataNo())
                .displayNm(goodsData.getGoodsDataNm())
                .build())
            .toList();
    }

    private List<Related> toRelatedByGoodsDataList(List<GoodsData> goodsDataList, Related.Type type) {
        if (goodsDataList == null || goodsDataList.isEmpty()) {
            return Collections.emptyList();
        }

        return goodsDataList.stream()
            .filter(Objects::nonNull)
            .filter(goodsData -> goodsData.getGoods() != null)
            .map(goodsData -> Related.builder()
                .type(type)
                .goodsNo(goodsData.getGoods().getGoodsNo())
                .goodsDataNo(goodsData.getGoodsDataNo())
                .displayNm(goodsData.getGoodsDataNm())
                .build())
            .toList();
    }

    private List<Related> prepareRelatedByRdbAndVdb(ModelAgentStreamBody streamBody) {
        List<Related> combined = new ArrayList<>();
        combined.addAll(prepareStructuredDateResources(streamBody));
        combined.addAll(prepareUnstructuredDateResources(streamBody));

        if (combined.isEmpty()) {
            return Collections.emptyList();
        }

        // type + goodsDataNo 기준 중복 제거
        Map<String, Related> unique = combined.stream()
            .filter(r -> r.goodsDataNo() != null && r.type() != null)
            .collect(Collectors.toMap(
                r -> r.type().name() + ":" + r.goodsDataNo(),
                Function.identity(),
                (existing, replacement) -> existing
            ));
        return new ArrayList<>(unique.values());
    }

    public List<Related> prepareRelatedData(List<Integer> relatedDataIds) {
    if (relatedDataIds == null || relatedDataIds.isEmpty()) {
        return Collections.emptyList();
    }
    return prepareStructuredDateResources(relatedDataIds);
}

}
