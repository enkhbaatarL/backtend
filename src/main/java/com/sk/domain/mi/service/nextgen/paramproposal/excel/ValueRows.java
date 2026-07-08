package com.sk.domain.mi.service.nextgen.paramproposal.excel;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.shared.entity.master.tmm.MiModelParam;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 확정 payload values row 판독 헬퍼.
 * slot 축 추출과 proposedValue 검증, 병합 실패 에러 생성을 한곳에 모은다.
 *
 * 에러 메시지 규칙: 모든 병합 에러는 "수급모델 입력 병합 실패" 한 prefix로 통일하고,
 * 위치 맥락은 [파라미터명 | 파일>시트] 형태로 담아 메시지만 보고 대상 시트를 열 수 있게 한다.
 */
final class ValueRows {

    private ValueRows() {
    }

    /** 에러 메시지에 넣을 위치 맥락 — "Temperature | Demand>WeatherAdjustment" 형태. */
    static String context(MiModelParam param) {
        return "%s | %s>%s".formatted(param.getParameterName(), param.getModelFile(), param.getModelSheet());
    }

    /**
     * row에서 메타 필드(basecaseValue/proposedValue/rationale)를 제외한 slot 축을 추출한다.
     * slot 축이 없거나 값이 null이면 좌표를 특정할 수 없으므로 에러.
     */
    static Map<String, Object> slotAxes(String context, Map<String, Object> valueRow) {
        Map<String, Object> axes = new LinkedHashMap<>();
        valueRow.forEach((key, value) -> {
            if (!ModelParamAdjustment.META_FIELDS.contains(key)) {
                axes.put(key, value);
            }
        });
        if (axes.isEmpty()) {
            throw error(context, "slot 축이 없습니다: %s".formatted(valueRow.keySet()));
        }
        axes.forEach((key, value) -> {
            if (value == null) {
                throw error(context, "slot 축 값이 null입니다: %s".formatted(key));
            }
        });
        return axes;
    }

    /** 셀에 기입할 최종 입력값. Agent 계약상 절대값 숫자만 유효하다. */
    static double proposedValue(String context, Map<String, Object> valueRow) {
        Double number = ExcelCells.asNumber(valueRow.get("proposedValue"));
        if (number == null) {
            throw error(context, "proposedValue가 없거나 숫자가 아닙니다: %s".formatted(valueRow.get("proposedValue")));
        }
        return number;
    }

    static AppErrorException error(String context, String detail) {
        return AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "수급모델 입력 병합 실패 [%s] — %s".formatted(context, detail));
    }

}
