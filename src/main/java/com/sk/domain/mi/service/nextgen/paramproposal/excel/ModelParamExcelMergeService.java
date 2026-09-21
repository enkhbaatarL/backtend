package com.sk.domain.mi.service.nextgen.paramproposal.excel;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.SystemErrorException;
import com.sk.domain.shared.entity.master.tmm.MiModelParam;
import com.sk.domain.shared.repository.master.tmm.MiModelParamRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 수급모델 파라미터 확정값 Excel 병합 서비스
 * 확정된 제안 파라미터(proposedValue = 최종 입력값)를 basecase 원본 xlsm 3개에 병합해 실행용 입력 파일 3개를 만든다.
 *
 * 메모리 정책(OOM 대응): POI DOM을 만들지 않는다 — XlsmStreamMerger가 zip/XML을 스트리밍으로 직접 조작해
 * 파일 크기와 무관하게 수십 MB 수준으로 동작한다. 파일 단위 순차 처리에 원본·산출물 모두 임시 파일 기반이며,
 * 병합 대상이 없는 파일은 다운로드한 원본 임시 파일이 그대로 산출물이 된다(바이트 동일).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelParamExcelMergeService {

    private final BasecaseWorkbookProvider workbookProvider;
    private final MiModelParamRepository modelParamRepository;

    /**
     * 확정 파라미터 전체를 병합해 수급모델 입력 파일 3개를 만든다.
     * 병합 대상이 없는 파일도 원본 그대로 포함해 항상 3개를 반환한다.
     * 산출물은 임시 파일로 반환되며, 호출 측이 사용 후 cleanup()으로 삭제해야 한다.
     */
    public MergedModelInputFiles merge(List<ModelParamAdjustment> adjustments) {

        if (adjustments == null || adjustments.isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "수급모델 입력 병합 실패 — 병합할 확정 파라미터가 없습니다");
        }

        Map<String, MiModelParam> masters = loadMasters(adjustments);
        Map<ModelInputFile, List<ModelParamAdjustment>> adjustmentsByFile = groupByFile(masters, adjustments);

        Map<ModelInputFile, Path> outputs = new EnumMap<>(ModelInputFile.class);
        WriteSummary total = new WriteSummary(0, 0);
        List<String> details = new ArrayList<>();

        try {
            for (ModelInputFile file : ModelInputFile.values()) {
                Path source = workbookProvider.materialize(file);
                List<ModelParamAdjustment> fileAdjustments = adjustmentsByFile.getOrDefault(file, List.of());

                if (fileAdjustments.isEmpty()) {
                    // 병합 대상 없음 — 다운로드한 원본 임시 파일이 곧 산출물 (바이트 동일, 재계산 플래그도 불필요)
                    outputs.put(file, source);
                    details.add("[%s] pass-through(병합 대상 없음)".formatted(file.getCode()));
                    continue;
                }

                try (XlsmStreamMerger merger = new XlsmStreamMerger(source, file.getBasecaseFileName())) {
                    for (ModelParamAdjustment adjustment : fileAdjustments) {
                        MiModelParam param = masters.get(adjustment.parameterName());
                        WriteSummary summary = apply(merger, file, param, adjustment);
                        total = WriteSummary.sum(total, summary);
                        details.add(describe(param, summary));
                    }
                    Path out = createOutputFile(file);
                    outputs.put(file, out);
                    merger.writeTo(out);
                } finally {
                    if (!source.equals(outputs.get(file))) {
                        deleteQuietly(source);
                    }
                }
            }
        } catch (RuntimeException e) {
            outputs.values().forEach(this::deleteQuietly);
            throw e;
        }

        log.info("[ModelInput Merge] - DONE - parameters={}, cells(write={}, append={}) — {}",
            adjustments.size(), total.writeCount(), total.appendCount(), String.join(" / ", details));

        return MergedModelInputFiles.of(outputs);

    }

    /**
     * 확정 파라미터를 대상 파일별로 묶는다. 파일 단위 순차 처리의 기반.
     */
    private Map<ModelInputFile, List<ModelParamAdjustment>> groupByFile(
        Map<String, MiModelParam> masters,
        List<ModelParamAdjustment> adjustments
    ) {
        Map<ModelInputFile, List<ModelParamAdjustment>> grouped = new EnumMap<>(ModelInputFile.class);
        for (ModelParamAdjustment adjustment : adjustments) {
            MiModelParam param = masters.get(adjustment.parameterName());
            grouped.computeIfAbsent(ModelInputFile.from(param.getModelFile()), key -> new ArrayList<>())
                .add(adjustment);
        }
        return grouped;
    }

    /**
     * 확정 파라미터 1건을 대상 시트에 기입한다.
     * read mode에 따라 기입 전략이 다름.
     * 제안 미대상(use_yn=N)이어도 사용자 확정에 포함되었으면 기입한다 — 단 경고 흔적을 반드시 남긴다.
     */
    private WriteSummary apply(XlsmStreamMerger merger, ModelInputFile file, MiModelParam param, ModelParamAdjustment adjustment) {
        String context = ValueRows.context(param);

        if (!Boolean.TRUE.equals(param.getIsUsed())) {
            log.warn("[ModelInput Merge] - OFF-TARGET [{}] 제안 미대상(use_yn=N) 파라미터가 확정 payload에 포함됨 — 사용자 확정이므로 기입은 진행 rows={}", context, adjustment.values() == null ? 0 : adjustment.values().size());
        }

        if (adjustment.values() == null || adjustment.values().isEmpty()) {
            throw ValueRows.error(context, "기입할 values가 없습니다");
        }

        MergeSheet sheet = merger.sheet(param.getModelSheet());

        if (sheet == null) {
            throw SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "수급모델 입력 병합 실패 [%s] — Basecase 원본(%s)에 시트가 없습니다 (마스터-원본 정합 확인 필요)".formatted(context, file.getBasecaseFileName()));
        }

        ColumnRoleMap roles = ColumnRoleMap.parse(context, param.getBasecaseColumnRoles());

        return switch (param.getBasecaseReadMode()) {
            case FIXED_COLUMNS -> new FixedColumnsWriter(sheet, param, roles).apply(adjustment.values());
            case MATRIX_TIME_COLUMNS -> new MatrixTimeColumnsWriter(sheet, param, roles).apply(adjustment.values());
        };
    }

    /**
     * DONE 요약의 파라미터별 세그먼트.
     * 0인 카운트는 생략하고, 제안 미대상 기입 건에는 OFF-TARGET 마커를 붙인다.
     */
    private String describe(MiModelParam param, WriteSummary summary) {
        List<String> counts = new ArrayList<>();
        if (summary.writeCount() > 0) {
            counts.add("write=%d".formatted(summary.writeCount()));
        }
        if (summary.appendCount() > 0) {
            counts.add("append=%d".formatted(summary.appendCount()));
        }
        String marker = Boolean.TRUE.equals(param.getIsUsed()) ? "" : " (OFF-TARGET)";
        return "[%s] %s%s".formatted(ValueRows.context(param), String.join(", ", counts), marker);
    }

    /**
     * parameterName들로 마스터를 일괄 조회한다. 마스터에 없는 파라미터는 좌표를 알 수 없으므로 즉시 실패.
     */
    private Map<String, MiModelParam> loadMasters(List<ModelParamAdjustment> adjustments) {

        // DB(tmm_mi_model_param_m.parameter_name) unique
        Set<String> names = adjustments.stream()
            .map(ModelParamAdjustment::parameterName)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if (names.stream().anyMatch(name -> name == null || name.isBlank())) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "수급모델 입력 병합 실패 — parameterName이 비어 있는 확정 파라미터가 있습니다");
        }

        Map<String, MiModelParam> masters = modelParamRepository.findByParameterNameInAndIsDeletedFalse(names).stream()
            .collect(Collectors.toMap(MiModelParam::getParameterName, Function.identity()));

        Set<String> missing = names.stream()
            .filter(name -> !masters.containsKey(name))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if (!missing.isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "수급모델 입력 병합 실패 — 파라미터 마스터(tmm_mi_model_param_m)에 정의되지 않은 파라미터입니다: %s".formatted(missing));
        }

        return masters;

    }

    private Path createOutputFile(ModelInputFile file) {
        try {
            return Files.createTempFile("merged-%s-".formatted(file.name().toLowerCase(Locale.ROOT)), ".xlsm");
        } catch (IOException e) {
            log.error("[ModelInput Merge] 산출물 임시 파일 생성 실패: {}", file.getBasecaseFileName(), e);
            throw SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "수급모델 입력 병합 실패 — 병합 파일 생성에 실패했습니다: %s".formatted(file.getBasecaseFileName()));
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("[ModelInput Merge] 임시 파일 삭제 실패: {}", path, e);
        }
    }

}
