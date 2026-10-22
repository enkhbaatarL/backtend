package com.sk.domain.mi.service.nextgen.paramproposal;

import com.sk.domain.mi.dto.response.nextgen.ModelParamShape;
import com.sk.domain.shared.entity.master.tmm.MiModelParam;
import com.sk.domain.shared.entity.master.tmm.MiModelParamTableCatalog;
import com.sk.domain.shared.repository.master.tmm.MiModelParamTableCatalogRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 모델 테이블(modelFile > modelSheet > tableName)의 화면 표시명을 카탈로그에서 해석한다.
 *
 * tableName(table1 등)은 시스템 key라 그대로 라벨로 쓸 수 없어 파라미터별 카탈로그 표시명을 테이블 노드에 붙인다.
 * 표시명이 하나로 정해지지 않으면(카탈로그 미등록·화면 숨김·표시명 공백·파라미터 1건에 항목 여러 건·테이블 내 표시명 상이)
 * 경고 로그를 남기고 받은 tableName(table1 등)을 그대로 표시명으로 내린다 — FE는 displayTableName만 라벨로 쓴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelTableDisplayNameResolver {

    private final MiModelParamTableCatalogRepository miModelParamTableCatalogRepository;

    public record ModelTableKey(String modelFile, String modelSheet, String tableName) {
    }

    /** 파라미터 마스터별 표시명 (해석 불가 파라미터는 맵에 없음) */
    public Map<Long, String> resolveByModelParamId(Collection<MiModelParam> masters) {
        Set<Long> modelParamIds = masters.stream()
            .map(MiModelParam::getModelParamId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (modelParamIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<MiModelParamTableCatalog>> itemsByParamId = miModelParamTableCatalogRepository
            .findVisibleInCurrentCatalog(modelParamIds).stream()
            .collect(Collectors.groupingBy(MiModelParamTableCatalog::getModelParamId));

        Map<Long, String> displayNames = new HashMap<>();
        itemsByParamId.forEach((modelParamId, items) -> {
            if (items.size() != 1) {
                // 한 파라미터를 여러 화면 항목으로 나눈 카탈로그 — 테이블 노드에 붙일 표시명을 하나로 정할 수 없다
                log.warn("[ModelTableDisplayName] - multiple catalog items for modelParamId={} — {}",
                    modelParamId, items.stream().map(MiModelParamTableCatalog::getDisplayName).toList());
                return;
            }
            String displayName = items.get(0).getDisplayName();
            if (displayName != null && !displayName.isBlank()) {
                displayNames.put(modelParamId, displayName);
            }
        });
        return displayNames;
    }

    /** 파라미터 표시명 — 해석 불가 시 경고 로그 후 마스터의 modelTable 그대로 */
    public static String displayTableNameOrDefault(Map<Long, String> displayNames, MiModelParam master) {
        String displayName = displayNames.get(master.getModelParamId());
        if (displayName == null) {
            log.warn("[ModelTableDisplayName] - display name not resolved, fallback to tableName — modelParamId={}, parameterName={}, tableName={}",
                master.getModelParamId(), master.getParameterName(), master.getModelTable());
            return master.getModelTable();
        }
        return displayName;
    }

    /** 테이블 표시명 — 해석 불가 시 경고 로그 후 받은 tableName 그대로 */
    public static String displayTableNameOrDefault(Map<ModelTableKey, String> displayNames, ModelTableKey key) {
        String displayName = displayNames.get(key);
        if (displayName == null) {
            log.warn("[ModelTableDisplayName] - display name not resolved, fallback to tableName — {}", key);
            return key.tableName();
        }
        return displayName;
    }

    /** 파라미터 마스터들을 테이블 단위로 묶어 표시명을 해석 */
    public Map<ModelTableKey, String> resolveByTable(Collection<MiModelParam> masters) {
        Map<Long, String> displayNames = resolveByModelParamId(masters);
        Map<ModelTableKey, Set<String>> namesByTable = new HashMap<>();
        for (MiModelParam master : masters) {
            namesByTable
                .computeIfAbsent(new ModelTableKey(master.getModelFile(), master.getModelSheet(), master.getModelTable()), key -> new LinkedHashSet<>())
                .add(displayNames.get(master.getModelParamId()));
        }
        return uniqueNames(namesByTable);
    }

    /**
     * 이미 표시명이 해석된 표 모양(parameterShapes)을 테이블 단위로 묶는다 — 추가 조회 없음.
     * shape 단계에서 대체된 파라미터가 섞인 테이블은 표시명이 상이해 테이블 단위에서도 tableName으로 대체된다.
     */
    public static Map<ModelTableKey, String> byTableFromShapes(Collection<ModelParamShape> shapes) {
        Map<ModelTableKey, Set<String>> namesByTable = new HashMap<>();
        for (ModelParamShape shape : shapes) {
            namesByTable
                .computeIfAbsent(new ModelTableKey(shape.modelFile(), shape.modelSheet(), shape.modelTable()), key -> new LinkedHashSet<>())
                .add(shape.displayTableName());
        }
        return uniqueNames(namesByTable);
    }

    /** 테이블 내 파라미터들의 표시명이 모두 같은 값으로 정해진 경우만 채택 */
    private static Map<ModelTableKey, String> uniqueNames(Map<ModelTableKey, Set<String>> namesByTable) {
        Map<ModelTableKey, String> resolved = new HashMap<>();
        namesByTable.forEach((key, names) -> {
            if (names.size() == 1 && !names.contains(null)) {
                resolved.put(key, names.iterator().next());
            } else if (names.stream().filter(Objects::nonNull).count() > 1) {
                log.warn("[ModelTableDisplayName] - conflicting display names in one table {} — {}", key, names);
            }
        });
        return resolved;
    }

}
