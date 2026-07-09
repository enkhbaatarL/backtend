package com.sk.domain.mi.service.nextgen.paramproposal.excel;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.SystemErrorException;
import com.sk.domain.shared.entity.master.tmm.MiModelParam;
import com.sk.domain.shared.repository.master.tmm.MiModelParamRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

/**
 * 수급모델 파라미터 확정값 Excel 병합 서비스
 * 확정된 제안 파라미터(proposedValue = 최종 입력값)를 basecase 원본 xlsm 3개에 병합해 실행용 입력 파일 3개를 만든다.
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
     */
    public MergedModelInputFiles merge(List<ModelParamAdjustment> adjustments) {

        if (adjustments == null || adjustments.isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "수급모델 입력 병합 실패 — 병합할 확정 파라미터가 없습니다");
        }

        Map<String, MiModelParam> masters = loadMasters(adjustments);

        Map<ModelInputFile, XSSFWorkbook> workbooks = new EnumMap<>(ModelInputFile.class);

        try {

            for (ModelInputFile file : ModelInputFile.values()) {
                workbooks.put(file, workbookProvider.open(file));
            }

            WriteSummary total = new WriteSummary(0, 0);
            int skippedParams = 0;
            int skippedRows = 0;

            for (ModelParamAdjustment adjustment : adjustments) {
                WriteSummary summary = apply(workbooks, masters.get(adjustment.parameterName()), adjustment);
                if (summary == null) {
                    skippedParams++;
                    skippedRows += adjustment.values() == null ? 0 : adjustment.values().size();
                    continue;
                }
                total = WriteSummary.sum(total, summary);
            }

            log.info("[ModelInput Merge] - DONE - parameters={} (기입 {}, skip {}) / cells: write={}, append={}, 미기입 rows={}",
                adjustments.size(), adjustments.size() - skippedParams, skippedParams,
                total.writeCount(), total.appendCount(), skippedRows);

            return MergedModelInputFiles.of(serialize(workbooks));

        } finally {
            workbooks.values().forEach(this::closeQuietly);
        }

    }

    /**
     * 확정 파라미터 1건을 대상 시트에 기입한다.
     * read mode에 따라 기입 전략이 다름. 기입했으면 셀 집계를, 미대상 skip이면 null을 반환한다.
     */
    private WriteSummary apply(Map<ModelInputFile, XSSFWorkbook> workbooks, MiModelParam param, ModelParamAdjustment adjustment) {
        String context = ValueRows.context(param);

        // 제안 대상(use_yn=Y)만 병합
        if (!Boolean.TRUE.equals(param.getIsUsed())) {
            log.warn("[ModelInput Merge] - SKIP - [{}] 미기입 rows={} — 제안 미대상(use_yn=N)이라 기입하지 않음 (확정 payload에는 포함됨)",
                context, adjustment.values() == null ? 0 : adjustment.values().size());
            return null;
        }

        if (adjustment.values() == null || adjustment.values().isEmpty()) {
            throw ValueRows.error(context, "기입할 values가 없습니다");
        }

        ModelInputFile file = ModelInputFile.from(param.getModelFile());
        Sheet sheet = workbooks.get(file).getSheet(param.getModelSheet());

        if (sheet == null) {
            throw SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR,
                "수급모델 입력 병합 실패 [%s] — Basecase 원본(%s)에 시트가 없습니다 (마스터-원본 정합 확인 필요)".formatted(context, file.getBasecaseFileName()));
        }

        ColumnRoleMap roles = ColumnRoleMap.parse(context, param.getBasecaseColumnRoles());

        return switch (param.getBasecaseReadMode()) {
            case FIXED_COLUMNS -> new FixedColumnsWriter(sheet, param, roles).apply(adjustment.values());
            case MATRIX_TIME_COLUMNS -> new MatrixTimeColumnsWriter(sheet, param, roles).apply(adjustment.values());
        };
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

    /**
     * 워크북을 바이트로 직렬화한다. 값이 바뀐 셀에 걸린 수식들이 다음 오픈 시 재계산되도록 플래그를 켠다.
     */
    private Map<ModelInputFile, byte[]> serialize(Map<ModelInputFile, XSSFWorkbook> workbooks) {
        Map<ModelInputFile, byte[]> contents = new EnumMap<>(ModelInputFile.class);
        workbooks.forEach((file, workbook) -> {
            workbook.setForceFormulaRecalculation(true);
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                contents.put(file, out.toByteArray());
            } catch (IOException e) {
                log.error("[ModelInput Merge] 직렬화 실패: {}", file.getBasecaseFileName(), e);
                throw SystemErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "수급모델 입력 병합 실패 — 병합 파일 생성에 실패했습니다: %s".formatted(file.getBasecaseFileName()));
            }
        });
        return contents;
    }

    private void closeQuietly(XSSFWorkbook workbook) {
        try {
            workbook.close();
        } catch (IOException e) {
            log.warn("[ModelInput Merge] workbook close 실패: {}", e.getMessage());
        }
    }

}
