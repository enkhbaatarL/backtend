package com.sk.domain.mi.service.newsletter.strategy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.model.TableRes;
import com.sk.common.utils.CombineUtil;
import com.sk.domain.admin.dto.request.dynamicData.DynamicDataReq;
import com.sk.domain.admin.service.MybatisDynamicQueryService;
import com.sk.domain.mi.dto.internal.IssueStateTree;
import com.sk.domain.mi.dto.internal.ResolvedBlockData;
import com.sk.domain.mi.dto.internal.TemplateTree;
import com.sk.domain.shared.entity.master.tmd.NewsletterPublishHistory;
import com.sk.domain.shared.entity.master.tmd.NewsletterTemplatePreferenceTable;
import com.sk.domain.shared.repository.master.tmd.NewsletterTemplatePreferenceTableRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("DYNAMIC_QUERY")
@RequiredArgsConstructor
public class DynamicQueryBlockDataStrategy implements BlockDataStrategy {

    private final NewsletterTemplatePreferenceTableRepository preferenceTableRepository;
    private final MybatisDynamicQueryService mybatisDynamicQueryService;
    private final ObjectMapper objectMapper;

    @Override
    public String getType() {
        return "DYNAMIC_QUERY";
    }

    @Override
    public ResolvedBlockData resolve(
        TemplateTree.BlockNode block,
        IssueStateTree.BlockStateNode blockState,
        NewsletterPublishHistory pubHist
    ) {
        String ref = block.dataSourceRef();
        if (ref == null || !ref.startsWith("pref_table:")) {
            return ResolvedBlockData.error("INVALID_DYNAMIC_QUERY_REFERENCE");
        }

        Integer dataRegSn = Integer.parseInt(ref.substring("pref_table:".length()));
        NewsletterTemplatePreferenceTable config = preferenceTableRepository
            .findTopByDataRegSnAndIsDeletedFalse(dataRegSn)
            .orElse(null);
        if (config == null) {
            return ResolvedBlockData.error("PREFERENCE_TABLE_NOT_FOUND");
        }

        try {
            DynamicDataReq dynamicDataReq = DynamicDataReq.builder()
                .targetDataSource(config.getTargetDatabaseName())
                .tableName(config.getTargetTableName())
                .columns(config.getTableColumnName())
                .whereClause(config.getQueryConditionContent())
                .additionalWhereClause(CombineUtil.createWhereClause("nws_ltr_pub_id", String.valueOf(pubHist.getNewsletterPublishId())))
                .build();

            List<Map<String, Object>> rows = mybatisDynamicQueryService.getDynamicData(dynamicDataReq);
            if (rows.isEmpty()) {
                return ResolvedBlockData.of(Map.of("tableData", TableRes.of(List.of(), List.of())));
            }

            Object payload = rows.get(0).get(config.getTableColumnName());
            TableRes tableRes = readTableDataFromJsonString(payload == null ? null : payload.toString());
            return ResolvedBlockData.of(Map.of("tableData", tableRes));
        } catch (Exception e) {
            log.error("동적 쿼리 블록 resolve 실패 - blockCode: {}", block.blockCode(), e);
            return ResolvedBlockData.error("DYNAMIC_QUERY_RESOLVE_FAILED");
        }
    }

    private TableRes readTableDataFromJsonString(String jsonString) throws Exception {
        if (jsonString == null) {
            return TableRes.of(List.of(), List.of());
        }

        Map<String, Object> jsonData = objectMapper.readValue(jsonString, new TypeReference<>() {
        });
        List<TableRes.Column> headers = new ArrayList<>();
        List<TableRes.Row> rows = new ArrayList<>();

        @SuppressWarnings("unchecked")
        List<Map<String, String>> headerList = (List<Map<String, String>>) jsonData.get("header");
        if (headerList != null) {
            headerList.forEach(header -> headers.add(TableRes.Column.of(header.get("field"), header.get("name"))));
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> detailList = (List<Map<String, Object>>) jsonData.get("detail");
        if (detailList != null) {
            detailList.forEach(detail -> rows.add(TableRes.Row.of((List<String>) detail.getOrDefault("cells", List.of()))));
        }

        return TableRes.of(headers, rows);
    }
}
