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
import java.util.Optional;
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

        List<Related> related = new ArrayList<>();

        // 1. 정형데이터 연관데이터 추출
        List<Related> structured = prepareStructuredDateResources(modelAgentStreamBody);
        if (structured != null && !structured.isEmpty()) {
            related.addAll(structured);
        }

        // 2. 비정형데이터 연관데이터 추출
        List<Related> unstructured = prepareUnstructuredDateResources(modelAgentStreamBody);
        if (unstructured != null && !unstructured.isEmpty()) {
            related.addAll(unstructured);
        }

        return related;
    }

    private List<Related> prepareStructuredDateResources(ModelAgentRes.Output output) {
        List<Integer> gdsNos = extractRelatedGdsNos(output);
        return prepareStructuredDateResources(gdsNos);
    }

    private List<Related> prepareStructuredDateResources(ModelAgentStreamBody modelAgentStreamBody) {

        List<Integer> gdsNos = modelAgentStreamBody.relatedData();
        return prepareStructuredDateResources(gdsNos);
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
        return prepareUnstructuredDateResources(extractDistinguishIds(modelAgentStreamBody.vectorSources()));
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
        return Optional.ofNullable(output)
            .map(ModelAgentRes.Output::messages)
            .orElse(Collections.emptyList()) // null 체크 강화
            .stream()
            .map(ModelAgentRes.Output.Message::content)
            .filter(Objects::nonNull)
            .map(ModelAgentRes.Output.Message.Content::relatedData)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .toList();
    }

    /**
     * 상품 데이터 식별번호 추출
     */
    private List<String> extractDistinguishIds(ModelAgentRes.Output output) {
        List<ModelAgentRes.Output.Message.Content.VectorSource> vectorSources = Optional.ofNullable(output)
            .map(ModelAgentRes.Output::messages)
            .orElse(Collections.emptyList()) // null 체크 강화
            .stream()
            .map(ModelAgentRes.Output.Message::content)
            .filter(Objects::nonNull)
            .map(ModelAgentRes.Output.Message.Content::vectorSources)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .toList();

        return extractDistinguishIds(vectorSources);
    }

    private List<String> extractDistinguishIds(List<ModelAgentRes.Output.Message.Content.VectorSource> vectorSources) {
        return Optional.ofNullable(vectorSources)
            .orElse(Collections.emptyList())
            .stream()
            .map(ModelAgentRes.Output.Message.Content.VectorSource::gdsDataDstgId)
            .toList();
    }

    public List<Related> prepareRelatedData(List<Integer> relatedDataIds) {
    if (relatedDataIds == null || relatedDataIds.isEmpty()) {
        return Collections.emptyList();
    }
    return prepareStructuredDateResources(relatedDataIds);
}

}
